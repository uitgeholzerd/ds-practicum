package be.uantwerpen.ds.ns;

import java.net.InetAddress;
import java.rmi.*;

public interface INameServer extends Remote {
	public void register(String name, InetAddress address) throws RemoteException;
	public InetAddress lookup(String name) throws RemoteException;

}
