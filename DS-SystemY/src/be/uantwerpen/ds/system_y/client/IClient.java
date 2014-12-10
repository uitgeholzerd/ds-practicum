package be.uantwerpen.ds.system_y.client;


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


}
