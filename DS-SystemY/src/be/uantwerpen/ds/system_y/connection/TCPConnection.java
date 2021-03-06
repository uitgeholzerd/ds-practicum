package be.uantwerpen.ds.system_y.connection;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import be.uantwerpen.ds.system_y.client.Client;

/**
 * Thread that contains a single TCP connection
 *
 */
public class TCPConnection implements Runnable {
	private Socket clientSocket;
	private DataInputStream in;
	private DataOutputStream out;
	private FileReceiver client;

	public TCPConnection(Socket clientSocket, FileReceiver client) {
		this.clientSocket = clientSocket;
		this.client = client;
		try {
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			System.err.println("Error while creating TCP connection");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		System.out.println("Incoming TCP connection");
		BufferedOutputStream fos = null;
		InetAddress sender = clientSocket.getInetAddress();
		try {
			String[] message = in.readUTF().split(" ");
			Protocol command = Protocol.valueOf(message[0]);
			String fileName = message[1];
			System.out.printf("TCP command: %s.%n", command);
			switch (command){
			case SEND_FILE:
				boolean owner = in.readBoolean();
				System.out.printf("Receiving file %s (owner=%s)%n", fileName, owner);
				Path file;
				if (owner) {
					file = Paths.get(Client.OWNED_FILE_PATH + fileName);
				} else {
					file = Paths.get(Client.LOCAL_FILE_PATH + fileName);
				}
				OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE };
				fos = new BufferedOutputStream(Files.newOutputStream(file, options));
				byte[] buffer = new byte[1024];
				int count;
				while ((count = in.read(buffer)) >= 0) {
					fos.write(buffer, 0, count);
				}
				fos.flush();
				client.receiveFile(sender, fileName, owner);
				break;
			case CHECK_OWNER:
				System.out.printf("Check if I already own %s.%n", fileName);
				out.writeBoolean(client.hasOwnedFile(fileName));
				break;
			default:
				System.out.println("Unknown command: "+ command);
			}

		} catch (IOException e) {
			System.err.println("Error during TCP connection");
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
				in.close();
				out.close();
				clientSocket.close();
			} catch (IOException e1) {
				System.err.println("Error while closing TCP connection");
				e1.printStackTrace();
			}
		}

	}

}
