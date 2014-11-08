package be.uantwerpen.ds.system_y.connection;

import java.net.InetAddress;

public interface PacketListener {
	/**
	 * This method is triggered when a package is sent to the object the
	 * implements this interface (uni- or multicast)
	 * 
	 * @param sender The address of the sender of the packet
	 * @param message The content of the packet
	 */
	void packetReceived(InetAddress sender, String message);

	/**
	 * Returns the IP address of the system the program is executed on
	 * 
	 * @return The address of the PacketListener object
	 */
	InetAddress getAddress();
}
