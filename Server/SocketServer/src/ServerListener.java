import java.net.ServerSocket;
import java.net.Socket;


public class ServerListener extends Thread {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			ServerSocket serverSocket = new ServerSocket(12345);
			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("A client has conneted to the server port 12345");
				SocketThread thread = new SocketThread(socket);
				thread.start();
			}
		} catch (Exception e) {
		}
	}

}
