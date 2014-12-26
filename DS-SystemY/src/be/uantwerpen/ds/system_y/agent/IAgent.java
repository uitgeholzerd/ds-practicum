package be.uantwerpen.ds.system_y.agent;

import java.io.Serializable;

import be.uantwerpen.ds.system_y.client.Client;

/**
 * Interface for all agent. This interface is necessary for the clients to send and receive the agents
 *
 */
public interface IAgent extends Runnable, Serializable  {
	
	
	/**
	 * Set the client for the agent to work with
	 * 
	 * @param client The client object
	 * @return	Boolean if the agent should be sent to the next client
	 */
	boolean setCurrentClient(Client client);
	
	/**
	 * Executes the actions needed before the agent can be sent to the next node
	 */
	void prepareToSend();

}
