package be.uantwerpen.ds.ns.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import be.uantwerpen.ds.ns.ConnectionFailureHandler;
import be.uantwerpen.ds.ns.DatagramHandler;
import be.uantwerpen.ds.ns.INameServer;
import be.uantwerpen.ds.ns.MulticastHandler;
import be.uantwerpen.ds.ns.PacketListener;
import be.uantwerpen.ds.ns.Protocol;

public class Client implements PacketListener {

	public static final int udpClientPort = 3456;

	private MulticastHandler group;
	private DatagramHandler udp;
	private INameServer nameServer;
	private String name;

	private int hash;
	private int previousNodeHash;
	private int nextNodeHash;

	private InetAddress serverAddress;
	private Timer replyTimer;
	private ArrayList<String> receivedPings;

	public Client() {
		replyTimer = new Timer();
		connectToNetwork();
		System.out.println("Client started on " + getAddress());
	}

	/**
	 * Joins the multicast group, sends a discovery message and starts listening
	 * for replies.
	 */
	public void connectToNetwork() {
		// set up UDP socket and receive messages
		udp = new DatagramHandler(udpClientPort, this);
		//join multicast group
		group = new MulticastHandler(this);
		try {
			name = getAddress().getHostName();
			group.sendMessage(Protocol.DISCOVER,
					name + " " + getAddress().getHostAddress());
			//DISCOVER_ACK reply should set nameServer, if this doesn't happen the connection failed
			replyTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (nameServer == null)
						System.err
								.println("Connect to RMI server failed: Connection timed out.");
				}
			}, 3 * 1000);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			String ex = e1.toString();
			cfh = new ConnectionFailureHandler(name, hash, ex, udpClientPort);
		}


	}

	/**
	 * This method is triggered when a package is sent to this client (uni- or
	 * multicast) Depending on the command contained in the message, the client
	 * will perform different actions
	 * 
	 * @param address
	 *            IP of the sender
	 * @param port
	 *            Data containing the command and a message
	 */
	@Override
	public void packetReceived(InetAddress sender, String data) {
		// TODO Should we add a field to determine if UDP or multicast?
		System.out.println("Received message from " + sender + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		switch (command) {
		case DISCOVER:
			// The client received a discover-message from a new host and will
			// recalculate its neighbours if needed
			if (nameServer == null ) return;
			try {
				int newNodeHash = nameServer
						.getShortHash(message[1]);
				if (hash < newNodeHash && newNodeHash < nextNodeHash) {
					udp.sendMessage(sender, udpClientPort, Protocol.SET_NODES,
							hash + " " + nextNodeHash);
					nextNodeHash = newNodeHash;
				} else if (previousNodeHash < newNodeHash && newNodeHash < hash) {
					previousNodeHash = newNodeHash;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				String ex = e1.toString();
				cfh = new ConnectionFailureHandler(name, hash, ex, udpClientPort);
			}
			break;

		case DISCOVER_ACK:
			// Server confirmed registration and answers with its location and
			// the number of nodes
			try {
				serverAddress = sender;
				// Try to bind the NameServer
				nameServer = (INameServer) Naming.lookup(message[1]);
				System.out.println("NameServer bound to " + message[1]);
			} catch (MalformedURLException | RemoteException
					| NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// If this is the only client in the system, it is its own
			// neighbours. Else wait for answer from neighbour (= do nothing)
			if (Integer.parseInt(message[2]) == 1) {
				// this is the only node
				nextNodeHash = hash;
				previousNodeHash = hash;
			}
			break;

		case SET_NODES:
			// Another client received the discover message and will provide
			// this client with its neighbours
			previousNodeHash = Integer.parseInt(message[1]);
			nextNodeHash = Integer.parseInt(message[2]);
			break;

		case PREVNODE:
			// Another client received the fail message and will provide this
			// client with its previous node
			previousNodeHash = Integer.parseInt(message[1]);
			break;

		case NEXTNODE:
			// Another client received the fail message and will provide this
			// client with its next node
			nextNodeHash = Integer.parseInt(message[1]);
			break;

		case PING:
			// Respond to other client's ping
			if (udp != null){
				try {
					udp.sendMessage(sender, udpClientPort, Protocol.PING_ACK, message[1]);
				} catch (IOException e) {
					System.err.println("Failed to respond to ping from " + sender.getAddress() + ": " +e.getMessage());
				}
			}
			break;
		case PING_ACK:
			receivedPings.add(message[1]);
			break;
		default:
			System.err.println("Command not found");
			break;
		}

	}

	@Override
	public InetAddress getAddress() {
		InetAddress address = null;
		try {
			Socket s = new Socket("8.8.8.8", 53);
			address = s.getLocalAddress();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return address;
	}

	/**
	 * This method will be called when the client will exit the group and shut
	 * itself down
	 * @throws IOException 
	 */

	public void shutdown() {
		try {
			warnPrevNode(previousNodeHash, nextNodeHash);
			warnNextNode(nextNodeHash, previousNodeHash);
			warnNSExitNode(hash);
		} catch (IOException e) {
			System.err.println("Shutdown failed: " + e.getMessage());
			e.printStackTrace();
		} finally {

		udp.closeClient();
		group.closeClient();

		udp = null;
		group = null;
		}

	}

	/**
	 * This method will be called to inform the server about its exit
	 *
	 * @param id
	 *            client hash
	 * @throws IOException
	 */
	public void warnNSExitNode(int idExitNode) throws IOException {

		if (udp == null) {
			System.err.println("Can't warn server if not connected!");
			return;
		}
		udp.sendMessage(serverAddress, udpClientPort, Protocol.LEAVE, ""
				+ idExitNode);

	}

	/**
	 * This method will be called to change the next node of the previous client
	 *
	 * @param prevNode
	 *            previous client hash
	 * @param nextNode
	 *            next client hash
	 * @throws IOException
	 */


	public void warnPrevNode(int idPrevNode, int idNextNode) throws IOException {
		if (nameServer == null) {
			System.err.println("Not connected to RMI server");
			return;
		}
		String ip="";
		try {
			ip = nameServer.lookupNode(name);
		} catch (RemoteException e) {
			System.err.println("RMI problem while looking up name: " + e.getMessage());
			return;
		}
		if (ip.isEmpty()){
			System.err.println("Host " + name + " not found by server");
			return;
		}
		InetAddress host = InetAddress.getByName(ip);

		if (udp == null) {
			System.err.println("Can't warn previous node if not connected!");
			return;
		}
		udp.sendMessage(host, udpClientPort, Protocol.NEXTNODE, "" + idNextNode);

	}

	/**
	 * This method will be called to change the previous node of the next client
	 *
	 * @param nextNode
	 *            next client hash
	 * @param prevNode
	 *            previous client hash
	 * @throws IOException
	 */

	public void warnNextNode(int idNextNode, int idPrevNode) throws IOException {
		if (nameServer == null) {
			System.err.println("Not connected to RMI server");
			return;
		}
		String ip="";
		try {
			ip = nameServer.lookupNode(name);
		} catch (RemoteException e) {
			System.err.println("RMI problem while looking up name: " + e.getMessage());
			return;
		}
		if (ip.isEmpty()){
			System.err.println("Host " + name + " not found by server");
			return;
		}
		InetAddress host = InetAddress.getByName(ip);

		if (udp == null) {
			System.err.println("Can't warn next node if not connected!");
			return;
		}
		udp.sendMessage(host, udpClientPort, Protocol.PREVNODE, "" + idPrevNode);

	}

	/**
	 * Sends a PING message to another client
	 * 
	 * @param name
	 *            The host to ping
	 * @throws IOException
	 */
	public void ping(final String name) throws IOException {
		if (nameServer == null)
			throw new IOException("Not connected to RMI server");
		String ip = nameServer.lookupNode(name);
		if (ip.isEmpty())
			throw new IOException("Host " + name + " not found by server");
		InetAddress host = InetAddress.getByName(ip);
		if (udp == null) {
			System.err.println("Can't ping if not connected!");
			return;
		}

		final String uuid = UUID.randomUUID().toString();
		udp.sendMessage(host, udpClientPort, Protocol.PING, uuid);
		replyTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (receivedPings.remove(uuid)){
					System.out.println("Ping reply from " + name);
				} else {
					System.err
							.println("Ping timeout from " + name);
				}
			}
		}, 3 * 1000);
	}

	/**
	 * Sends a PING message to the multicast group
	 * 
	 * @throws IOException
	 */
	public void pingGroup() throws IOException {
		if (group == null) {
			System.err.println("Can't ping if not connected!");
			return;
		}
		group.sendMessage(Protocol.PING, "");
	}
	
	public String lookupNode(String name){
		String result = "";
		try {
			result = nameServer.lookupNode(name);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
