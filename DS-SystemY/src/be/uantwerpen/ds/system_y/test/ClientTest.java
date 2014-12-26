package be.uantwerpen.ds.system_y.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import be.uantwerpen.ds.system_y.client.Client;

public class ClientTest {

	private static Client client;

	public static void main(String[] args) {
		try {
			client = new Client();
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}

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
				System.out.println(client.debugNodes());
			} else if (command.equals("ping")) {
				try {
					if (cmd[1] == null) {
						System.err.println("Need 1 argument to ping.");
					} else { 
						client.pingNodeName(cmd[1]);
					}
				} catch (IOException e) {
					System.err.println("Failed to send ping: " + e.getMessage());
				}
			} else if (command.equals("lookup")) {
				if (cmd[1] == null) {
					System.err.println("Need 1 argument to lookup.");
				} else { 
					System.out.println(client.debugLookup(cmd[1]));
				}
			} else if (command.equals("file")) {
				if (cmd[1] == null) {
					System.err.println("Need 1 argument to file.");
				} else { 
					System.out.println(client.debugFile(cmd[1]));
				}
			} else if (command.equals("info")) {
				System.out.println(client.debugInfo());
			} else if (command.equals("local")) {
				System.out.println(client.debugLocalFiles());
			} else if (command.equals("owned")) {
				System.out.println(client.debugOwnedFiles());
			} else if (command.equals("avail")) {
				System.out.println(client.debugAvailableFiles());
			} else if (command.equals("lock")) {
				if (cmd[1] == null) {
					System.err.println("Need 1 argument to file.");
				} else { 
					System.out.println(client.debugRequestLock(cmd[1]));
				}
			} else if (command.equals("unlock")) {
				if (cmd[1] == null) {
					System.err.println("Need 1 argument to file.");
				} else { 
					System.out.println(client.debugRequestUnlock(cmd[1]));
				}
			} else if (command.equals("locks")) {
				System.out.println(client.debugLocks());
			} else if (command.equals("send")) {
				if (cmd[2] == null) {
					System.err.println("Need 2 arguments to send.");
				} else { 
					client.debugSendFile(cmd[1], cmd[2]);
				}
			} else if (command.equals("id")) {
				System.out.println(client.getName() + " [" + client.getAddress() + "]");
			} else if (command.equals("pwd")) {
				System.out.println(Paths.get("").toAbsolutePath());
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
