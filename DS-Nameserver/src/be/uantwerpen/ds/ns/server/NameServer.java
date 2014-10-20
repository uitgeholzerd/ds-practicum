package be.uantwerpen.ds.ns.server;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
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
	private static final String bindLocation =  "//localhost/NameServer";
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
	}
	
	/**
	 * Binds the current NameServer object to the bindlocation
	 * 
	 */
	private void rmiBind() {
        try { 
			LocateRegistry.createRegistry(rmiPort);
			Naming.bind(bindLocation, this);
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
	public boolean registerNode(String name, String address) {
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
	 * @return The address of the node, returns an empty string if the node was not found
	 */
	public String lookupNode(String name) {
		int hash = getShortHash(name);
		if (!nodeMap.containsKey(hash)) {
			return nodeMap.get(getShortHash(name));
		}
		else {
			return "";
		}
	}
	
	public String lookupSurroundingNodes(int name){
		int prevNode=0,nextNode=0;
		String prevAddress=null, nextAddress=null;
		boolean foundPrev=false, foundNext=false;
		String send;
		
		for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
			if(nodeMap.size()>1){
				if (entry.getKey() == name) {
					if(nodeMap.firstKey()==name){
						prevNode=nodeMap.lastKey();
						prevAddress = nodeMap.get(prevNode);
					}
					foundPrev=true;
				}
				else {
					if(foundPrev==false){
						prevNode = name;
						prevAddress = entry.getValue();
					}
					else{
						if(foundNext==false){
							if(nodeMap.lastKey()==name){
								nextNode=nodeMap.firstKey();
								nextAddress = nodeMap.get(nextNode);
							}
							else{
								nextNode=name;
								nextAddress = entry.getValue();
							}
							foundNext=true;
						}						
					}	
				}
			}
			else {
				prevNode=name;
				nextNode=name;
			}
		}
		
		send = prevNode + " " + prevAddress + " " + nextNode + " " + nextAddress;
		
		return send;
	}

	/**
	 * Removes a node from the name server's map
	 * 
	 * @param name	The name of the node
	 * @return	True if the node was removed, false if the node didn't exist
	 * @throws RemoteException 
	 */
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
	 * Removes a node from the name server's map
	 * 
	 * @param name	The name of the node
	 * @return boolean	True if the node was removed, false if the node didn't exist
	 * @throws RemoteException 
	 */
	public boolean unregisterNode(int hash) {
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
	 * @return The address at which the file can be found
	 */
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


	/**
	 * Generates a short hash based on the input object
	 * 
	 * @return Number between 0 and 32768
	 */
	// TODO: maybe this should be in a shared class instead of using RMI
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
	public void packetReceived(InetAddress sender, String data) {
		System.out.println("Received message from " + sender + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		
		switch (command) {
		case DISCOVER:
			// Register the node and send it the address of the nameserver + the number of clients in the system
			String nodeName = message[1].split("/")[0];
			String nodeIp = message[2].split("/")[0];
			registerNode(nodeName, nodeIp);
			
			try {
				//TODO should be old size but that doesn't make sense? could just subtract 1...
				udp.sendMessage(sender, Client.udpClientPort, Protocol.DISCOVER_ACK, InetAddress.getLocalHost() + "/NameServer" + " " + nodeMap.size());
			}
			 catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case LEAVE:
			unregisterNode(message[1]);
			System.out.println("CLIENT LEFT.");
			break;
			
		case FAIL:
			String dataReturn = lookupSurroundingNodes(Integer.parseInt(message[1]));
			try {
				udp.sendMessage(sender, Client.udpClientPort, Protocol.ID_ACK, dataReturn);
			}
			 catch (IOException e) {
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
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return address;
	}
}