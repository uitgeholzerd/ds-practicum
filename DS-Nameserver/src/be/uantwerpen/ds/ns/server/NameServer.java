package be.uantwerpen.ds.ns.server;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import be.uantwerpen.ds.ns.INameServer;

public class NameServer extends UnicastRemoteObject implements INameServer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<Integer, InetAddress> nodeMap;

	protected NameServer() throws RemoteException {
		super();
		nodeMap = Collections.synchronizedMap(new TreeMap<Integer, InetAddress>());
		//TODO lijst uit XML bestand inladen
	}

	/**
	 * Adds a node to the name server's map
	 *
	 * @param	name	The name of the node
	 * @param	address	The address at which the node can be found
	 * @return	boolean	Returns true if the node is added, false if the name already exists and the node is not added
	 */
	public boolean registerNode(String name, InetAddress address) throws RemoteException {
		int hash = getShortHash(name);
		if (!nodeMap.containsKey(hash)) {
			nodeMap.put(hash, address);
			return true;
		}
		else {
			return false;
		}
		//saveMap();
	}

	/**
	 * Retrieve the address of the node the given name
	 *
	 * @param	name	The name of the node
	 * @return	InetAddress	The address of the node, returns null if the node was not found      
	 */
	public InetAddress lookupNode(String name) throws RemoteException {
		return nodeMap.get(getShortHash(name));
	}

	/**
	 * Removes a node from the name server's map
	 *
	 * @param	name	The name of the node
	 * @return  void
	 */
	public boolean unregisterNode(String name) {
		int hash = getShortHash(name);
		if (nodeMap.containsKey(hash)) {
			nodeMap.remove(hash);
			return true;
		}
		else {
			return false;
		}
		//saveMap();
	}

	/**
	 * Saves the current map of nodes the the hard disk
	 *
	 * @return      void
	 */
	//TODO werkt niet
	private void saveMap() {
		try {
			FileOutputStream fao = new FileOutputStream(new File("./names.xml"));
			XMLEncoder xml = new XMLEncoder(fao);
			xml.writeObject(nodeMap);
			xml.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * Retrieve the address of the node that stores the file with the given name
	 *
	 * @return	InetAddress		The address at which the file can be found
	 */
	public InetAddress getFilelocation(String filename) throws RemoteException {
		int hash = getShortHash(filename);
		InetAddress location = null;
		
		//If the hash of the file is lower than the hash of the first node, the file can be found on the last node
		if (hash < nodeMap.firstKey() ) {
			location = nodeMap.lastEntry().getValue();
		}
		//Else iterate over the map until a node hash lower than the file hash is found
		else {
			for (Map.Entry<Integer, InetAddress> entry : nodeMap.entrySet()) {
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
	 * @return	int		number between 0 and 32768
	 */
	private static int getShortHash(Object s) {
		return Math.abs(s.hashCode()) % (int) Math.pow(2, 15);
	}
	
	public static void main(String[] args) throws Exception {
		NameServer ns = new NameServer();
		boolean failed = false;
		
		//Test toevoegen
		ns.registerNode("Test1", InetAddress.getByName("localhost"));
		
		//Test ophalen
		InetAddress ip1 = ns.lookupNode("Test1");
		if (ip1 == null) {
			System.out.println("Ip1 not found");
			failed = true;
		}
		
		//Test verwijderen
		ns.unregisterNode("Test1");
		InetAddress ip2 = ns.lookupNode("Test1");
		if (ip2 != null) {
			System.out.println("Ip2  found");
			failed = true;
		}
		
		if (failed) {
			System.out.println("One or more tests failed");
		}
		else {
			System.out.println("Test completed successfully");
		}
		
	}
}