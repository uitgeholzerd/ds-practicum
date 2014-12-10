package be.uantwerpen.ds.system_y.server;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.connection.DatagramHandler;
import be.uantwerpen.ds.system_y.connection.MulticastHandler;
import be.uantwerpen.ds.system_y.connection.PacketListener;
import be.uantwerpen.ds.system_y.connection.Protocol;

public class NameServer extends UnicastRemoteObject implements INameServer, PacketListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1957228712436209754L;
	private static final String fileLocation = "./names.xml";
	private static final String bindLocation = "NameServer";
	public static final int rmiPort = 1099;
	private static final int udpServerPort = 2345;

	private SortedMap<Integer, String> nodeMap;

	private MulticastHandler group;
	private DatagramHandler udp;

	public NameServer() throws RemoteException {
		super();
		nodeMap = Collections.synchronizedSortedMap(new TreeMap<Integer, String>());

		loadMap();
		rmiBind();

		try {
			// join multicast group and receive messages
			group = new MulticastHandler(this);

			// set up UDP socket and receive messages
			udp = new DatagramHandler(udpServerPort, this);

			System.out.println("Server started on " + getAddress());
		} catch (IOException e) {
			System.err.println("Server set-up failed: " + e.getMessage());
			udp.closeClient();
			group.closeClient();
		}
	}

	/**
	 * Loads the current map of nodes from the the hard disk
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void loadMap() {
		XMLDecoder decoder = null;
		try {
			decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(fileLocation)));
			nodeMap = (TreeMap<Integer, String>) decoder.readObject();
			System.out.println("Loaded " + nodeMap.keySet().size() + " nodes from " + fileLocation);
		} catch (Exception e) {
			System.out.println("Warning: The TreeMap file could not be found or parsed. Starting name server with empty TreeMap");
		} finally {
			if (decoder != null) {
				decoder.close();
			}
		}
	}

	/**
	 * Saves the current map of nodes to the the hard disk
	 * 
	 */
	// TODO de enige reden dat deze methode public is, is voor de tests
	public void saveMap() {
		FileOutputStream fos = null;
		XMLEncoder xml = null;

		try {
			fos = new FileOutputStream(fileLocation);
			xml = new XMLEncoder(fos);
			xml.writeObject(nodeMap);
			xml.close();
			fos.close();
		} catch (IOException e) {
			System.err.println("Failed to save map: " + e.getMessage());
		}
	}

	/**
	 * Binds the current NameServer object to the bindlocation
	 * 
	 */
	private void rmiBind() {
		try {
			LocateRegistry.createRegistry(rmiPort);
			Naming.bind("//" + getAddress().getHostAddress() + "/" + bindLocation, this);
		} catch (MalformedURLException | AlreadyBoundException e) {
			System.err.println("java RMI registry already exists.");
		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e.getMessage());
		}
	}

	/**
	 * Adds a node to the name server's map
	 * 
	 * @param name The name of the node
	 * @param address The address at which the node can be found
	 * @return True if the node was added, false if the name already exists and the node was not added
	 */
	// TODO de enige reden dat deze methode public is, is voor de tests
	public boolean registerNode(String name, String address) {
		int hash = getShortHash(name);
		boolean success;
		// TODO like this it will refuse to re-register a node after its IP has changed
		if (!nodeMap.containsKey(hash)) {
			nodeMap.put(hash, address);
			success = true;
			System.out.println("Added node " + name + "/" + address + " with hash " + hash);
		} else {
			System.out.println("Node already exists: " + hash);
			success = false;
		}
		saveMap();
		return success;
	}

	@Override
	public boolean unregisterNode(int nodeHash) {
		boolean success;

		if (nodeMap.containsKey(nodeHash)) {
			nodeMap.remove(nodeHash);
			success = true;
		} else {
			success = false;
		}
		saveMap();
		return success;
	}

	/**
	 * Retrieve the address of the node given its name
	 * 
	 * @param name The name of the node
	 * @return The address of the node, returns an empty string if the node was not found
	 */
	public InetAddress lookupNodeByName(String name) {
		int hash = getShortHash(name);
		return lookupNode(hash);
	}

	/**
	 * Retrieve the address of the node given its hash
	 * 
	 * @param hash The hash of the node
	 * @return The address of the node, returns an empty string if the node was not found
	 */
	public InetAddress lookupNode(int hash) {
		if (nodeMap.containsKey(hash)) {
			try {
				return InetAddress.getByName(nodeMap.get(hash));
			} catch (UnknownHostException e) {
				System.err.println("NameServer - Host not found");
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int reverseLookupNode(String address) throws RemoteException {
		int hash = -1;
		
		for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
			if (entry.getValue().equals(address)) {
				hash = entry.getKey();
				break;
			}
		}
		
		return hash;
	}
	
	/**
	 * Calculates the location of the file in the given map
	 * 
	 * @param fileName
	 * @param map
	 * @return
	 */
	public InetAddress getFilelocationFromMap(String fileName, SortedMap<Integer, String> map) {
		int hash = getShortHash(fileName);
		String location = null;

		// If the hash of the file is lower than the hash of the first node, the file can be found on the last node
		if (hash < map.firstKey()) {
			location = map.get(map.lastKey());
		}
		// Else iterate over the map until the biggest node hash lower than the file hash is found
		else {
			for (Map.Entry<Integer, String> entry : map.entrySet()) {
				if (entry.getKey() < hash) {
					location = entry.getValue();
				}
			}
		}
		try {
			return InetAddress.getByName(location);
		} catch (UnknownHostException e) {
			System.err.println("NameServer - Host not found");
			return null;
		}
	}


	@Override
	public InetAddress getFilelocation(String fileName) {
		return getFilelocationFromMap(fileName, nodeMap);
	}
	
	@Override
	public boolean isFileOwner(int nodeHash, InetAddress nodeLocation, String fileName) throws RemoteException {
		SortedMap<Integer, String> clonedMap = new TreeMap<Integer, String>();
		nodeMap.putAll(clonedMap);
		clonedMap.put(nodeHash, nodeLocation.getHostAddress());
		
		InetAddress location = getFilelocationFromMap(fileName, clonedMap);
		return nodeLocation.equals(location);
	}
	
	

	@Override
	public InetAddress[] lookupNeighbours(int nodeHash) {
		String previousNode, nextNode;
		int index = 0;
		
		// If the node was not found, return null
		if (!nodeMap.containsKey(nodeHash)) {
			return null;
		}

		// Interate over the map until the node is found
		for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
			if (entry.getKey() == nodeHash) {
				break;
			}
			index++;
		}

		// The previous node in the map is its previous neighbour (cyclic)
		if (index == 0) {
			previousNode = (String) nodeMap.values().toArray()[nodeMap.size() - 1];
		} else {
			previousNode = (String) nodeMap.values().toArray()[index - 1];
		}

		// The next node in the map is its next neighbour (cyclic)
		if (index == nodeMap.size() - 1) {
			nextNode = (String) nodeMap.values().toArray()[0];
		} else {
			nextNode = (String) nodeMap.values().toArray()[index + 1];
		}

		try {
			InetAddress prev =  InetAddress.getByName(previousNode);
			InetAddress next =  InetAddress.getByName(nextNode);
			return new InetAddress[] {prev, next};
		} catch (UnknownHostException e) {
			System.err.println("NameServer - Host not found");
			return null;
		}
	}

	/**
	 * This method is triggered when a package is sent to this server (uni- or multicast) Depending on the command contained in the message, the server will perform
	 * different actions
	 * 
	 * @param address IP of the sender
	 * @param port Data containing the command and a message
	 */

	@Override
	public void packetReceived(InetAddress sender, String data) {
		System.out.println("Received message from " + sender + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);

		switch (command) {
		case DISCOVER:
			// Register the node and send it the address of the nameserver + the number of clients in the system
			String nodeName = message[1];
			String nodeIp = message[2];
			registerNode(nodeName, nodeIp);

			try {
				udp.sendMessage(sender, Client.UDP_CLIENT_PORT, Protocol.DISCOVER_ACK, "NameServer" + " " + nodeMap.size());
			} catch (IOException e) {
				System.err.println("Failed to respond to client DISCOVER: " + e.getMessage());
			}
			break;
		case NODE_JOINED:
			break;
		default:
			System.err.println("Command not found");
			break;
		}

	}

	@Override
	public int getShortHash(Object o) {
		if (o == null) {
			return -1;
		}
		else {
			return Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
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
			System.err.println("Failed to get local IP address: " + e.getMessage());
		}
		return address;
	}
	

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public String debugDumpMap() {
		String result = "Nodes in map:\n";
		for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
			result += entry.getKey() + "=" + entry.getValue() + "\n";
		}
		return result;
	}

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public void debugClearMap() {
		nodeMap = Collections.synchronizedSortedMap(new TreeMap<Integer, String>());
	}

}
