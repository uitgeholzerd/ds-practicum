package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.InetAddress;

import be.uantwerpen.ds.ns.client.Client;
import be.uantwerpen.ds.ns.server.NameServer;

/**
 * Contains the methods for handling a connection failure from node to node
 *
 */

public class ConnectionFailureHandler{	
	/**
	 * This method removes the failed node from the network by informing its previous neighbours and the nameserver
	 * 
	 * @param failedNodeName The name of the failed node
	 */
	
	public static void fixFailure(NameServer nameServer, DatagramHandler udp, String failedNodeName) {
		String[] neighbours = nameServer.lookupNeighbours(failedNodeName);
		String ipPrevNode, ipNextNode;
				
			try {
				ipPrevNode = nameServer.lookupNode(neighbours[0]);
				ipNextNode = nameServer.lookupNode(neighbours[1]);
				
				// Send the previous node of the failed node to the next node of the failed note and vice versa
				udp.sendMessage(InetAddress.getByName(ipPrevNode), Client.udpClientPort, Protocol.SET_NEXTNODE, "" + nameServer.getShortHash(neighbours[1]));
				udp.sendMessage(InetAddress.getByName(ipNextNode), Client.udpClientPort, Protocol.SET_PREVNODE, "" + nameServer.getShortHash(neighbours[0]));
				
				nameServer.unregisterNode(failedNodeName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	
}
