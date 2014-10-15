package be.uantwerpen.ds.ns.client;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.TreeMap;

import be.uantwerpen.ds.ns.INameServer;

public class Client {
	
	private TreeMap<Integer, InetAddress> fileMap;
	
	public Client() {
		fileMap = new TreeMap<Integer, InetAddress>();
	}
	

	public static void main(String[] args) {
		try {
			
			INameServer ns = (INameServer) Naming.lookup("//localhost/NameServer");
				ns.registerNode("google", "www.google.com");
				System.out.println("google registered");
				ns.registerNode("localhost", "localhost");
				System.out.println("localhost registered");
			
			System.out.println(ns.lookupNode("google"));
			System.out.println(ns.lookupNode("localhost"));
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	


}
