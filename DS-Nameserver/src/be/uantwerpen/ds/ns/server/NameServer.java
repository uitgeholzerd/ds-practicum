package be.uantwerpen.ds.ns.server;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import be.uantwerpen.ds.ns.INameServer;

public class NameServer extends UnicastRemoteObject implements INameServer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<Integer, InetAddress> names;

	protected NameServer() throws RemoteException {
		super();
		names = new HashMap<Integer, InetAddress>();
	}
	
	public void register(String name, InetAddress address) throws RemoteException {
		names.put(getShortHash(name), address);
		writeNames();
	}

	public InetAddress lookup(String name) throws RemoteException {
		return names.get(getShortHash(name));
	}
	public void unregister(String name) {
		int hash = getShortHash(name);
		if (names.containsKey(hash))
			names.remove(hash);
	}
	private void writeNames(){
		try {
			FileOutputStream fao = new FileOutputStream(new File("./names.xml"));
			//ByteArrayOutputStream bao = new ByteArrayOutputStream();
			XMLEncoder xml = new XMLEncoder(fao);
			xml.writeObject(names);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	
	}
	private static int getShortHash(Object s){
		
		return Math.abs(s.hashCode())%(int)Math.pow(2,15);
		
	}
}