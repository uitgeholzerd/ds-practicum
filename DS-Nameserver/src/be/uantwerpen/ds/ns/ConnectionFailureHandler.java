package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import be.uantwerpen.ds.ns.client.Client;
import be.uantwerpen.ds.ns.server.NameServer;

/**
 * Contains the methods for handling a connection failure from node to node
 *
 */

public class ConnectionFailureHandler implements PacketListener{

	private NameServer nameServer;
	private DatagramHandler udp;

	/**
	 * @param failingID		The hash of the id which caused the connection failure
	 * @param clientHash	The hash of client
	 * @param message		To know which error has occurred
	 * @param port			Port for receiving and sending messages
	 */
	public ConnectionFailureHandler(NameServer nameServer, DatagramHandler udp){
		this.nameServer = nameServer;
		this.udp = udp;
	}
	
	public void fixFailure(String failedNodeName) {
		String[] neighbours = nameServer.lookupNeighbours(failedNodeName);
		
		String ipPrevNode, ipNextNode;
				
			try {
				ipPrevNode = nameServer.lookupNode(neighbours[0]);
				ipNextNode = nameServer.lookupNode(neighbours[1]);
				
				// Send the previous node of the failed node to the next node of the failed note and vice versa
				udp.sendMessage(InetAddress.getByName(ipPrevNode), Client.udpClientPort, Protocol.NEXTNODE, "" + neighbours[1]);
				udp.sendMessage(InetAddress.getByName(ipNextNode), Client.udpClientPort, Protocol.PREVNODE, "" + neighbours[0]);
				
				nameServer.unregisterNode(failedNodeName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	
	
	/**
	 * This method is triggered when a package is sent to this class (unicast)
	 * Depending on the command contained in the message, the handler will perform different actions
	 * 
	 * @param address	IP of the sender
	 * @param port		Data containing the command and a message
	 */
	
	@Override
	public void packetReceived(InetAddress sendToID, String message) {

	}
	
	@Override
	public InetAddress getAddress() {
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return address;
	}
}
