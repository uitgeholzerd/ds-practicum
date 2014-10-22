package be.uantwerpen.ds.ns.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
	private Timer replyTimer;
	private ArrayList<String> receivedPings;

	public Client() {
		replyTimer = new Timer();
		connectToNetwork();
		System.out.println("Client started on " + getAddress().getHostName());
	}

	/**
	 * Joins the multicast group, sends a discovery message and starts listening for replies.
	 */
	public void connectToNetwork() {
		// set up UDP socket and receive messages
		System.out.println("Connecting to network...");
		udp = new DatagramHandler(udpClientPort, this);
		
		//join multicast group
		group = new MulticastHandler(this);
		try {
			name = getAddress().getHostName();
			group.sendMessage(Protocol.DISCOVER, name + " " + getAddress().getHostAddress());
			
			//DISCOVER_ACK reply should set nameServer, if this doesn't happen the connection failed
			replyTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (nameServer == null){
						System.err.println("Connect to RMI server failed: Connection timed out.");
						System.exit(1);
					}
				}
			}, 3 * 1000);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			String ex = e1.toString();
			//cfh = new ConnectionFailureHandler(name, hash, ex, udpClientPort);
		}


	}

	/**
	 * This method is triggered when a package is sent to this client (uni- or multicast) Depending on the command contained in the message, the client will perform
	 * different actions
	 * 
	 * @param address
	 *            IP of the sender
	 * @param port
	 *            Data containing the command and a message
	 */
	@Override
	public void packetReceived(InetAddress sender, String data) {
		System.out.println("Received message from " + sender.getHostAddress() + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		switch (command) {
		case DISCOVER:
			// The client received a discover-message from a new host and will recalculate its neighbours if needed
			// disregard if it's myself
			if (sender.getHostAddress().equals(getAddress().getHostAddress())) return;
			if (nameServer == null ) {
				System.err.println("Not connected to RMI server, can't process incoming DISCOVER.");
				return;
			}
			try {
				int newNodeHash = nameServer.getShortHash(message[1]);
				System.out.println("New node joined with hash " + newNodeHash);
				
				/*if (newNodeHash > hash){
					if (newNodeHash > nextNodeHash){
						if (nextNodeHash <= hash){
							nextNodeHash = newNodeHash;
							udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + hash);
						}
					} else {
						nextNodeHash = newNodeHash;
						udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + hash);
					}
				} else {
					if (newNodeHash < previousNodeHash){
						if (previousNodeHash >= hash) {
							previousNodeHash = newNodeHash;
							udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + hash);
						}
					} else {
						previousNodeHash = newNodeHash;
					}
				}*/
				
				/*if (previousNodeHash == hash && nextNodeHash == hash){
					System.out.println("Finally, someone to talk to!");
					previousNodeHash = newNodeHash;
					nextNodeHash = newNodeHash;
					udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + hash);
				} else if (newNodeHash < nextNodeHash && newNodeHash > hash){
					System.out.println("It's between me and the next node!");
					nextNodeHash = newNodeHash;
					udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + nextNodeHash);
				} else if (nextNodeHash < hash && newNodeHash < nextNodeHash){
					System.out.println("Now he's the first node!");
					nextNodeHash = newNodeHash;
					udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + nextNodeHash);
				} else if (newNodeHash > previousNodeHash & newNodeHash < hash) {
					System.out.println("It's between me and the previous node!");
					previousNodeHash = newNodeHash;
				} else if (previousNodeHash > hash &&  newNodeHash > previousNodeHash) {
					System.out.println("Now he's the last node!");
					previousNodeHash = newNodeHash;
				}*/
				
				
				if ((newNodeHash < nextNodeHash && newNodeHash > hash) || nextNodeHash == hash || (nextNodeHash < hash && newNodeHash > hash || newNodeHash < nextNodeHash)) {
 					System.out.println("It's between me and the next node!");
 					udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES, hash + " " + nextNodeHash);
 					nextNodeHash = newNodeHash;
 				}
				if ((newNodeHash > previousNodeHash & newNodeHash < hash) || previousNodeHash == hash || (previousNodeHash > hash && ( newNodeHash < hash || newNodeHash > previousNodeHash))) {
 					System.out.println("It's between me and the previous node!");
 					previousNodeHash = newNodeHash;
 				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				String ex = e1.toString();
				//cfh = new ConnectionFailureHandler(name, hash, ex, udpClientPort);
			}
			break;

		case DISCOVER_ACK:
			// Server confirmed registration and answers with its location and the number of nodes
			try {
				Registry registry = LocateRegistry.getRegistry(sender.getHostAddress(), 1099);
				String[] list = registry.list();
/*				for (String string : list) {
					System.out.println(string);
				}*/
				nameServer = (INameServer) registry.lookup(message[1]);
				//test if correctly registered
				String registeredAddress = nameServer.lookupNode(name);
				String localAddress = getAddress().getHostAddress();
				hash = nameServer.getShortHash(name);
				if (registeredAddress.equals(localAddress)){
					System.out.println(message[1] +" self-test success: registered as ["+ hash + "]=" + registeredAddress);
				} else {
					System.err.println(message[1] +" self-test failed: registered as ["+ hash + "]="+ registeredAddress + ", should be " + localAddress);
				}
				
				
			} catch (RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// If this is the only client in the system, it is its own neighbours. Else wait for answer from neighbour (= do nothing)
			if (Integer.parseInt(message[2]) == 1) {
				nextNodeHash = hash;
				previousNodeHash = hash;
			}
			break;

		case SET_NODES:
			// Another client received the discover message and provides this client with its neighbours
			previousNodeHash = Integer.parseInt(message[1]);
			nextNodeHash = Integer.parseInt(message[2]);
			break;

		case SET_PREVNODE:
			// Another client encountered a failed node and provides this client with its new previous node
			previousNodeHash = Integer.parseInt(message[1]);
			break;

		case SET_NEXTNODE:
			// Another client received the fail message and will provide this client with its next node
			nextNodeHash = Integer.parseInt(message[1]);
			break;

		case PING:
			// Respond to other client's ping
			if (udp != null) {
				try {
					udp.sendMessage(sender, udpClientPort, Protocol.PING_ACK, message[1]);
				} catch (IOException e) {
					System.err.println("Failed to respond to ping from " + sender.getAddress() + ": " + e.getMessage());
				}
			}
			break;
		case PING_ACK:
			receivedPings.add(message[1]);
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
			Socket s = new Socket("8.8.8.8", 53);
			address = s.getLocalAddress();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return address;
	}

	/**
	 * This method is used to shutdown the node. It will update its neighbour node, inform the nameserver and close all connections
	 * 
	 * @throws IOException
	 */
	public void shutdown() {
		try {
			// Make previous node and next node neighbours
			String prevNodeIp = nameServer.lookupNodeByHash(previousNodeHash);
			InetAddress prevNode = InetAddress.getByName(prevNodeIp);
			udp.sendMessage(prevNode, udpClientPort, Protocol.SET_NEXTNODE, Integer.toString(nextNodeHash));
			
			String nextNodeIp = nameServer.lookupNodeByHash(nextNodeHash);
			InetAddress nextNode = InetAddress.getByName(nextNodeIp);
			udp.sendMessage(nextNode, udpClientPort, Protocol.SET_PREVNODE, Integer.toString(previousNodeHash));
			
			//Unregister the node on the namserver
			nameServer.unregisterNode(name);
			
			// Close connections
			udp.closeClient();
			group.closeClient();
			System.out.println("Disconnected from network");
			// Shutdown the program - disabled for testing (also, there's a command for that)
			//System.exit(0);
		} catch (IOException e) {
			System.err.println("Shutdown failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Sends a PING message to another client
	 * 
	 * @param name
	 *            The host to ping
	 * @throws IOException
	 */
	public void ping(final String name) throws IOException {
		if (nameServer == null)
			throw new IOException("Not connected to RMI server");
		String ip = nameServer.lookupNode(name);
		if (ip.isEmpty())
			throw new IOException("Host " + name + " not found by server");
		InetAddress host = InetAddress.getByName(ip);
		if (udp == null) {
			System.err.println("Can't ping if not connected!");
			return;
		}

		final String uuid = UUID.randomUUID().toString();
		udp.sendMessage(host, udpClientPort, Protocol.PING, uuid);
		replyTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (receivedPings.remove(uuid)) {
					System.out.println("Ping reply from " + name);
				} else {
					System.err.println("Ping timeout from " + name);
				}
			}
		}, 3 * 1000);
	}

	/**
	 * Sends a PING message to the multicast group
	 * 
	 * @throws IOException
	 */
	public void pingGroup() throws IOException {
		if (group == null) {
			System.err.println("Can't ping if not connected!");
			return;
		}
		group.sendMessage(Protocol.PING, "");
	}
	
	//TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public String lookupNode(String name){
		String result = "";
		try {
			result = nameServer.lookupNode(name);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public String getNodes(){
		return "Previous: " + previousNodeHash + "\nLocal: "+ hash+ "\nNext: "+ nextNodeHash;
	}
}
