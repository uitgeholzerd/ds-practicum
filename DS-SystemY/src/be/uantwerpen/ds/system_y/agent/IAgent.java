package be.uantwerpen.ds.system_y.agent;

import be.uantwerpen.ds.system_y.client.Client;

public interface IAgent extends Runnable {
	
	
	/**
	 * Set the client for the agent to work with
	 * @param client The client object
	 * @return	Boolean if the agent should be sent to the next client
	 */
	boolean setCurrentClient(Client client);
	
	/**
	 * Executes the actions needed before the agent can be sent to the next node
	 */
	void prepareToSend();

}
