package be.uantwerpen.ds.ns;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface INameServer extends Remote {
	public boolean registerNode(String name, String address) throws RemoteException;
	public String lookupNode(String name) throws RemoteException;
	public String getFilelocation(String filename) throws RemoteException;
	public int getShortHash(Object o) throws RemoteException;
}
