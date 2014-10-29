package be.uantwerpen.ds.system_y.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import be.uantwerpen.ds.system_y.client.Client;

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
				Thread.sleep(500);
				System.out.print(">");
				input = br.readLine();
			} catch (IOException ioe) {
				System.err.println("IO error trying to read command: " + ioe.getMessage());
				System.exit(1);
			} catch (InterruptedException e) {
				System.err.println("Thread interrupted: " + e.getMessage());
			}
			String[] cmd = input.split(" ");
			String command = cmd[0];
			if (command == null || command.isEmpty()) {
				continue;
			} else if (command.equals("leave")) {
				client.disconnect();
			} else if (command.equals("join")) {
				client.connect();
			} else if (command.equals("nodes")) {
				System.out.println(client.getNodes());
			} else if (command.equals("ping")) {
				try {
					if (cmd[1] == null) {
						System.err.println("");
					}
					client.ping(cmd[1]);
				} catch (IOException e) {
					System.err.println("Failed to send ping: " + e.getMessage());
				}
			} else if (command.equals("lookup")) {
				System.out.println(client.lookupNode(cmd[0]));
			} else if (command.equals("id")) {
				System.out.println(client.getName() + " [" + client.getAddress() + "]");
			} else if (command.equals("pingall")) {
				try {
					client.pingGroup();
				} catch (IOException e) {
					System.err.println("Failed to send multicast ping: " + e.getMessage());
				}
			} else if (command.equals("quit")) {
				client.disconnect();
				System.out.println("Bye.");
				System.exit(0);
			} else if (command.equals("fail")) {
				System.out.println("Ooops.");
				System.exit(1);
			} else {
				System.err.println("Say what?!");
			}
		}

	}

}