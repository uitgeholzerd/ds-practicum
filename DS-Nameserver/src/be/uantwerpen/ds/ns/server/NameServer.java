package be.uantwerpen.ds.ns.server;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import be.uantwerpen.ds.ns.DatagramHandler;
import be.uantwerpen.ds.ns.INameServer;
import be.uantwerpen.ds.ns.MulticastHandler;
import be.uantwerpen.ds.ns.PacketListener;
import be.uantwerpen.ds.ns.Protocol;
import be.uantwerpen.ds.ns.client.Client;

public class NameServer extends UnicastRemoteObject implements INameServer, PacketListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1957228712436209754L;
	private static final String fileLocation = "./names.xml";
	private static final String bindLocation =  "NameServer";
	private static final int rmiPort = 1099;
	private static final int udpServerPort = 2345;
	
	private SortedMap<Integer, String> nodeMap;
	private MulticastHandler group;
	private DatagramHandler udp;

	@SuppressWarnings("unchecked")
	public NameServer() throws RemoteException {
		super();
		nodeMap = Collections.synchronizedSortedMap(new TreeMap<Integer, String>()) ;
		XMLDecoder decoder = null;
		try {
			decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(fileLocation)));
			nodeMap = (TreeMap<Integer, String>) decoder.readObject();
		} catch (Exception e) {
			System.err.println("Warning: The TreeMap file could not be found or parsed. Starting name server with empty TreeMap");
		} finally {
			if (decoder != null) {
				decoder.close();
			}
		}
		rmiBind();
		
		//join multicast group and receive messages
		group = new MulticastHandler(this);
		
		//set up UDP socket and receive messages
		udp = new DatagramHandler(udpServerPort, this);
		System.out.println("Server started on " + getAddress());
	}
	
	/**
	 * Binds the current NameServer object to the bindlocation
	 * 
	 */
	private void rmiBind() {
        try { 
			LocateRegistry.createRegistry(rmiPort);
			Naming.bind("//" + getAddress().getHostAddress() +"/" + bindLocation, this);
        } catch (MalformedURLException | AlreadyBoundException e) {
            System.err.println("java RMI registry already exists.");
        } catch (RemoteException e) {
        	 System.err.println("RemoteException: " +e.getMessage());
		}
	}

	/**
	 * Adds a node to the name server's map
	 * 
	 * @param	name	The name of the node
	 * @param	address	The address at which the node can be found
	 * @return	True if the node was added, false if the name already exists and the node was not added
	 */
	//TODO de enige reden dat deze methode public is, is voor de tests
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

	/**
	 * Retrieve the address of the node given its name
	 * @param	name	The name of the node
	 * @return The address of the node, returns an empty string if the node was not found
	 */
	public String lookupNode(String name) {
		int hash = getShortHash(name);
		return lookupNodeByHash(hash);
	}

	/**
	 * Retrieve the address of the node given its hash
	 * @param hash The hash of the node
	 * @return The address of the node, returns an empty string if the node was not found
	 */
	public String lookupNodeByHash(int hash) {
		if (nodeMap.containsKey(hash)) {
			return nodeMap.get(hash);
		}
		else {
			return "";
		}
	}


	public boolean unregisterNode(String name) {
		int hash = getShortHash(name);
		boolean success;

		if (nodeMap.containsKey(hash)) {
			nodeMap.remove(hash);
			success = true;
		} else {
			success = false;
		}
		saveMap();
		return success;
	}

	/**
	 * Saves the current map of nodes to the the hard disk
	 * 
	 */
	//TODO de enige reden dat deze methode public is, is voor de tests
	public void saveMap() {
		FileOutputStream fos = null;
		XMLEncoder xml = null;
		
		try {
			fos = new FileOutputStream(fileLocation);
			xml = new XMLEncoder(fos);
			xml.writeObject(nodeMap);
			xml.close();
			fos.close();
		} catch ( IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getFilelocation(String filename) {
		int hash = getShortHash(filename);
		String location = null;

		// If the hash of the file is lower than the hash of the first node, the file can be found on the last node
		if (hash < nodeMap.firstKey()) {
			location = nodeMap.get(nodeMap.lastKey());
		}
		// Else iterate over the map until the biggest node hash lower than the file hash is found
		else {
			for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
				if (entry.getKey() < hash) {
					location = entry.getValue();
				}
			}
		}
		return location;
	}
	
	@Override
	public String[] lookupNeighbours(String name) {
		int hash = getShortHash(name);
		String previousNode, nextNode;
		int index = 0;
		
		// Interate over the map until the node is found
		for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
			if (entry.getKey() == hash) {
				break;
			}
			index++;
		}

		// The previous node in the map is its previous neighbour (cyclic)
		if (index == 0) {
			previousNode = (String) nodeMap.values().toArray()[nodeMap.size() - 1];
		}
		else {
			previousNode = (String) nodeMap.values().toArray()[index - 1];
		}

		// The next node in the map is its next neighbour (cyclic)
		if (index == nodeMap.size() - 1) {
			nextNode = (String) nodeMap.values().toArray()[0];
		}
		else {
			nextNode = (String) nodeMap.values().toArray()[index + 1];
		}

		return new String[]{previousNode, nextNode};
	}

	@Override
	public int getShortHash(Object o) {
		return Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
	}

	/**
	 * This method is triggered when a package is sent to this server (uni- or multicast)
	 * Depending on the command contained in the message, the server will perform different actions
	 * 
	 * @param address	IP of the sender
	 * @param port		Data containing the command and a message
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
				udp.sendMessage(sender, Client.udpClientPort, Protocol.DISCOVER_ACK, "NameServer" + " " + nodeMap.size());
			}
			 catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	
	//TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public String dumpMap(){
		String result = "";
		for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
			result+=entry.getKey()+"="+entry.getValue()+"\n";
		}
		return result;
	}
	public void clearMap(){
		nodeMap = Collections.synchronizedSortedMap(new TreeMap<Integer, String>()) ;
	}

}