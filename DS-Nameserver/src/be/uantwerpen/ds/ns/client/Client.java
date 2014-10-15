package be.uantwerpen.ds.ns.client;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
		group.addPacketListener(this);
		group.start();
		try {
			group.sendMessage("Hello group!");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	public static void main(String[] args) {

		Client c = new Client();
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

	@Override
	public void packetReceived(InetAddress sender, String message) {
		// TODO Auto-generated method stub
		System.out.println("Received multicast from " + sender + ": " + message);
	}


}
