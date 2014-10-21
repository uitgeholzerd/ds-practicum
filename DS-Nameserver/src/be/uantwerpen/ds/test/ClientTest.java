package be.uantwerpen.ds.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import be.uantwerpen.ds.ns.client.Client;

public class ClientTest {

	private static Client client;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		client = new Client();
		
		//read commands from stdin
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String command = null;
		while (true) {
			try {
				System.out.print(">");
				command = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}
			// Test leave
			if (command.equals("leave")){
				client.Shutdown();
			}
		}
		
		
	}

}
