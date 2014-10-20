package be.uantwerpen.ds.ns.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import be.uantwerpen.ds.ns.DatagramHandler;
import be.uantwerpen.ds.ns.INameServer;
import be.uantwerpen.ds.ns.MulticastHandler;
import be.uantwerpen.ds.ns.PacketListener;
import be.uantwerpen.ds.ns.Protocol;


public class Client implements PacketListener {

	public static final int udpClientPort = 3456;

	private MulticastHandler group;
	private DatagramHandler udp;
	private INameServer nameServer;
	private String name;
	private int hash;
	private int previousNodeHash;
	private int nextNodeHash;
	private InetAddress serverAddress;


	public Client() {
		joinMulticastGroup();
		try {
			name = InetAddress.getLocalHost().getHostName();
			group.sendMessage(Protocol.DISCOVER, name + " " + InetAddress.getLocalHost());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// set up UDP socket and receive messages
		udp = new DatagramHandler(udpClientPort, this);
	}

	/**
	 * Joins a multicast group and starts a thread to listen for incoming messages
	 * 
	 * @param address	IP of the multicast group
	 * @param port		Port for receiving and sending messages
	 */
	private void joinMulticastGroup() {
		group = new MulticastHandler(this);
		new Thread(group).start();

	}

	/**
	 * This method is triggered when a package is sent to this client (uni- or multicast)
	 * Depending on the command contained in the message, the client will perform different actions
	 * 
	 * @param address	IP of the sender
	 * @param port		Data containing the command and a message
	 */
	@Override
	public void packetReceived(InetAddress sender, String data) {
		// TODO Should we add a field to determine if UDP or multicast?
		System.out.println("Received message from " + sender + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		switch (command) {
		case DISCOVER:
			// The client received a discover-message from a new host and will recalculate its neighbours if needed
			try {
				int newNodeHash = nameServer.getShortHash(message[1].split("/")[0]);
				if (hash < newNodeHash && newNodeHash < nextNodeHash) {
					udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + nextNodeHash);
					nextNodeHash = newNodeHash;
				}
				else if (previousNodeHash < newNodeHash && newNodeHash < hash) {
					previousNodeHash = newNodeHash;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
			
		case DISCOVER_ACK:
			// Server confirmed registration and answers with its location and the number of nodes
			try {
				serverAddress = sender;
				// Try to bind the NameServer
				nameServer = (INameServer) Naming.lookup(message[1]);
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// If this is the only client in the system, it is its own neighbours. Else wait for answer from neighbour (= do nothing)
			if (Integer.parseInt(message[2]) == 1) {
				// this is the only node
				nextNodeHash = hash;
				previousNodeHash = hash;
			}
			break;
			
		case SET_NODES:
			// Another client received the discover message and will provide this client with its neighbours
			previousNodeHash = Integer.parseInt(message[1]);
			nextNodeHash = Integer.parseInt(message[2]);
			break;
			
		case PREVNODE:
			// Another client received the fail message and will provide this client with its previous node
			previousNodeHash = Integer.parseInt(message[1]);
			break;
			
		case NEXTNODE:
			// Another client received the fail message and will provide this client with its next node
			nextNodeHash = Integer.parseInt(message[1]);
			break;
			
		default:
			System.err.println("Command not found");
			break;
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
	
	
	/**
	 * This method will be called when the client will exit the group and shut itself down
	 *
	 * @param port		Data containing the command and a message
	 */
	
	public void Shutdown(){
	
		WarnPrevNode(previousNodeHash,nextNodeHash);
		WarnNextNode(nextNodeHash,previousNodeHash);
		WarnNSExitNode(hash);
		
		udp.closeClient();
	}
	
	/**
	 * This method will be called to inform the server about its exit
	 *
	 * @param id		client hash
	 */
	public void WarnNSExitNode(int idExitNode){
		try{
			udp.sendMessage(serverAddress, udpClientPort, Protocol.LEAVE, ""+idExitNode);
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}
	}
	
	/**
	 * This method will be called to change the next node of the previous client
	 *
	 * @param prevNode		previous client hash
	 * @param nextNode		next client hash
	 */
	
	public void WarnPrevNode(int idPrevNode, int idNextNode){
		try{
			InetAddress ipNode = InetAddress.getByName("" + idPrevNode);
			udp.sendMessage(ipNode, udpClientPort, Protocol.NEXTNODE, ""+idNextNode);
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}
	}
	
	/**
	 * This method will be called to change the previous node of the next client
	 *
	 * @param nextNode		next client hash
	 * @param prevNode		previous client hash
	 */
	
	public void WarnNextNode(int idNextNode, int idPrevNode){
		try{
			InetAddress ipNode = InetAddress.getByName("" + idNextNode);
			udp.sendMessage(ipNode, udpClientPort, Protocol.PREVNODE, ""+idPrevNode);
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
