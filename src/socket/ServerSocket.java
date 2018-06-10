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

public class ServerSocket extends Thread
{
	private final int BUFFER_SIZE = 100;

	private java.net.ServerSocket server;
	private HashMap<String, ClientRunnerThread> clients;

	public ServerSocket(int port)	throws Exception
	{
		server = new java.net.ServerSocket();
		clients = new HashMap<>();
		
		InetSocketAddress ipep = new InetSocketAddress(port);
		server.bind(ipep);
	}

	private void listen()	throws Exception
	{
		Socket client = server.accept();

		String userUUID = UUID.randomUUID().toString();
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
				
				clientThread.sendData(new JsonParser().add("command", "Request ID").add("UUIDs", sb.toString()).toString());
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
			}
		});

		clientThread.setDataReceiveListener("Send To", new DataReceiveListener(){
			@Override
			public void receiveData(String message)
			{
				JsonParser json = JsonParser.parse(message);
				System.out.println("received from " + clientThread.getUserID() + " : " + json.get("message"));

				for(String uuid : clients.keySet())
					if(uuid.equals(json.get("ID")))
					{
						clients.get(uuid).sendData(json.get("message"));
						System.out.println(String.format("from %s to %s : %s", clientThread.getUserID(), uuid, json.get("message"))); 
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
