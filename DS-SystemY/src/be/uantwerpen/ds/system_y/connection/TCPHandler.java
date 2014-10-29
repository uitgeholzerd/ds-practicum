package be.uantwerpen.ds.system_y.connection;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import be.uantwerpen.ds.system_y.PacketListener;

public class TCPHandler implements Runnable{
	private static int serverPort = 23456;
	
	Socket sendSocket;
	ServerSocket listenSocket; 
	private Thread listenThread;
	
	int port;
	
	public TCPHandler(int port, PacketListener listener) {
		this.port = port;
		try {
			this.listenSocket = new ServerSocket(serverPort);
			listenThread = new Thread(this);
			listenThread.setName("TCPHandler");
			listenThread.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		Socket connectionSocket;
		TCPConnection connection;
		// Handler waits untill a connection is made on de listenSocket, then spawn a new TCPConnection thread that handles the transfer
		while (true) {
			try {
				connectionSocket = listenSocket.accept();
				connection = new TCPConnection(connectionSocket);
				(new Thread(connection)).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public boolean sendFile(InetAddress address, int port, Protocol command, String filename, int filehash){
		try {
			sendSocket = new Socket(address, port);
			File file = new File(filename);
			FileInputStream fis = new FileInputStream(file);
			byte[] fileByteArray = new byte[1024];
			DataOutputStream out = new DataOutputStream(sendSocket.getOutputStream());
			
			out.writeUTF(filename);
			out.writeInt(filehash);
			
			int count;
			while ((count = fis.read(fileByteArray)) >= 0) {
				out.write(fileByteArray, 0, count);
			}
			
			fis.close();
			out.close();
			sendSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

}
