package be.uantwerpen.ds.system_y.client;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import be.uantwerpen.ds.system_y.agent.IAgent;

public interface IClient extends Remote {

	/**
	 * Method for receiving agent from other clients. This method will run the agent and send it to the next client if needed.
	 * 
	 * @param agent
	 */
	void receiveAgent(IAgent agent) throws RemoteException;

	/**
	 * This method is triggered when the TCPHandler receives a file and passes it to the FileReceiver
	 * 
	 * @param fileHash Hash of the file
	 * @param fileName Name of the file
	 */
	void fileReceived(InetAddress sender, String fileName, boolean isOwner);

	boolean isFileOwner(String fileName);
}
