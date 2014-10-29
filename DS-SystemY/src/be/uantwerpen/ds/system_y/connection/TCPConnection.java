package be.uantwerpen.ds.system_y.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;

public class TCPConnection implements Runnable {
	private Socket clientSocket;
	private DataInputStream in;
	private DataOutputStream out;
	private Thread connectionThread;
	private FileReceiver client;
	
	public TCPConnection(Socket clientSocket, FileReceiver client) {
		this.clientSocket = clientSocket;
		this.client = client;
		try {
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			connectionThread = new Thread(this);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		FileOutputStream fos = null;
		try {
			String fileName = in.readUTF();
			int fileHash = in.readInt();
			File file = new File(fileName);
			fos = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int count;
			while ((count = in.read(buffer)) >= 0 ){
				fos.write(buffer, 0, count);
			}
			fos.flush();
			client.fileReceived(fileHash, fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (fos != null){
					fos.close();
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

}
