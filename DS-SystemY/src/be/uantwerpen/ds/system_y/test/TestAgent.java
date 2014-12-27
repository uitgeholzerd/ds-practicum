package be.uantwerpen.ds.system_y.test;

import be.uantwerpen.ds.system_y.agent.IAgent;
import be.uantwerpen.ds.system_y.client.Client;

public class TestAgent implements IAgent {
	
	private static final long serialVersionUID = 4834357296019854379L;
	Client client;

	@Override
	/**
	 * Run the agent and check when arrived at a client. 
	 */
	public void run() {
		System.out.println("TestAgent aangekomen in client: " + client.getName() + " - " + client.getHash());
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	/**
	 * Set the agent on current client
	 */
	public boolean setCurrentClient(Client client) {
		this.client = client;
		return true;
	}

	@Override
	/**
	 * Make ready to send from client
	 */
	public void prepareToSend() {
		this.client = null;
		
	}

}