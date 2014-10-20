package be.uantwerpen.ds.ns;

import java.net.InetAddress;

public interface PacketListener {
	/**
	 * @param sender	The address of the sender of the packet
	 * @param message	The content of the packet
	 */
	void packetReceived(InetAddress sender, String message);
	/**
	 * @return The address of the PacketListener object
	 */
	InetAddress getAddress();
}
