package be.uantwerpen.ds.system_y.server;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface INameServer extends Remote {

	/**
	 * Get the location of a node by its hash
	 * 
	 * @param hash The hash of the node
	 * @return The location (IP) at which the node can be found if it exists, else returns null
	 * @throws RemoteException
	 */
	InetAddress lookupNode(int hash) throws RemoteException;

	/**
	 * Get the location of a node by its hash
	 * 
	 * @param name The name of the node
	 * @return The location (IP) at which the node can be found if it exists, else returns null
	 * @throws RemoteException
	 */
	InetAddress lookupNodeByName(String name) throws RemoteException;
	
	/**
	 * Looks up the hash of the node by its adress
	 * 
	 * @param address	address of the node
	 * @return	hash of the node
	 * @throws RemoteException
	 */
	int reverseLookupNode (String address) throws RemoteException;

	/**
	 * Removes the node from the name server's map
	 * 
	 * @param name The name of the node
	 * @return True if the node was removed, false if the node doesn't exist
	 */
	boolean unregisterNode(int nodeHash) throws RemoteException;

	/**
	 * Calculates the location of the file
	 * 
	 * @param filename The name of the file
	 * @return The location (IP) at which the file can be found if it exists, else returns null
	 * @throws RemoteException
	 */
	InetAddress getFilelocation(String filename) throws RemoteException;

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
	 * @return An array containing the names of previous and the next nodes. Returns null if the node was not found
	 */
	InetAddress[] lookupNeighbours(int nodeHash) throws RemoteException;
	
	
	/**
	 * Checks if the given node should be the file owner
	 * 
	 * @param nodeName	Name of the node
	 * @param nodeLocation	Location of the node
	 * @param fileName	Name of the file
	 * 
	 * @return	Returns true if the node should be the owner, else false
	 * 
	 * @throws RemoteException
	 */
	boolean isFileOwner(int nodeHash, InetAddress nodeLocation, String fileName) throws RemoteException;
}
