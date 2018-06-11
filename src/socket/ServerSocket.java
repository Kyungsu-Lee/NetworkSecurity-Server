package socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.HashMap;
import java.util.UUID;

import java.util.Scanner;

import socket.listener.*;
import json.*;
import jce.rsa.*;
import util.*;

import java.security.PublicKey;

public class ServerSocket extends Thread
{
	public static final int BUFFER_SIZE = 20000;

	private java.net.ServerSocket server;
	private HashMap<String, ClientRunnerThread> clients;
	private HashMap<String, String> publicKeys;

	public ServerSocket(int port)	throws Exception
	{
		server = new java.net.ServerSocket();
		clients = new HashMap<>();
		publicKeys = new HashMap<>();
		
		InetSocketAddress ipep = new InetSocketAddress(port);
		server.bind(ipep);
	}

	private void listen()	throws Exception
	{
		Socket client = server.accept();

		//String userUUID = UUID.randomUUID().toString();
		String userUUID = client.getRemoteSocketAddress().toString();
		System.out.println("accepted, " + userUUID);

		final ClientRunnerThread clientThread = new ClientRunnerThread(client, userUUID);
		clientThread.start();
		clientThread.setDataSendListener(new DataSendListener(){
		
			@Override
			public void sendData(boolean result)
			{
				if(!result)
				{
					clients.remove(clientThread.getUserID());
				}
			}
		});

		//set command
		clientThread.setDataReceiveListener("To Server", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);
				System.out.println("received from " + clientThread.getUserID() + " : " + json.get("message"));
			}
		});

		clientThread.setDataReceiveListener("Request ID", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);
				
				StringBuilder sb = new StringBuilder();

				for(String UUIDs : clients.keySet())
					sb.append(UUIDs).append(",");
				
				for(String id : clients.keySet())
					clients.get(id).sendData(new JsonParser().add("command", "Request ID").add("UUIDs", sb.toString()).toString());
				System.out.println(String.format("%s requested UUIDs", clientThread.getUserID()));
			}
		});

		clientThread.setDataReceiveListener("Request My ID", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);
				
				clientThread.sendData(new JsonParser().add("command", "Request My ID").add("UUID", clientThread.getUserID()).toString());
				System.out.println(String.format("%s requested %s's UUIDs", clientThread.getUserID(), clientThread.getUserID()));

				try{Thread.sleep(1000);}catch(Exception e){e.printStackTrace();}

				clientThread.sendData(new JsonParser()
					.add("command", "request public key")
					.toString()
				);
			}
		});

		clientThread.setDataReceiveListener("Send To", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				System.out.println(message);
				JsonParser json = JsonParser.parse(message);
				System.out.println("received from " + clientThread.getUserID() + " : " + json.get("message"));
				
				try{
				//test for RSA in server
				if(json.get("encrypt").equals("RSA"))
				{
					System.out.println("key : " + publicKeys.get(json.get("FROM")));
					RSASecretKey key = RSASecretKey.setPrivateKey(publicKeys.get(json.get("FROM")));
					System.out.println(key.toPrivateKey2String());
					System.out.println(key.toPrivateKey2String().length());
					System.out.println(json.get("message"));
					String det = RSAAlgorithm.decryptBase64AsString(json.get("message"), key.getPrivateKey());
					System.out.println(det);
				}
				}catch(Exception e)
				{
					e.printStackTrace();
				}
				///////

				for(String uuid : clients.keySet())
					if(uuid.equals(json.get("ID")))
					{
						JsonParser tmp = JsonParser.parse(message);
						clients.get(uuid).sendData(tmp.toString());
						System.out.println(String.format("from %s to %s : %s", clientThread.getUserID(), uuid, tmp.toString())); 
					}
			}
		});

		clientThread.setDataReceiveListener("Request Key", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);

				String key = json.get("key");
				publicKeys.put(json.get("ID"), key);
				System.out.println("saved " + json.get("ID") + " : " + json.get("key"));
			}
		});

		//test
		clientThread.setDataReceiveListener("Test", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser parser = JsonParser.parse(message);

				System.out.println("hello");
				System.out.println(parser.get("key"));
				
				RSASecretKey publicKey = (RSASecretKey)ObjectByteStream.toObject(Base64.decode(parser.get("key"), Base64.NO_WRAP));
				System.out.println("hello");
				System.out.println("key : " + publicKey.toPublicKey2String());

				String en = RSAAlgorithm.encrpytedAsBase64("hello", publicKey.getPublicKey());

				System.out.println("en : " + en);


				clientThread.sendData(new JsonParser()
					.add("command", "Test")
					.add("message", en)
					.toString()
				);
			}
		});
		///////////

		clientThread.receiveData();
		clients.put(userUUID, clientThread);

	}

	@Override
	public void run()
	{
		new ConnectionThread().start();
		new ScannerThread().start();
	}

	private class ScannerThread extends Thread
	{
		@Override
		public void run()
		{
			Scanner sc = new Scanner(System.in);

			String message = null;
			while((message = sc.nextLine()) != null)
			{
				for(String clientID : clients.keySet())
				{
					System.out.println("send data to " + clientID);
					clients.get(clientID).sendData(message);
				}
			}
		}
	}

	private class ConnectionThread extends Thread
	{
		@Override
		public void run()
		{
			try{
				while(true)
				{
					System.out.println("listen");
					listen();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
