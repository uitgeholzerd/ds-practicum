package be.uantwerpen.ds.system_y.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Handles TCP communication by spawning TCP connections
 * 
 */
public class TCPHandler implements Runnable {
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
		while (!listenThread.isInterrupted()) {
			try {
				connectionSocket = listenSocket.accept();
				connection = new TCPConnection(connectionSocket, listener);
				(new Thread(connection)).start();
			} catch (IOException e) {
				if (!listenThread.isInterrupted()) {
					System.err.println("Error while listening for connections in TCPHandler");
					e.printStackTrace();
				}
			}
		}
		System.out.println("TCP socket closed ");
	}

	/**
	 * Check if the node is already of of the file
	 * 
	 * @param address Address of the node
	 * @param fileName Name of the file
	 * @return
	 */
	public boolean checkHasOwnedFile(InetAddress address, String fileName) {
		boolean result = false;
		DataOutputStream out = null;
		DataInputStream in = null;

		try {
			sendSocket = new Socket(address, port);
			out = new DataOutputStream(sendSocket.getOutputStream());
			in = new DataInputStream(sendSocket.getInputStream());
			out.writeUTF(Protocol.CHECK_OWNER + " " + fileName);
			result = in.readBoolean();
			System.out.printf("Check if %s already owns %s: %s%n", address.getHostAddress(), fileName, result);

		} catch (IOException e) {
			System.err.println("Error while checking owner TCPHandler");
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
				if (sendSocket != null) {
					// sendSocket.close();
				}
			} catch (Exception e) {
				System.err.println("Error while closing TCPHandler resources");
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Send a file to the address per 1024 bytes
	 * 
	 * @param address Address of the receiver
	 * @param file Name of the file
	 * @param filehash Hash of the file
	 * 
	 * @throws IOException This is throw if the address cannot be reached
	 */
	public void sendFile(InetAddress address, File file, boolean receiverIsOwner) throws IOException {
		System.out.print("Sending file " + file + " to " + address.getHostAddress() + "... ");
		FileInputStream fis = null;
		DataOutputStream out = null;

		sendSocket = new Socket(address, port);

		try {
			out = new DataOutputStream(sendSocket.getOutputStream());
			fis = new FileInputStream(file);
			byte[] fileByteArray = new byte[1024];

			out.writeUTF(Protocol.SEND_FILE + " " + file.getName());
			out.flush();

			out.writeBoolean(receiverIsOwner);

			out.flush();

			int count;
			// While there are bytes available, write them to the outputstream
			while ((count = fis.read(fileByteArray)) >= 0) {
				out.write(fileByteArray, 0, count);
			}
			out.flush();
			System.out.println("File sent.");
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
				if (sendSocket != null) {
					// sendSocket.close();
				}
			} catch (Exception e) {
				System.err.println("Error while closing TCPHandler resources");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Closes the socket of the client
	 */
	public void closeClient() {
		listenThread.interrupt();
		try {
			listenSocket.close();

		} catch (IOException e) {
			System.err.println("Error while closing TCP handler");
			e.printStackTrace();
		}

		listenSocket = null;
		listenThread = null;
	}
}
