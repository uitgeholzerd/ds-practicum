package be.uantwerpen.ds.system_y.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Handles sending and receiving of UDP multicasts
 *
 */
public class MulticastHandler implements Runnable {
	private static final String MULTICAST_ADDRESS = "225.6.7.8";
	private static final int MULTICAST_PORT = 5678;

	private MulticastSocket socket;
	private DatagramPacket inPacket;
	private byte[] inBuffer;
	private PacketListener listener;
	private Thread listenThread;

	public MulticastHandler(PacketListener listener) throws IOException {
		this.listener = listener;
		try {
			socket = new MulticastSocket(MULTICAST_PORT);
			socket.setInterface(listener.getAddress());
			socket.joinGroup(InetAddress.getByName(MULTICAST_ADDRESS));
		} catch (IOException e) {
			System.err.println("Failed to open multicast socket: " + e.getMessage());
			throw e;
		}

		listenThread = new Thread(this);
		listenThread.setName("MulticastHandler");
		listenThread.start();
	}

	public void run() {
		// Listen for packets
		System.out.println("Multicast socket listening on port " + socket.getLocalPort());
		while (!listenThread.isInterrupted()) {
			inBuffer = new byte[1024];
			inPacket = new DatagramPacket(inBuffer, inBuffer.length);
			try {
				socket.receive(inPacket);
			} catch (IOException e) {
				if (!listenThread.isInterrupted())
					System.err.println("Failed to receive multicast packet: " + e.getMessage());
			}
			if (inPacket != null && inPacket.getAddress() != null) {
				String msg = new String(inBuffer, 0, inPacket.getLength());
				listener.packetReceived(inPacket.getAddress(), msg);
			}

		}
		System.out.println("Multicast socket closed ");
	}

	/**
	 * Sends a message to the multicast group
	 * 
	 * @param command
	 *            A command from the Protocol enum
	 * @param message
	 *            The message to send
	 * @throws IOException
	 */
	public void sendMessage(Protocol command, String data) throws IOException {
		// prepare packet & send to existing socket
		String message = command + " " + data;
		DatagramPacket outPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(MULTICAST_ADDRESS), MULTICAST_PORT);
		socket.send(outPacket);
		System.out.println("Sent multicast " + outPacket + "[" + message + "]");
	}

	/**
	 * Closes the socket of the client
	 */
	public void closeClient() {
		listenThread.interrupt();
		try {
			socket.leaveGroup(InetAddress.getByName(MULTICAST_ADDRESS));
			socket.close();
		} catch (IOException e) {
			System.err.println("Failed to leave multicast group: " + e.getMessage());
		}
		socket = null;
		listenThread = null;

	}
}
