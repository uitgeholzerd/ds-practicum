package be.uantwerpen.ds.ns;

import java.net.InetAddress;

public interface PacketListener {
	public void packetReceived(InetAddress sender, String message);
}
