package be.uantwerpen.ds.ns.server;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
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
import be.uantwerpen.ds.ns.MulticastGroup;
import be.uantwerpen.ds.ns.PacketListener;
import be.uantwerpen.ds.ns.Protocol;

public class NameServer extends UnicastRemoteObject implements INameServer, PacketListener{

	
	/**
	 * 
	 */
	// TODO: should we put these in a shared class so settings are shared?
	private static final long serialVersionUID = -1957228712436209754L;
	private static final String fileLocation = "./names.xml";
	private static final String bindLocation =  "//localhost/NameServer";
	private static final String multicastAddress = "225.6.7.8";
	private static final int multicastPort = 5678;
	private static final int rmiPort = 1099;
	private static final int udpServerPort = 2345;
	private static final int udpClientPort = 3456;
	
	private SortedMap<Integer, String> nodeMap;
	private MulticastGroup group;
	private DatagramHandler udp;

	@SuppressWarnings("unchecked")
	protected NameServer() throws RemoteException {
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
		group = new MulticastGroup(multicastAddress, multicastPort);
		group.addPacketListener(this);
		new Thread(group).start();
		
		//set up UDP socket and receive messages
		udp = new DatagramHandler(udpServerPort);
		udp.addPacketListener(this);
		new Thread(udp).start();
	}
	
	/**
	 * Binds the current NameServer object to the bindlocation
	 * 
	 */
	private void rmiBind() {
        try { 
			LocateRegistry.createRegistry(rmiPort);
			Naming.bind(bindLocation, this);
	        System.out.println("NameServer is ready at:" + bindLocation);
            System.out.println("java RMI registry created.");
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
	 * @return	boolean	True if the node was added, false if the name already exists and the node was not added
	 */
	public boolean registerNode(String name, String address) throws RemoteException {
		int hash = getShortHash(name);
		boolean success;
		if (!nodeMap.containsKey(hash)) {
			nodeMap.put(hash, address);
			success = true;
		} else {
			success = false;
		}
		saveMap();
		return success;
	}

	/**
	 * Retrieve the address of the node the given name
	 * 
	 * @param	name	The name of the node
	 * @return	InetAddress The address of the node, returns null if the node was not found
	 */
	public String lookupNode(String name) throws RemoteException {
		return nodeMap.get(getShortHash(name));
	}

	/**
	 * Removes a node from the name server's map
	 * 
	 * @param name	The name of the node
	 * @return boolean	True if the node was removed, false if the node didn't exist
	 * @throws RemoteException 
	 */
	public boolean unregisterNode(String name) throws RemoteException {
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
	 * @return void
	 */
	private void saveMap() {
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

	/**
	 * Retrieve the address of the node that stores the file with the given name
	 * 
	 * @return InetAddress The address at which the file can be found
	 */
	public String getFilelocation(String filename) throws RemoteException {
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

	/**
	 * Generates a short hash based on the input object
	 * 
	 * @return int	Number between 0 and 32768
	 */
	// TODO: maybe this should be in a shared class instead of using RMI
	public int getShortHash(Object o) throws RemoteException {
		return Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
	}
	
	@Override
	public void packetReceived(InetAddress sender, String message) {
		// TODO Auto-generated method stub
		System.out.println("Received message from " + sender + ": " + message);
		String[] command = message.split(" ");
		if (command[0].equals(Protocol.REGISTER.getCommand())) {
			// new node wants to register
			// get name/ip from message
			String name = command[1].split("/")[0];
			String ip = command[1].split("/")[1];
			try {
				registerNode(name, ip);
				//respond with new nodemap size
				//TODO should be old size but that doesn't make sense? could just subtract 1...
				udp.sendMessage(sender, udpClientPort, String.format(Protocol.REG_ACK.toString(), nodeMap.size()));
			} catch (RemoteException e) {
				// TODO This shouldn't happen as it's called locally?
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (command[0].equals(Protocol.REGISTER.getCommand())) {
			
		}

	}

	public static void main(String[] args) throws Exception {
		NameServer ns = new NameServer();
		boolean failed = false;
		boolean test = false;

		// Test toevoegen
		test = ns.registerNode("aaa123", "localhost");
		if (!test) {
			System.out.println("Fout bij het toevoegen");
			failed = true;
		}

		// Test dubbel toevoegen
		test = ns.registerNode("aaa123", "localhost");
		if (test) {
			System.out.println("Geen fout bij dubbel toevoegen");
			failed = true;
		}

		// Test ophalen
		String ip1 = ns.lookupNode("aaa123");
		if (ip1 == null) {
			System.out.println("Ip1 not found");
			failed = true;
		}

		// Test verwijderen
		test = ns.unregisterNode("aaa123");
		String ip2 = ns.lookupNode("aaa123");
		if (!test || ip2 != null) {
			System.out.println("Ip2  found");
			failed = true;
		}

		// Test dubbel verwijderen
		test = ns.unregisterNode("aaa123");
		String ip3 = ns.lookupNode("aaa123");
		if (test || ip3 != null) {
			System.out.println("Ip2  found");
			failed = true;
		}
		
		//Bestandsnaam opvragen
		String node1Name = "fileNode";
		String node1Location = "1.2.3.4";
		String node2Name = "azerty";
		String node2Location = "4.5.6.6";
		String filename = "1";
		
		int node1Hash =  Math.abs(node1Name.hashCode()) % (int) Math.pow(2, 15);
		int node2Hash =  Math.abs(node2Name.hashCode()) % (int) Math.pow(2, 15);
		int fileHash =  Math.abs(filename.hashCode()) % (int) Math.pow(2, 15);
		
		ns.registerNode(node1Name, node1Location);
		ns.registerNode(node2Name, node2Location);
		
		String location = ns.getFilelocation(filename);
		
		System.out.println("Node 1 hash: " + node1Hash + ", location: " + node1Location);
		System.out.println("Node 2 hash: " + node2Hash + ", location: " + node2Location);
		System.out.println("File hash: " + fileHash + ", location: " + location);
		
		if ((fileHash < node1Hash && !location.equals(node2Location)) || (fileHash > node1Hash && !location.equals(node1Location))) {
			System.out.println("File lookup failed");
			failed = true;
		}
		//TODO test voor gelijktijd opvragen
		
		System.out.println();
		if (failed) {
			System.out.println("One or more tests failed");
		} else {
			System.out.println("Test completed successfully");
		}

	}
}