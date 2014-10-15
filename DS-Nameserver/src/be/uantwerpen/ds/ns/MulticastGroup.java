package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class MulticastGroup implements Runnable{
	private MulticastSocket socket = null;
    private DatagramPacket inPacket = null;
    private byte[] inBuf = new byte[256];
    private List<PacketListener> listeners = new ArrayList<PacketListener>();
    private InetAddress address;
    private int port;

    public MulticastGroup(String group, int port){
    	 try {
    	      //Prepare to join multicast group
    		  this.port = port;
    	      socket = new MulticastSocket(port);
    	      address = InetAddress.getByName(group);
    	      //check if valid multicast address
    	      assert(address.isMulticastAddress());
    	      //join group
    	      socket.joinGroup(address);
    	 } catch (IOException e){
    		 e.printStackTrace();
    	 }
    	 
    }
    public void run(){
    	//keep listening for packets
    	while (true) {
            inPacket = new DatagramPacket(inBuf, inBuf.length);
            try {
				socket.receive(inPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            //when packet received, alert listeners
            String msg = new String(inBuf, 0, inPacket.getLength());
            for (PacketListener pl : listeners){
            	pl.packetReceived(inPacket.getAddress(), msg);
            }
          }
    }
	/**
	 * Sends a message to the multicast group
	 * 
	 * @param message The message to send
	 * @throws IOException
	 */
    public void sendMessage(String message) throws IOException{
    	//prepare packet & send to existing socket
    	//TODO: this might not work on the same socket used for listening
    	DatagramPacket outPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
    	socket.send(outPacket);
    }
    /**
     * Adds a listener that will be notified if any packets are received from the multicast group
     * @param pl The listener object
     */
    public void addPacketListener(PacketListener pl){
    	listeners.add(pl);
    }
}
