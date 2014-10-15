package be.uantwerpen.ds.ns.client;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.util.TreeMap;

import be.uantwerpen.ds.ns.INameServer;
import be.uantwerpen.ds.ns.MulticastGroup;
import be.uantwerpen.ds.ns.PacketListener;

public class Client implements PacketListener {
	
	private TreeMap<Integer, InetAddress> fileMap;
	private MulticastGroup group;
	
	public Client() {
		fileMap = new TreeMap<Integer, InetAddress>();
		group = new MulticastGroup("225.6.7.8", 5678);
	}

	public static void main(String[] args) {
		try {		
			INameServer ns = (INameServer) Naming.lookup("//localhost/NameServer");
			try {
				ns.registerNode("google", InetAddress.getByName("www.google.com"));
				System.out.println("google registered");
				ns.registerNode("localhost", InetAddress.getLocalHost());
				System.out.println("localhost registered");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(ns.lookupNode("google"));
			System.out.println(ns.lookupNode("localhost"));
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void packetReceived(InetAddress sender, String message) {
		// TODO Auto-generated method stub
		System.out.println("Received multicast from " + sender + ": " + message);
	}
	


}
