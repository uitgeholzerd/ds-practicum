package be.uantwerpen.ds.system_y.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import be.uantwerpen.ds.system_y.FileRecord;
import be.uantwerpen.ds.system_y.connection.DatagramHandler;
import be.uantwerpen.ds.system_y.connection.FileReceiver;
import be.uantwerpen.ds.system_y.connection.MessageHandler;
import be.uantwerpen.ds.system_y.connection.MulticastHandler;
import be.uantwerpen.ds.system_y.connection.PacketListener;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.connection.TCPHandler;
import be.uantwerpen.ds.system_y.server.INameServer;

public class Client implements PacketListener, FileReceiver {

	public static final int UDP_CLIENT_PORT = 3456;
	public static final int TCP_CLIENT_PORT = 4567;
	private static final String LOCAL_FILE_PATH = "files/";
	private static final String OWNED_FILE_PATH = "files/owned";

	private MulticastHandler group;
	private DatagramHandler udp;
	private TCPHandler tcp;
	private INameServer nameServer;
	private MessageHandler messageHandler;
	private Timer timer;

	private String name;
	private int hash;
	private int previousNodeHash;
	private int nextNodeHash;
	private ArrayList<String> localFiles;
	private ArrayList<FileRecord> ownedFiles;
	private ArrayList<String> receivedPings;
	private Path filedir;

	public Client() {
		timer = new Timer();
		ownedFiles = new ArrayList<FileRecord>();
		receivedPings = new ArrayList<String>();
		localFiles = new ArrayList<String>();
		connect();
		messageHandler = new MessageHandler(this, nameServer, udp,hash, nextNodeHash, previousNodeHash);
		System.out.println("Client started on " + getAddress().getHostName());
		filedir = Paths.get(LOCAL_FILE_PATH);
		if (!Files.exists(filedir)) {
			try {
				Files.createDirectories(filedir);
				System.out.println("Created directory for files: " + filedir.toAbsolutePath());
			} catch (IOException e) {
				System.err.println("Failed to create directory " + filedir.toAbsolutePath() + ": " + e.getMessage());
			}

		} else {
			System.out.println("Storing files in " + filedir.toAbsolutePath());
		}
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	/**
	 * Joins the multicast group, sends a discovery message and starts listening for replies.
	 */
	public void connect() {
		try {
			// set up UDP socket and receive messages
			System.out.println("Connecting to network...");
			udp = new DatagramHandler(UDP_CLIENT_PORT, this);
			// join multicast group
			group = new MulticastHandler(this);
			setName(getAddress().getHostName());
			group.sendMessage(Protocol.DISCOVER, getName() + " " + getAddress().getHostAddress());

			// If the namesever isn't set after a certain period, assume the connection has failed
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (nameServer == null) {
						System.err.println("Connect to RMI server failed: Connection timed out.");
					}
				}
			}, 3 * 1000);
			tcp = new TCPHandler(TCP_CLIENT_PORT, this);
			// After 4 seconds, scan for files. Repeat this task every 60 seconds
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					scanFiles(LOCAL_FILE_PATH);
				}
			}, 4 * 1000, 60 * 1000);
		} catch (IOException e) {
			System.err.println("Connect failed: " + e.getMessage());
			udp.closeClient();
			group.closeClient();
			udp = null;
			group = null;
			nameServer = null;
			e.printStackTrace();
		}
	}

	public void scanFiles(String path) {
		File[] files = Paths.get(path).toFile().listFiles();
		// If the path is not a directory, then listFiles() returns null.
		for (File file : files) {
			if (file.isFile()) {
				boolean newFile = localFiles.add(file.getName());
				if (newFile) {
					newFileFound(file);
				}
			}
		}
	}

	/**
	 * This method is used to disconnect the node. It will update its neighbour node, inform the nameserver and close all connections
	 * 
	 * @throws IOException
	 */
	public void disconnect() {
		try {
			// Make previous node and next node neighbours
			InetAddress prevNode = nameServer.lookupNodeByHash(previousNodeHash);
			udp.sendMessage(prevNode, UDP_CLIENT_PORT, Protocol.SET_NEXTNODE, Integer.toString(nextNodeHash));

			InetAddress nextNode = nameServer.lookupNodeByHash(nextNodeHash);
			udp.sendMessage(nextNode, UDP_CLIENT_PORT, Protocol.SET_PREVNODE, Integer.toString(previousNodeHash));

			// Unregister the node on the nameserver
			nameServer.unregisterNode(getName());

		} catch (IOException e) {
			System.err.println("Disconnect failed: " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Close connections
			udp.closeClient();
			group.closeClient();
			System.out.println("Disconnected from network");
			udp = null;
			group = null;
			nameServer = null;
		}
	}

	/**
	 * This method is triggered when a package is sent to this client (uni- or multicast) Depending on the command contained in the message, the client will perform
	 * different actions
	 * 
	 * @param address IP of the sender
	 * @param port Data containing the command and a message
	 */
	@Override
	public void packetReceived(InetAddress sender, String data) {
		System.out.println("Received message from " + sender.getHostAddress() + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		switch (command) {
		case DISCOVER:
			messageHandler.processDISCOVER(sender, message);
			break;

		case DISCOVER_ACK:
			messageHandler.processDISCOVER_ACK(sender, message);
			break;

		case SET_NODES:
			// Another client received the discover message and provides this client with its neighbours
			previousNodeHash = Integer.parseInt(message[1]);
			nextNodeHash = Integer.parseInt(message[2]);
			break;

		case SET_PREVNODE:
			// Another client encountered a failed node and provides this client with its new previous node
			previousNodeHash = Integer.parseInt(message[1]);
			break;

		case SET_NEXTNODE:
			// Another client received the fail message and will provide this client with its next node
			nextNodeHash = Integer.parseInt(message[1]);
			break;

		case PING:
			messageHandler.processPING(sender, message);
			break;
		case PING_ACK:
			receivedPings.add(message[1]);
			break;
		default:
			System.err.println("Command not found: " + message[0]);
			break;
		}

	}

	/**
	 * Remediates a failed node, updates its neighbours and the server.
	 * 
	 * @param nodeName Name of the failed node
	 */
	private void removeFailedNode(String nodeName) {
		try {
			InetAddress[] neighbours = nameServer.lookupNeighbours(nodeName);
			InetAddress prevNodeAddress = neighbours[0];
			InetAddress nextNodeAddress = neighbours[1];

			// Send the previous node of the failed node to the next node of the failed note and vice versa
			udp.sendMessage(prevNodeAddress, Client.UDP_CLIENT_PORT, Protocol.SET_NEXTNODE, "" + nameServer.getShortHash(neighbours[1]));
			udp.sendMessage(nextNodeAddress, Client.UDP_CLIENT_PORT, Protocol.SET_PREVNODE, "" + nameServer.getShortHash(neighbours[0]));

			nameServer.unregisterNode(nodeName);
		} catch (IOException e) {
			System.err.println("Failed to remediate failed node " + nodeName + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendFile(String client, String fileName) {
		try {
			InetAddress host = nameServer.lookupNode(client);
			File file = Paths.get(LOCAL_FILE_PATH + "/" + fileName).toFile();
			int fileHash = nameServer.getShortHash(fileName);
			tcp.sendFile(host, file, fileHash);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void fileReceived(String fileName) {
		InetAddress fileOwner;
		try {
			fileOwner = nameServer.getFilelocation(fileName);
			int fileHash = nameServer.getShortHash(fileName);
			// Check if this node is the owner of the file
			if (this.getAddress().equals(fileOwner)) {
				FileRecord record = new FileRecord(fileName, fileHash);
				ownedFiles.add(record);
			} else {
				localFiles.add(fileName);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Sends a PING message to another client
	 * 
	 * @param nodeName The host to ping
	 * @throws IOException
	 */
	public void pingNode(final String nodeName) throws IOException {
		if (nameServer == null)
			throw new IOException("Not connected to RMI server");
		InetAddress host = nameServer.lookupNode(nodeName);
		if (udp == null) {
			System.err.println("Can't ping if not connected!");
			return;
		}

		final String uuid = UUID.randomUUID().toString();
		udp.sendMessage(host, UDP_CLIENT_PORT, Protocol.PING, uuid);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (receivedPings.remove(uuid)) {
					System.out.println("Ping reply from " + nodeName);
				} else {
					System.err.println("Ping timeout from " + nodeName);
					removeFailedNode(nodeName);
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
		final String uuid = UUID.randomUUID().toString();
		group.sendMessage(Protocol.PING, uuid);
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

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public String lookupNode(String name) {
		InetAddress result = null;
		try {
			result = nameServer.lookupNode(name);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result.getHostAddress();
	}

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public String getNodes() {
		return "Previous: " + previousNodeHash + "\nLocal: " + hash + "\nNext: " + nextNodeHash;
	}

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public void sendFileTest(String client, String fileName) {
		try {
			InetAddress host = nameServer.lookupNode(client);
			tcp.sendFile(host, new File(filedir.toFile(), fileName), nameServer.getShortHash(fileName));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// TODO
	public void newFileFound(File file) {
		InetAddress[] nodes;
		InetAddress fileLocation = null;
		int newFileLocation;
		FileRecord fr;

		try {
			fileLocation = nameServer.getFilelocation(file.getName());
			newFileLocation = nameServer.getShortHash(fileLocation);
			if (newFileLocation == hash) {
				fileLocation = nameServer.lookupNodeByHash(previousNodeHash);
				fr = new FileRecord(file.getName(), newFileLocation);
				ownedFiles.add(fr);
				fr.addNode(fileLocation);
			}
			tcp.sendFile(fileLocation, file, newFileLocation);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// TODO
	public void moveFilesToPrev() {
		InetAddress fileLocation = null;
		int newFileLocation;
		FileRecord fr;
		String fileName = "";

		try {
			for (int i = 0; i < localFiles.size(); i++) {
				fileName = localFiles.get(i);
				fileLocation = nameServer.lookupNodeByHash(previousNodeHash);
				newFileLocation = nameServer.getShortHash(fileLocation);
				File file = Paths.get(LOCAL_FILE_PATH + "/" + fileName).toFile();
				// ???
				//tcp.sendFile(newFileLocation, file, fileLocation);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
