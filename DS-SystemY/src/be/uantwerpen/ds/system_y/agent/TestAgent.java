package be.uantwerpen.ds.system_y.agent;

import be.uantwerpen.ds.system_y.client.Client;

public class TestAgent implements IAgent {
	
	Client client;

	@Override
	public void run() {
		System.out.println("Aangekomen in client: " + client.getName() + " - " + client.getHash());
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean setCurrentClient(Client client) {
		this.client = client;
		return true;
	}

	@Override
	public void prepareToSend() {
		this.client = null;
		
	}

}