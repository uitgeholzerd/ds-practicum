package be.uantwerpen.ds.ns.server;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;

import be.uantwerpen.ds.ns.MulticastGroup;
import be.uantwerpen.ds.ns.PacketListener;

public class Server {
	private static MulticastGroup group = new MulticastGroup("225.6.7.8", 5678);

	public static void main(String[] args) {

		String bindLocation = "//localhost/NameServer";
        try { 
        	NameServer names = new NameServer();
			LocateRegistry.createRegistry(1099);
			Naming.bind(bindLocation, names);
	        System.out.println("NameServer is ready at:" + bindLocation);
            System.out.println("java RMI registry created.");
        } catch (MalformedURLException | AlreadyBoundException e) {
            System.err.println("java RMI registry already exists.");
        } catch (RemoteException e) {
        	 System.err.println("RemoteException: " +e.getMessage());
		}
	}



}
