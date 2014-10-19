package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author seb
 *
 */

public class DatagramHandler implements Runnable{
	
	private DatagramSocket socket;
	DatagramPacket inPacket;
	byte[] buffer;
	private List<PacketListener> listeners = new ArrayList<PacketListener>();
	
	/**
	 * @param port	UDP port to listen on and send outgoing messages to
	 */
	public DatagramHandler(int port){
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		buffer = new byte[1024];
		// keep listening for UDP datagrams
		while (true) {
			inPacket = new DatagramPacket (buffer, buffer.length);
			try {
				socket.receive(inPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// get message out of received datagram
            String msg = new String(buffer, 0, inPacket.getLength());
            // notify all registered listeners
            for (PacketListener pl : listeners){
            	pl.packetReceived(inPacket.getAddress(), msg);
            }
		}
	}
	/**
	 * Sends a message via UDP
	 * 
	 * @param to	Remote address
	 * @param port 	Remote port
	 * @param message The message to send
	 * @throws IOException
	 */
	public void sendMessage(InetAddress to, int port,  String message) throws IOException{
		DatagramPacket outPacket = new DatagramPacket (message.getBytes(), message.length(),to, port);
		socket.send(outPacket);
	}
    /**
     * Adds a listener that will be notified if any packets are received on this UDP port
     * @param pl The listener object
     */
    public void addPacketListener(PacketListener pl){
    	listeners.add(pl);
    }
}
