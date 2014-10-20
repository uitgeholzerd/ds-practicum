package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import be.uantwerpen.ds.ns.client.Client;

public class MulticastHandler implements Runnable {
	private static final String multicastAddress = "225.6.7.8";
	private static final int multicastPort = 5678;

	private MulticastSocket socket;
	private DatagramPacket inPacket;
	private byte[] inBuffer;
	private PacketListener listener;
	private Thread listenThread;
	private boolean isRunning;

	public MulticastHandler(PacketListener listener) {
		this.listener = listener;
		try {
			socket = new MulticastSocket(multicastPort);
			socket.joinGroup(InetAddress.getByName(multicastAddress));
			// TODO else?
		} catch (IOException e) {
			System.err.println("Failed to open multicast socket: "
					+ e.getMessage());
		}
		
		isRunning = true;
		listenThread = new Thread(this);
		listenThread.setName("MulticastHandler");
		listenThread.start();
	}

	public void run() {
		// Listen for packets
		System.err.println("Multicast socket listening...");
		while (isRunning) {
			inBuffer = new byte[1024];
			inPacket = new DatagramPacket(inBuffer, inBuffer.length);
			
			try {
				socket.receive(inPacket);
			} catch (IOException e) {
				
					System.err.println("Failed to receive multicast packet: "
							+ e.getMessage());
			}
			System.out.println("Multicast: " + inPacket);
			if (inPacket != null && inPacket.getAddress() != null) {

				// Prevent sender from receiving its own broadcast
				if (!inPacket.getAddress().equals(listener.getAddress())) {
					String msg = new String(inBuffer, 0, inPacket.getLength());
					listener.packetReceived(inPacket.getAddress(), msg);
				} else // for debugging
				{
					System.out.println("discarded local multicast");
				}

			}
		}
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
		// TODO: this might not work on the same socket used for listening
		String message = command + " " + data;
		DatagramPacket outPacket = new DatagramPacket(message.getBytes(),
				message.getBytes().length,
				InetAddress.getByName(multicastAddress), multicastPort);
		socket.send(outPacket);
	}

	/**
	 * Closes the socket of the client
	 */
	public void closeClient() {
		isRunning = false;
		try {
			socket.leaveGroup(InetAddress.getByName(multicastAddress));
		} catch (IOException e) {
			System.err.println("Failed to leave multicast group: "
					+ e.getMessage());
		}

		socket.close();

	}
}
