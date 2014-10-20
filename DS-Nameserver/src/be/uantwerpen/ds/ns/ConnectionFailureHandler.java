package be.uantwerpen.ds.ns;

import java.io.IOException;
import java.net.*;

import be.uantwerpen.ds.ns.DatagramHandler;
import be.uantwerpen.ds.ns.PacketListener;
import be.uantwerpen.ds.ns.client.Client;

/**
 * Contains the methods for handling a connection failure from node to node
 *
 */

public class ConnectionFailureHandler implements PacketListener{

	private Client client = new Client();
	private int thisID = client.hash;
	private int failID;
	private String thisMessage;
	private DatagramHandler udp;
	private int thisPort;

	/**
	 * @param failingID	The hash of the id which caused the connection failure
	 * @param message	To know which error has occurred
	 * @param port		Port for receiving and sending messages
	 */

	public ConnectionFailureHandler(int failingID, String message, int port){
		failID = failingID;
		thisMessage = message;
		thisPort = port;
		//set up UDP socket and receive messages
		udp = new DatagramHandler(port, this);
		new Thread(udp).start();
		
		WarnNSFailNode(failID);
	}
	
	
	/**
	 * @param idFailNode	The hash of the id which caused the connection failure 
	 */
	
	public void WarnNSFailNode(int idFailNode){
		try{
			InetAddress ipNameServer = InetAddress.getByName("//localhost/NameServer");
			udp.sendMessage(ipNameServer, thisPort, Protocol.FAIL, ""+idFailNode);
		}catch(IOException e){
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
		// TODO Auto-generated method stub
		InetAddress ipPrevNode, ipNextNode;
		int idPrevNode, idNextNode;
		String[] command = message.split(" ");
				
		if (command[0].equals(Protocol.ID_ACK)) {
			//params zo binnenkrijgen vanuit server int id1, InetAddress ip1, int id2, InetAddress ip2
			// id1 = command[1], ip1 = command[2], id2 = command[3], ip2 = command[4]
				
			try {
				idPrevNode = Integer.parseInt(command[1]);
				idNextNode = Integer.parseInt(command[3]);
				ipPrevNode = InetAddress.getByName(command[2]);
				ipNextNode = InetAddress.getByName(command[4]);
				
				if(thisID == idPrevNode){
					//hash prevNode veranderen
					udp.sendMessage(ipNextNode, thisPort, Protocol.PREVNODE, ""+idPrevNode);
				}
				if(thisID == idNextNode){
					//hash nextNode veranderen
					udp.sendMessage(ipPrevNode, thisPort,  Protocol.NEXTNODE, "" + idNextNode);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
