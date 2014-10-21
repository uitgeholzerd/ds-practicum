package be.uantwerpen.ds.ns;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface INameServer extends Remote {
	
	/**
	 * @param name	The name of the node
	 * @return	The location (IP) at which the node can be found if it exists, else returns null
	 * @throws	RemoteException
	 */
	String lookupNode(String name) throws RemoteException;
	
	/**
	 * @param	filename The name of the file
	 * @return The location (IP) at which the file can be found if it exists, else returns null
	 * @throws RemoteException
	 */
	String getFilelocation(String filename) throws RemoteException;
	
	/**
	 * @param o The object that should be hashed
	 * @return The hash of the object
	 * @throws RemoteException
	 */
	int getShortHash(Object o) throws RemoteException;
	
	String[] lookupNeighbours(String name) throws RemoteException;
}
