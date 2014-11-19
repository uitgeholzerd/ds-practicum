package be.uantwerpen.ds.system_y.client;

import java.rmi.Remote;

import be.uantwerpen.ds.system_y.agents.IAgent;

public interface IClient extends Remote {

	public void receiveAgent(IAgent agent);
}
