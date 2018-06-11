import socket.*;

import json.*;
import jce.rsa.*;

public class Main
{
	public static void main(String[] args)
	{
		RSASecretKey key = new RSASecretKey();
		String message = "hello";
		
		String encrypt = RSAAlgorithm.encrpytedAsBase64(message, key.getPublicKey());
		System.out.println(encrypt);
	

		RSASecretKey copyKey = RSASecretKey.setPrivateKey(key.toPrivateKey2String());
		String decrypt = RSAAlgorithm.decryptBase64AsString(encrypt, copyKey.getPrivateKey());

		System.out.println(decrypt);

		System.out.println(key.toPrivateKey2String());
		System.out.println(copyKey.toPrivateKey2String());

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
