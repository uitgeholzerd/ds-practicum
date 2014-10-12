package be.uantwerpen.ds.ns.server;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;

public class Server {

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
