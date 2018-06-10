package socket;

import java.util.Scanner;
import java.util.HashMap;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import socket.listener.*;
import json.*;

public class ClientRunnerThread extends Thread
{
	private String userID;

	private Socket client;
	private OutputStream output;
	private InputStream input;

	private final int BUFFER_SIZE = 100;
	
	private DataSendListener dataSendListener;
	private DataReceiveListener dataReceiveListener;
	
	private HashMap<String, DataReceiveListener> dataReceiveListnerCollections = new HashMap<>();

	public ClientRunnerThread(Socket client, String userID) throws Exception
	{
		this.userID = userID;

		output = client.getOutputStream();
		input = client.getInputStream();
	}

	public byte[] trimByte(byte[] data, int length)
	{
		byte[] returnValue = new byte[length];

		for(int i=0; i<length; i++)
			returnValue[i] = data[i];

		return returnValue;
	}

	public String getUserID()
	{
		return this.userID;
	}

	public void run()
	{
	}

	//send
	public void sendData(String message)
	{
		new SendThread(message).start();
	}

	public void setDataSendListener(DataSendListener dataSendListener)
	{
		this.dataSendListener = dataSendListener;
	}

	//data send class
	private class SendThread extends Thread
	{
		private String message;

		public SendThread(String message)
		{
			this.message = message;
		}

		@Override
			public void run()
			{
				boolean sendResult = false;

				try
				{
					byte[] data = message.getBytes();
					output.write(data, 0, data.length);	
					sendResult = true;
				}
				catch(Exception e)
				{
					//e.printStackTrace();
					sendResult = false;
				}

				if(dataSendListener != null)
					dataSendListener.sendData(sendResult);
			}
	}

	//receive
	public void receiveData()
	{
		new ReceiveThread().start();
	}

	public void setDataReceiveListener(DataReceiveListener listener)
	{
		this.dataReceiveListener = listener;
	}

	public void setDataReceiveListener(String command, DataReceiveListener listner)
	{
		this.dataReceiveListnerCollections.put(command, listner);
	}

	//data receive listener
	private class ReceiveThread extends Thread
	{
		@Override
		public void run()
		{
			boolean closed = false;

			try
			{
				byte[] data = new byte[BUFFER_SIZE];
				int length = input.read(data, 0, BUFFER_SIZE);
				data = trimByte(data, length);

				String message = new String(data);

				if(dataReceiveListener != null)
					dataReceiveListener.receiveData(message);

				try
				{
					JsonParser json = JsonParser.parse(message);
					String command = json.get("command");

					for(String key_command : dataReceiveListnerCollections.keySet())
						if(key_command.equals(command))
							dataReceiveListnerCollections.get(key_command).receiveData(message);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.out.println("wrong json type");
				}

				closed = false;
			}
			catch(Exception e)
			{
				closed = true;
			}
			finally
			{
				if(!closed)
					receiveData();
			}
		}
	}
}
