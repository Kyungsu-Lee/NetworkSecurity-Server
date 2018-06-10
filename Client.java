import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
 
public class Client {
    public static void main(String... args){
        //자동 close
        try(Socket client = new Socket()){
            //클라이언트 초기화
            InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", 9999);
            //접속
            client.connect(ipep);
		Scanner sc = new Scanner(System.in);
             
            //send,reciever 스트림 받아오기
            //자동 close
            try(OutputStream sender = client.getOutputStream();
                InputStream receiver = client.getInputStream();){
                //서버로부터 데이터 받기
                //11byte
                //서버로 데이터 보내기
                //2byte
		while(true)
		{
			String message = sc.nextLine();
       	        	byte[] data = message.getBytes();
                	sender.write(data, 0, data.length);
		}
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
    }
}

