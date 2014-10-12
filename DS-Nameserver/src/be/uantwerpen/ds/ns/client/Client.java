package be.uantwerpen.ds.ns.client;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.*;

import be.uantwerpen.ds.ns.INameServer;

public class Client {

	public static void main(String[] args) {
		try {
			
			INameServer ns = (INameServer) Naming.lookup("//localhost/NameServer");
			try {
				ns.registerServer("google", InetAddress.getByName("www.google.com"));
				System.out.println("google registered");
				ns.registerServer("localhost", InetAddress.getLocalHost());
				System.out.println("localhost registered");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(ns.lookupServer("google"));
			System.out.println(ns.lookupServer("localhost"));
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	


}
