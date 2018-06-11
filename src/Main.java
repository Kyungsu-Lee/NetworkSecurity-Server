import socket.*;

import json.*;
import jce.rsa.*;

public class Main
{
	public static void main(String[] args)
	{
		try
		{
			ServerSocket serverSocket = new ServerSocket(9999);
			serverSocket.run();
			System.out.println("well");
			
			serverSocket.join();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
