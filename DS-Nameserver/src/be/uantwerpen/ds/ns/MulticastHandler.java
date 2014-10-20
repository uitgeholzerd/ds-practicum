package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastHandler implements Runnable {
	private static final String multicastAddress = "225.6.7.8";
	private static final int multicastPort = 5678;

	private MulticastSocket socket;
	private DatagramPacket inPacket;
	private byte[] inBuffer;
	private PacketListener listener;

	public MulticastHandler(PacketListener listener) {
		this.listener = listener;
		try {
			socket = new MulticastSocket(multicastPort);
			socket.joinGroup(InetAddress.getByName(multicastAddress));
			// TODO else?
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(this).start();
	}

	public void run() {
		// Listen for packets
		while (true) {
			inBuffer = new byte[1024];
			inPacket = new DatagramPacket(inBuffer, inBuffer.length);
			try {
				socket.receive(inPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String msg = new String(inBuffer, 0, inPacket.getLength());
			// Prevent sender from receiving its own broadcast
			if (inPacket.getAddress() != listener.getAddress()) {
				listener.packetReceived(inPacket.getAddress(), msg);
			}
		}
	}

	/**
	 * Sends a message to the multicast group
	 * 
	 * @param command A command from the Protocol enum
	 * @param message	The message to send
	 * @throws IOException
	 */
	public void sendMessage(Protocol command, String data) throws IOException {
		// prepare packet & send to existing socket
		// TODO: this might not work on the same socket used for listening
		String message = command + " " + data;
		DatagramPacket outPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(multicastAddress), multicastPort);
		socket.send(outPacket);
	}
}
