package be.uantwerpen.ds.system_y.connection;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import be.uantwerpen.ds.system_y.client.Client;

public class TCPConnection implements Runnable {
	private Socket clientSocket;
	private DataInputStream in;
	private Thread connectionThread;
	private FileReceiver client;
	
	public TCPConnection(Socket clientSocket, FileReceiver client) {
		this.clientSocket = clientSocket;
		this.client = client;
		try {
			in = new DataInputStream(clientSocket.getInputStream());
			connectionThread = new Thread(this);
			connectionThread.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		FileOutputStream fos = null;
		InetAddress sender = clientSocket.getInetAddress();
		try {
			String fileName = in.readUTF();
			File file = new File(Client.OWNED_FILE_PATH + fileName);
			//TODO naar juiste pad schrijven
			fos = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int count;
			while ((count = in.read(buffer)) >= 0 ){
				fos.write(buffer, 0, count);
			}
			fos.flush();
			client.fileReceived(sender, fileName);
			System.out.println("Received file " + fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
				in.close();
				clientSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}

}
