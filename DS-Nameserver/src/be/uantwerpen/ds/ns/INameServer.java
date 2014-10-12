package be.uantwerpen.ds.ns;

import java.net.InetAddress;
import java.rmi.*;

public interface INameServer extends Remote {
	public void registerServer(String name, InetAddress address) throws RemoteException;
	public InetAddress lookupServer(String name) throws RemoteException;

}
