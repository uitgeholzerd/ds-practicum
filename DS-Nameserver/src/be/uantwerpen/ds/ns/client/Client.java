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
	
	private static final String multicastAddress = "225.6.7.8";
	private static final int multicastPort = 5678;
	private static final String bindLocation =  "//localhost/NameServer";

	
	private TreeMap<Integer, InetAddress> fileMap;
	private MulticastGroup group;
	
	public Client() {
		fileMap = new TreeMap<Integer, InetAddress>();
		joinMulticastGroup(multicastAddress, multicastPort);
		try {
			group.sendMessage("Hello group!");
			group.sendMessage("Knock knock");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
	}
	

	public static void main(String[] args) {

		Client c = new Client();
		/*try {		
			INameServer ns = (INameServer) Naming.lookup(bindLocation);
			ns.registerNode("google", "www.google.com");
			System.out.println(ns.lookupNode("localhost"));
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	private void joinMulticastGroup(String address, int port){
		group = new MulticastGroup(address, port);
		group.addPacketListener(this);
		new Thread(group).start();

	}

	@Override
	public void packetReceived(InetAddress sender, String message) {
		// TODO Auto-generated method stub
		System.out.println("Received multicast from " + sender + ": " + message);
	}


}
