package be.uantwerpen.ds.system_y.agents;

import be.uantwerpen.ds.system_y.client.Client;

public interface IAgent extends Runnable {
	
	public boolean setCurrentClient(Client client);

}
