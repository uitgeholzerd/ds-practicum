package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketPermission;
import java.net.SocketTimeoutException;

/**
 * @author seb
 *
 */

public class DatagramHandler implements Runnable {

	private DatagramSocket socket;
	private DatagramPacket inPacket;
	private byte[] buffer;
	private PacketListener listener;
	private Thread listenThread;
	private boolean isRunning;

	/**
	 * @param port
	 *            UDP port to listen on
	 */
	public DatagramHandler(int listenPort, PacketListener listener) {
		this.listener = listener;
		try {
			socket = new DatagramSocket();
			socket.bind(new InetSocketAddress(listener.getAddress(), listenPort));
		} catch (SocketException e) {
			System.err.println("Failed to open UDP socket: " + e.getMessage());
		}
		listenThread = new Thread(this);
		listenThread.setName("DatagramHandler");
		listenThread.start();
	}

	@Override
	public void run() {
		if (socket == null) {
			System.err.println("UDP socket not open, exiting listener thread");
			return;
		}
		buffer = new byte[1024];
		// Listen for UDP datagrams
		System.err.println("UDP socket listening on port " + socket.getLocalPort());
		while (isRunning) {
			inPacket = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(inPacket);
			} catch (IOException e) {
					System.err.println("Failed to receive UDP datagram: "
							+ e.getMessage());
			}
			System.out.println("Datagram: " + inPacket);
			if (inPacket != null && inPacket.getAddress()!=null) {
				String msg = new String(buffer, 0, inPacket.getLength());
				listener.packetReceived(inPacket.getAddress(), msg);
			}
		}
	}

	/**
	 * Sends a message via UDP
	 * 
	 * @param address
	 *            Remote address
	 * @param port
	 *            Remote port
	 * @param command
	 *            A command from the Protocol enum
	 * @param message
	 *            The message to send
	 * @throws IOException
	 */
	public void sendMessage(InetAddress address, int port, Protocol command,
			String data) throws IOException {
		String message = command + " " + data;
		DatagramPacket outPacket = new DatagramPacket(message.getBytes(),
				message.length(), address, port);
		socket.send(outPacket);
		System.out.println("Sent datagram "+outPacket+"[" + message + "] to " + address.getHostAddress()+":"+port);
	}

	/**
	 * Closes the socket of the client
	 */
	public void closeClient() {
		isRunning = false;
		socket.close();
	}
}
