package be.uantwerpen.ds.system_y.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import be.uantwerpen.ds.system_y.agent.FileAgent;
import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.server.INameServer;
import be.uantwerpen.ds.system_y.server.NameServer;

/**
 * Handles all complicated logic when a command is received
 *
 */
public class MessageHandler {
	Client client;
	DatagramHandler udp;
	MulticastHandler group;
	
	
	public MessageHandler(Client client, DatagramHandler udp, MulticastHandler group){
		this.client = client;
		this.udp = udp;
		this.group = group;
	}
	
	/**
	 * The client received a discover-message from a new host and will recalculate its neighbours if needed Disregard the message if it came from the current node
	 * 
	 * @param sender Host that sent the message
	 * @param message Message cotaining the data
	 */
	public void processNODE_JOINED(InetAddress sender, String[] message) {
		//
		
		if (client.getNameServer() == null) {
			System.err.println("Not connected to RMI server, can't process incoming DISCOVER.");
			return;
		}
		try {
			int newNodeHash = client.getNameServer().getShortHash(message[1]);
			System.out.println("New node joined with hash " + newNodeHash);

			// Check if the new node is the previous and/or next neighbour of the current node
			if ((client.getHash() < newNodeHash && newNodeHash < client.getNextNodeHash()) || client.getNextNodeHash() == client.getHash()
					|| (client.getNextNodeHash() < client.getHash() && (client.getHash() < newNodeHash || newNodeHash < client.getNextNodeHash()))) {
				System.out.println("It's between me and the next node!");
				udp.sendMessage(sender, Client.UDP_CLIENT_PORT, Protocol.SET_NODES, client.getHash() + " " + client.getNextNodeHash());
				client.setNextNodeHash(newNodeHash);
				client.recheckOwnedFiles();
			}
			if ((client.getPreviousNodeHash() < newNodeHash && newNodeHash < client.getHash()) || client.getPreviousNodeHash() == client.getHash()
					|| (client.getPreviousNodeHash() > client.getHash() && (client.getHash() > newNodeHash || newNodeHash > client.getPreviousNodeHash()))) {
				System.out.println("It's between me and the previous node!");
				client.setPreviousNodeHash(newNodeHash);
			}

		} catch (IOException e) {
			System.err.println("RMI name lookup failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Server confirmed registration and answers with its location and the number of nodes
	 * 
	 * @param sender Host that sent the message
	 * @param message Message containing the data
	 */
	public void processDISCOVER_ACK(InetAddress sender, String[] message) {
		int nodeCount = Integer.parseInt(message[2]);
		try {
			Registry registry = LocateRegistry.getRegistry(sender.getHostAddress(), NameServer.rmiPort);
			client.setNameServer((INameServer) registry.lookup(message[1]));
			
			InetAddress registeredAddress = client.getNameServer().lookupNodeByName(client.getName());
			InetAddress localAddress = client.getAddress();
			client.setHash(client.getNameServer().getShortHash(client.getName()));
			if (registeredAddress.equals(localAddress)) {
				System.out.println(message[1] + " self-test success: registered as " + client.getHash() + " [" + registeredAddress + "], " + nodeCount + " nodes in network.");
			} else {
				System.err.println(message[1] + " self-test failed: registered as " + client.getHash() + " [" + registeredAddress + "], should be " + localAddress);
			}

		} catch (RemoteException | NotBoundException e) {
			System.err.println("RMI setup failed: " + e.getMessage());
			e.printStackTrace();
		}
		
		// If this is the only client in the system, it is its own neighbours. Else wait for answer from neighbour (= do nothing)
		if (nodeCount == 1) {
			System.out.println("I'm all alone, setting next and previous node to myself ("+ client.getHash() + ")");
			client.setNextNodeHash(client.getHash());
			client.setPreviousNodeHash(client.getHash()); 
			client.receiveAgent(new FileAgent());
		} else { 
			try {
				group.sendMessage(Protocol.NODE_JOINED, client.getName() + " " + client.getAddress().getHostAddress());
			} catch (IOException e) {
				System.err.println("Error while sending group message in MessageHandler");
				e.printStackTrace();
			}
		}
	}
	


	/**
	 * Respond to other client's ping
	 * 
	 * @param sender
	 * @param message
	 */
	public void processPING(InetAddress sender, String[] message) {
		if (udp != null) {
			try {
				udp.sendMessage(sender, Client.UDP_CLIENT_PORT, Protocol.PING_ACK, message[1]);
			} catch (IOException e) {
				System.err.println("Failed to respond to ping from " + sender.getAddress() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

}
