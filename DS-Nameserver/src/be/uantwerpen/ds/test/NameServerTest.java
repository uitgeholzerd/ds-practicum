package be.uantwerpen.ds.test;

import be.uantwerpen.ds.ns.server.NameServer;

public class NameServerTest {
	public static void main(String[] args) throws Exception {
		NameServer ns = new NameServer();
		boolean failed = false;
		boolean test = false;

		// Test toevoegen
		test = ns.registerNode("aaa123", "localhost");
		if (!test) {
			System.out.println("Fout bij het toevoegen");
			failed = true;
		}

		// Test dubbel toevoegen
		test = ns.registerNode("aaa123", "localhost");
		if (test) {
			System.out.println("Geen fout bij dubbel toevoegen");
			failed = true;
		}

		// Test ophalen
		String ip1 = ns.lookupNode("aaa123");
		if (ip1 == null) {
			System.out.println("Ip1 not found");
			failed = true;
		}

		// Test verwijderen
		test = ns.unregisterNode("aaa123");
		String ip2 = ns.lookupNode("aaa123");
		if (!test || ip2 != null) {
			System.out.println("Ip2  found");
			failed = true;
		}

		// Test dubbel verwijderen
		test = ns.unregisterNode("aaa123");
		String ip3 = ns.lookupNode("aaa123");
		if (test || ip3 != null) {
			System.out.println("Ip2  found");
			failed = true;
		}
		
		//Bestandsnaam opvragen
		String node1Name = "fileNode";
		String node1Location = "1.2.3.4";
		String node2Name = "azerty";
		String node2Location = "4.5.6.6";
		String filename = "1";
		
		int node1Hash =  Math.abs(node1Name.hashCode()) % (int) Math.pow(2, 15);
		int node2Hash =  Math.abs(node2Name.hashCode()) % (int) Math.pow(2, 15);
		int fileHash =  Math.abs(filename.hashCode()) % (int) Math.pow(2, 15);
		
		ns.registerNode(node1Name, node1Location);
		ns.registerNode(node2Name, node2Location);
		
		String location = ns.getFilelocation(filename);
		
		System.out.println("Node 1 hash: " + node1Hash + ", location: " + node1Location);
		System.out.println("Node 2 hash: " + node2Hash + ", location: " + node2Location);
		System.out.println("File hash: " + fileHash + ", location: " + location);
		
		if ((fileHash < node1Hash && !location.equals(node2Location)) || (fileHash > node1Hash && !location.equals(node1Location))) {
			System.out.println("File lookup failed");
			failed = true;
		}
		//TODO test voor gelijktijd opvragen
		
		System.out.println();
		if (failed) {
			System.out.println("One or more tests failed");
		} else {
			System.out.println("Test completed successfully");
		}

		
		//Test leave
		
	}

}
