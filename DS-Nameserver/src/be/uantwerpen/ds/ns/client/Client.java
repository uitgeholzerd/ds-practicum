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
	private static final String bindLocation = "//localhost/NameServer";

	private TreeMap<Integer, InetAddress> fileMap;
	private MulticastGroup group;
	private DatagramHandler udp;
	private INameServer ns;
	private String myName;
	private int myHash;
	private int prevNode;
	private int nextNode;

	public Client() {
		fileMap = new TreeMap<Integer, InetAddress>();
		joinMulticastGroup(multicastAddress, multicastPort);
		try {
			rmiBind();
			myName = InetAddress.getLocalHost().toString().split(" ")[0];
			// we need this to compare newly registered nodes to
			myHash = ns.getShortHash(myName);
			group.sendMessage("REGISTER " + InetAddress.getLocalHost());

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// set up UDP socket and receive messages
		udp = new DatagramHandler(udpClientPort);
		udp.addPacketListener(this);
		new Thread(udp).start();
	}

	public static void main(String[] args) {
		Client c = new Client();

	}

	/**
	 * Tries to bind to the remote NameServer
	 * 
	 * @throws NotBoundException
	 * @throws RemoteException
	 * @throws MalformedURLException
	 */
	private void rmiBind() throws MalformedURLException, RemoteException,
			NotBoundException {
		ns = (INameServer) Naming.lookup(bindLocation);
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
	public void packetReceived(InetAddress sender, String message) {
		// TODO Sould we add a field to determine if UDP or multicast?
		System.out.println("Received message from " + sender + ": " + message);
		String[] command = message.split(" ");
		if (command[0].equals(Protocol.REGISTER.getCommand())) {
			// a new node has registered
			try {
				// check if it's not our own multicast
				if (!command[1].equals(InetAddress.getLocalHost())) {
					// calculate the new node's hash
					int newHash = ns.getShortHash(command[1].split("/")[0]);
					// TODO: check if we are its next/previous node and respond
					// with PREV/NEXTNODE if necessary
					// I can't wrap my head around 5a) and b) in the requirements, shouldn't it be the other way around?
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// how can you not know localhost ughh
				e.printStackTrace();
			}
		} else if (command[0].equals(Protocol.REG_ACK.getCommand())) {
			// server confirmed registration and returned number of nodes
			if (Integer.parseInt(command[1]) == 1) {
				// this is the only node
				nextNode = myHash;
				prevNode = myHash;
			} else {
				// there are other nodes
				// TODO need to wait for PREVNODE/NEXNODE command
			}
		} else if (command[0].equals(Protocol.NEXTNODE.getCommand())) {
			// other node has determined it's our next node
			nextNode = Integer.parseInt(command[1]);
		} else if (command[0].equals(Protocol.PREVNODE.getCommand())) {
			// other node has determined it's our previous node
			prevNode = Integer.parseInt(command[1]);
		}

	}

}
