package be.uantwerpen.ds.system_y.connection;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;



public class TCPHandler implements Runnable{
	private int port;
	private Socket sendSocket;
	private ServerSocket listenSocket; 
	private Thread listenThread;
	private FileReceiver listener;
	
	public TCPHandler(int port, FileReceiver listener) {
		this.port = port;
		this.listener = listener;
		try {
			this.listenSocket = new ServerSocket(port);
			listenThread = new Thread(this);
			listenThread.setName("TCPHandler");
			listenThread.start();
			
		} catch (IOException e) {
			System.err.println("Error while initializing TCPHandler socket");
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
				connection = new TCPConnection(connectionSocket, listener);
				(new Thread(connection)).start();
			} catch (IOException e) {
				System.err.println("Error while listening for connections in TCPHandler");
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Send a file to the address per 1024 bytes
	 * 
	 * @param address	Address of the receiver
	 * @param filename	Name of the file
	 * @param filehash	Hash of the file
	 */
	public void sendFile(InetAddress address, String filename, int filehash) {
		System.out.println("Sending file " + filename);
		FileInputStream fis = null;
		DataOutputStream out = null;
		
		try {
			sendSocket = new Socket(address, port);
			File file = new File(filename);
			fis = new FileInputStream(file);
			byte[] fileByteArray = new byte[1024];
			out = new DataOutputStream(sendSocket.getOutputStream());
			
			out.writeUTF(filename);
			out.writeInt(filehash);
			
			int count;
			// While there are bytes available, write then to the outputstream
			while ((count = fis.read(fileByteArray)) >= 0) {
				out.write(fileByteArray, 0, count);
			}
			
		} catch (IOException e) {
			System.err.println("Error while sending file in TCPHandler");
			e.printStackTrace();
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
				if (out != null) {
					out.close();
				}
				sendSocket.close();
			} catch (IOException e) {
				System.err.println("Error while closing TCPHandler resources");
				e.printStackTrace();
			}
		}
	}

}
