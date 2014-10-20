package be.uantwerpen.ds.test;

import be.uantwerpen.ds.ns.client.Client;

public class ClientTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Client node1 = new Client();
		
		//Test leave
		node1.Shutdown(2345);
	}

}
