package be.uantwerpen.ds.ns;

import java.net.InetAddress;
import java.rmi.*;

public interface INameServer extends Remote {
	public boolean registerNode(String name, InetAddress address) throws RemoteException;
	public InetAddress lookupNode(String name) throws RemoteException;
	public InetAddress getFilelocation(String filename) throws RemoteException;

}
