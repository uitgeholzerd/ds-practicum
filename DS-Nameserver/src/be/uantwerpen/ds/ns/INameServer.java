package be.uantwerpen.ds.ns;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface INameServer extends Remote {
	boolean registerNode(String name, String address) throws RemoteException;
	String lookupNode(String name) throws RemoteException;
	String getFilelocation(String filename) throws RemoteException;
	int getShortHash(Object o) throws RemoteException;
}
