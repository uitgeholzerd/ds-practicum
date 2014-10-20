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
	 * Removes the node from the name server's map
	 * 
	 * @param name	The name of the node
	 * @return	True if the node was removed, false if the node doesn't exist
	 */
	boolean unregisterNode(String name) throws RemoteException;
	
	/**
	 * @param	filename The name of the file
	 * @return The location (IP) at which the file can be found if it exists, else returns null
	 * @throws RemoteException
	 */
	String getFilelocation(String filename) throws RemoteException;
	
	/**
	 * Return a hash between 0 and 32768 of the object
	 * 
	 * @param o The object that should be hashed
	 * @return The hash of the object
	 * @throws RemoteException
	 */
	int getShortHash(Object o) throws RemoteException;
	
	
	/**
	 * Searches the neighbours of the given node
	 * 
	 * @param name The name of the node of whos the neighbours are being looked up
	 * @return An array containing the names of previous and the next node
	 */
	String[] lookupNeighbours(String name) throws RemoteException;
}
