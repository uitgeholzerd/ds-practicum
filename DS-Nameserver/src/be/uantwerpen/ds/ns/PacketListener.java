package be.uantwerpen.ds.ns;

import java.net.InetAddress;

public interface PacketListener {
	void packetReceived(InetAddress sender, String message);
	InetAddress getAddress();
}
