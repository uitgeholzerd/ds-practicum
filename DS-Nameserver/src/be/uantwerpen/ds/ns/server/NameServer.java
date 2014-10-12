package be.uantwerpen.ds.ns.server;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import be.uantwerpen.ds.ns.INameServer;

public class NameServer extends UnicastRemoteObject implements INameServer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<Integer, InetAddress> serverMap;

	protected NameServer() throws RemoteException {
		super();
		serverMap = new HashMap<Integer, InetAddress>();
		//TODO lijst uit XML bestand inladen
	}

	
	/**
	 * Adds a server to the name server's map
	 *
	 * @param	name	The name of the server
	 * @param	address	The address at which the server can be found
	 * @return      void
	 */
	public void registerServer(String name, InetAddress address) throws RemoteException {
		serverMap.put(getShortHash(name), address);
		saveMap();
	}

	/**
	 * Return the address of the server the given name
	 *
	 * @param	name	The name of the server
	 * @return	The address of the server      
	 */
	public InetAddress lookupServer(String name) throws RemoteException {
		return serverMap.get(getShortHash(name));
	}

	/**
	 * Removes a server from the name server's map
	 *
	 * @param	name	The name of the server
	 * @return      void
	 */
	public void unregisterServer(String name) {
		int hash = getShortHash(name);
		if (serverMap.containsKey(hash)) {
			serverMap.remove(hash);
		}
		saveMap();
	}

	/**
	 * Saves the current map the the harddisk
	 *
	 * @return      void
	 */
	private void saveMap() {
		try {
			FileOutputStream fao = new FileOutputStream(new File("./names.xml"));
			XMLEncoder xml = new XMLEncoder(fao);
			xml.writeObject(serverMap);
			xml.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static int getShortHash(Object s) {
		return Math.abs(s.hashCode()) % (int) Math.pow(2, 15);
	}
	
	public static void main(String argv[]) throws Exception {
		NameServer ns = new NameServer();
		
		//Test toevoegen
		ns.registerServer("Test1", InetAddress.getByName("localhost"));
		
		//Test ophalen
		InetAddress ip1 = ns.lookupServer("Test1");
		if (ip1 == null) {
			System.out.println("Ip1 not found");
			System.out.println("Test failed");
		}
		
		//Test verwijderen
		ns.unregisterServer("Test1");
		InetAddress ip2 = ns.lookupServer("Test1");
		if (ip2 != null) {
			System.out.println("Ip2  found");
			System.out.println("Test failed");
		}
		
	}
}