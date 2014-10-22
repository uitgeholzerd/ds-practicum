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

		// read commands from stdio
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input = null;
		while (true) {
			try {
				System.out.print(">");
				input = br.readLine();
			} catch (IOException ioe) {
				System.err.println("IO error trying to read command: "
						+ ioe.getMessage());
				System.exit(1);
			}
			String[] cmd = input.split(" ");
			String command = cmd[0];
			if (command == null) {
				break;
			} else if (command.equals("leave")) {
				client.disconnect();
			} else if (command.equals("join")) {
				client.connect();
			} else if (command.equals("nodes")) {
				System.out.println(client.getNodes());
			} else if (command.equals("ping")) {
				try {
					if (cmd[1] == null ) {
						System.err.println("");
					}
					client.ping(cmd[1]);
				} catch (IOException e) {
					System.err
							.println("Failed to send ping: " + e.getMessage());
				}
			} else if (command.equals("lookup")) {
				System.out.println(client.lookupNode(cmd[0]));
			} else if (command.equals("id")) {
				System.out.println(client.getName()+" ["+client.getAddress()+"]");
			} else if (command.equals("pingall")) {
				try {
					client.pingGroup();
				} catch (IOException e) {
					System.err.println("Failed to send multicast ping: "
							+ e.getMessage());
				}
			} else if (command.equals("quit")) {
				client.disconnect();
				System.exit(0);
			} else if (command.equals("fail")) {
				System.exit(1);
			} else {
				System.err.println("Unknown command.");
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.err.println("Thread interrupted: " + e.getMessage());
			}
		}

	}

}
