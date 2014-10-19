package be.uantwerpen.ds.ns.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.TreeMap;

import be.uantwerpen.ds.ns.DatagramHandler;
import be.uantwerpen.ds.ns.INameServer;
import be.uantwerpen.ds.ns.MulticastGroup;
import be.uantwerpen.ds.ns.PacketListener;
import be.uantwerpen.ds.ns.Protocol;

public class Client implements PacketListener {

	private static final int udpClientPort = 3456;
	private static final int udpServerPort = 2345;
	private static final String multicastAddress = "225.6.7.8";
	private static final int multicastPort = 5678;

	private TreeMap<Integer, InetAddress> fileMap;
	private MulticastGroup group;
	private DatagramHandler udp;
	private INameServer nameServer;
	private String name;
	private int hash;
	private int previousNode;
	private int nextNode;

	public Client() {
		fileMap = new TreeMap<Integer, InetAddress>();
		joinMulticastGroup(multicastAddress, multicastPort);
		try {
			name = InetAddress.getLocalHost().getHostName();
			group.sendMessage(Protocol.DISCOVER + " " + name + " " + InetAddress.getLocalHost());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// set up UDP socket and receive messages
		udp = new DatagramHandler(udpClientPort);
		udp.addPacketListener(this);
		new Thread(udp).start();
	}

	/**
	 * Joins a multicast group and starts a thread to listen for incoming
	 * messages
	 * 
	 * @param address
	 *            IP of the multicast group
	 * @param port
	 *            Listen port and destination for sent messages
	 */
	private void joinMulticastGroup(String address, int port) {
		group = new MulticastGroup(address, port);
		group.addPacketListener(this);
		new Thread(group).start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.uantwerpen.ds.ns.PacketListener#packetReceived(java.net.InetAddress,
	 * java.lang.String)
	 */
	@Override
	public void packetReceived(InetAddress sender, String data) {
		// TODO Should we add a field to determine if UDP or multicast?
		System.out.println("Received message from " + sender + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		switch (command) {
		case DISCOVER:
			try {
				int newNodeHash = nameServer.getShortHash(message[1].split("/")[0]);
				// TODO: check if we are its next/previous node and respond
				// with PREV/NEXTNODE if necessary
				// I can't wrap my head around 5a) and b) in the requirements, shouldn't it be the other way around?
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		case DISCOVER_ACK:
			// Try to bind the NameServer
			try {
				nameServer = (INameServer) Naming.lookup(message[1]);
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// server confirmed registration and returned number of nodes
			if (Integer.parseInt(message[2]) == 1) {
				// this is the only node
				nextNode = hash;
				previousNode = hash;
			} else {
				// there are other nodes
				// TODO need to wait for PREVNODE/NEXNODE command
			}

			break;
		case NEXTNODE:
			// other node has determined it's our next node
			nextNode = Integer.parseInt(message[1]);
			break;
		case PREVNODE:
			// other node has determined it's our previous node
			previousNode = Integer.parseInt(message[1]);
			break;

		default:
			System.err.println("Command not found");
			break;
		}

	}

}
