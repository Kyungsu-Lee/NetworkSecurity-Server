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
import jce.des.*;
import jce.aes.*;

import util.*;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

public class ServerSocket extends Thread
{
	public static final int BUFFER_SIZE = 20000;

	private java.net.ServerSocket server;
	private HashMap<String, ClientRunnerThread> clients;
	private HashMap<String, RSASecretKey> publicKeys;

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
			
				publicKeys.put(json.get("ID"), RSASecretKey.setPublicKey(json.get("key mod"), json.get("key exp")));
			}
		});

		clientThread.setDataReceiveListener("KDC1", new DataReceiveListener(){

			private String encrypt(String message, RSAPublicKey key)
			{
				return RSAAlgorithm.encrpytedAsBase64(message, key);
			}
	
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);

				String userFrom = json.get("From");	//user 1
				String userTo   = json.get("To");	//user 2
				String nonce    = json.get("Nonce");

				String sessionKeyForDES = new DESSecretKey().toString();
				String sessionKeyForAES = new AESSecretKey().toString();
	
				RSASecretKey user1Key = publicKeys.get(userFrom);
				RSASecretKey user2Key = publicKeys.get(userTo);

				String message1 = new JsonParser()
						.add("session key", sessionKeyForDES)
						.add("From", userFrom)
						.add("To", userTo)
						.add("Nonce", nonce)
						.toString();

				String message2 = new JsonParser()
						.add("session key", "sssss")
						.add("From", userFrom)
						.toString();

				System.out.println(message1);
				System.out.println(message2);

				clientThread.sendData(
					new JsonParser()
						.add("command", "KDC2")
						.add("user1 session key des", 	encrypt(sessionKeyForDES, 	user1Key.getPublicKey()))
						.add("user1 session key aes",	encrypt(sessionKeyForAES,	user1Key.getPublicKey()))
						.add("user1 From", 		encrypt(userFrom, 		user1Key.getPublicKey()))
						.add("user1 To", 		encrypt(userTo, 		user1Key.getPublicKey()))
						.add("user1 Nonce", 		encrypt(nonce, 			user1Key.getPublicKey()))
						.add("user2 session key des", 	encrypt(sessionKeyForDES, 	user2Key.getPublicKey()))
						.add("user2 session key aes",	encrypt(sessionKeyForAES,	user2Key.getPublicKey()))
						.add("user2 From", 		encrypt(userFrom, 		user2Key.getPublicKey()))
						.toString()
				);
			}
		});

		clientThread.setDataReceiveListener("KDC3", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);
				
				String toUser = json.get("To");

				for(String uuid : clients.keySet())
					if(uuid.equals(toUser))
					{
						JsonParser tmp = JsonParser.parse(message);
						clients.get(uuid).sendData(tmp.toString());
						System.out.println(String.format("from %s to %s : %s", clientThread.getUserID(), uuid, tmp.toString())); 
					}
			}
		});

		//test
		clientThread.setDataReceiveListener("Test", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				try{
				System.out.println(message);
				JsonParser parser = JsonParser.parse(message);

				//RSASecretKey publicKey = RSASecretKey.setPublicKey(parser.get("key mod"), parser.get("key exp"));
				RSASecretKey publicKey = publicKeys.get(parser.get("ID"));

				String en = RSAAlgorithm.encrpytedAsBase64("orld", publicKey.getPublicKey());

				clientThread.sendData(new JsonParser()
					.add("command", "Test")
					.add("message", "hello")
					.add("key mod", parser.get("key mod"))
					.add("key exp", parser.get("key exp"))
					.add("en", en)
					.toString()
				);
				}catch(Exception e)
				{
					e.printStackTrace();
				}
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
