package be.uantwerpen.ds.system_y.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.server.INameServer;

public class MessageHandler {
	Client client;
	DatagramHandler udp;
	int hash, nextNodeHash, previousNodeHash;
	
	public MessageHandler(Client client, DatagramHandler udp, int hash, int nextNodeHash, int previousNodeHash){
		this.client = client;
		this.udp = udp;
		this.hash = hash;
		this.nextNodeHash = nextNodeHash;
		this.previousNodeHash = previousNodeHash;
	}
	
	/**
	 * The client received a discover-message from a new host and will recalculate its neighbours if needed Disregard the message if it came from the current node
	 * 
	 * @param sender Host that sent the message
	 * @param message Message cotaining the data
	 */
	public void processDISCOVER(InetAddress sender, String[] message) {
		//
		if (sender.getHostAddress().equals(client.getAddress().getHostAddress())) {
			return;
		}
		if (client.getNameServer() == null) {
			System.err.println("Not connected to RMI server, can't process incoming DISCOVER.");
			return;
		}
		try {
			int newNodeHash = client.getNameServer().getShortHash(message[1]);
			System.out.println("New node joined with hash " + newNodeHash);

			if ((hash < newNodeHash && newNodeHash < nextNodeHash) || nextNodeHash == hash
					|| (nextNodeHash < hash && (hash < newNodeHash || newNodeHash < nextNodeHash))) {
				System.out.println("It's between me and the next node!");
				udp.sendMessage(sender, Client.UDP_CLIENT_PORT, Protocol.SET_NODES, hash + " " + nextNodeHash);
				nextNodeHash = newNodeHash;
			}
			if ((previousNodeHash < newNodeHash && newNodeHash < hash) || previousNodeHash == hash
					|| (previousNodeHash > hash && (hash > newNodeHash || newNodeHash > previousNodeHash))) {
				System.out.println("It's between me and the previous node!");
				previousNodeHash = newNodeHash;
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
	 * @param message Message cotaining the data
	 */
	public void processDISCOVER_ACK(InetAddress sender, String[] message) {
		try {
			Registry registry = LocateRegistry.getRegistry(sender.getHostAddress(), 1099);
			client.setNameServer((INameServer) registry.lookup(message[1]));

			InetAddress registeredAddress = client.getNameServer().lookupNode(client.getName());
			InetAddress localAddress = client.getAddress();
			hash = client.getNameServer().getShortHash(client.getName());
			if (registeredAddress.equals(localAddress)) {
				System.out.println(message[1] + " self-test success: registered as " + hash + " [" + registeredAddress + "]");
			} else {
				System.err.println(message[1] + " self-test failed: registered as " + hash + " [" + registeredAddress + "], should be " + localAddress);
			}

		} catch (RemoteException | NotBoundException e) {
			System.err.println("RMI setup failed: " + e.getMessage());
			e.printStackTrace();
		}
		// If this is the only client in the system, it is its own neighbours. Else wait for answer from neighbour (= do nothing)
		if (Integer.parseInt(message[2]) == 1) {
			nextNodeHash = hash;
			previousNodeHash = hash;
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
