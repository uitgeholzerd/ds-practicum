package be.uantwerpen.ds.system_y.agent;

import be.uantwerpen.ds.system_y.client.Client;

public interface IAgent extends Runnable {
	
	boolean setCurrentClient(Client client);
	
	void prepareToSend();

}
