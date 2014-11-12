package be.uantwerpen.ds.system_y.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import be.uantwerpen.ds.system_y.connection.DatagramHandler;
import be.uantwerpen.ds.system_y.connection.FileReceiver;
import be.uantwerpen.ds.system_y.connection.MessageHandler;
import be.uantwerpen.ds.system_y.connection.MulticastHandler;
import be.uantwerpen.ds.system_y.connection.PacketListener;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.connection.TCPHandler;
import be.uantwerpen.ds.system_y.file.FileRecord;
import be.uantwerpen.ds.system_y.server.INameServer;

public class Client implements PacketListener, FileReceiver {

	public static final int UDP_CLIENT_PORT = 3456;
	public static final int TCP_CLIENT_PORT = 4567;
	public static final String LOCAL_FILE_PATH = "files/";
	public static final String OWNED_FILE_PATH = "files/owned/";

	private MulticastHandler group;
	private DatagramHandler udp;
	private TCPHandler tcp;
	private MessageHandler messageHandler;
	private INameServer nameServer;
	private Timer timer;

	private String name;
	private int hash;
	private int previousNodeHash;
	private int nextNodeHash;
	private TreeSet<String> localFiles;
	private ArrayList<FileRecord> ownedFiles;
	private TreeMap<String, Boolean> availableFiles;
	private ArrayList<String> receivedPings;
	private Path filedir;

	public Client() {
		timer = new Timer();
		ownedFiles = new ArrayList<FileRecord>();
		receivedPings = new ArrayList<String>();
		localFiles = new TreeSet<String>();
		connect();
		messageHandler = new MessageHandler(this, udp);
		System.out.println("Client started on " + getAddress().getHostName());
		createDirectory(LOCAL_FILE_PATH);
		createDirectory(OWNED_FILE_PATH);
		
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public INameServer getNameServer() {
		return nameServer;
	}

	public void setNameServer(INameServer nameServer) {
		this.nameServer = nameServer;
	}
	
	public ArrayList<FileRecord> getOwnedFiles() {
		return ownedFiles;
	}
	
	public TreeMap<String, Boolean> getAvailableFiles() {
		return availableFiles;
	}
	
	public void setAvailableFiles(TreeMap<String, Boolean> files){
		this.availableFiles = files;
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
					System.out.println("Scanning files");
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

	/**
	 * Scan a path for files and compare them to previously found files
	 * If the file was not found before, execute the newFileFound method
	 * @param path	Path to scan
	 */
	private void scanFiles(String path) {
		File[] files = Paths.get(path).toFile().listFiles();
		// If the path is not a directory, then listFiles() returns null.
		for (File file : files) {
			if (file.isFile()) {
				boolean newFile = localFiles.add(file.getName());
				if (newFile) {
					System.out.println("File found: " + file.getName());
					newFileFound(file);
				}
			}
		}
	}

	private void createDirectory(String dir) {
		filedir = Paths.get(dir);
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

	/**
	 * This method is used to disconnect the node. It will update its neighbour node, inform the nameserver and close all connections
	 * 
	 * @throws IOException
	 */
	public void disconnect() {
		try {
			// Make the previous node the new owner of the files owned by the current node
			moveFilesToPrev();
			// Warn the owner of the replicated files of the current node to update its file records
			warnFileOwner();

			// Make previous node and next node neighbours
			InetAddress prevNode = nameServer.lookupNodeByHash(getPreviousNodeHash());
			udp.sendMessage(prevNode, UDP_CLIENT_PORT, Protocol.SET_NEXTNODE, Integer.toString(getNextNodeHash()));

			InetAddress nextNode = nameServer.lookupNodeByHash(getNextNodeHash());
			udp.sendMessage(nextNode, UDP_CLIENT_PORT, Protocol.SET_PREVNODE, Integer.toString(getPreviousNodeHash()));

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
		if (sender.getHostAddress().equals(this.getAddress().getHostAddress())) {
			return;
		}
		System.out.println("Received message from " + sender.getHostAddress() + ": " + data);
		String[] message = data.split(" ");
		Protocol command = Protocol.valueOf(message[0]);
		switch (command) {
		case DISCOVER:
			int next = getNextNodeHash();
			messageHandler.processDISCOVER(sender, message);
			// If the nextNodeHash has changed, check if this new node should be owner of any of the owned files
			if (next != getNextNodeHash()) {
				recheckOwnedFiles();
			}
			break;

		case DISCOVER_ACK:
			messageHandler.processDISCOVER_ACK(sender, message);
			break;

		case SET_NODES:
			// Another client received the discover message and provides this client with its neighbours
			setPreviousNodeHash(Integer.parseInt(message[1]));
			setNextNodeHash(Integer.parseInt(message[2]));
			break;

		case SET_PREVNODE:
			// Another client encountered a failed node and provides this client with its new previous node
			setPreviousNodeHash(Integer.parseInt(message[1]));
			break;

		case SET_NEXTNODE:
			// Another client received the fail message and will provide this client with its next node
			setNextNodeHash(Integer.parseInt(message[1]));
			break;

		case PING:
			messageHandler.processPING(sender, message);
			break;
		case PING_ACK:
			receivedPings.add(message[1]);
			break;
		case UPDATE_FILERECORD:
			updateOwnedFiles(message[1], message[2]);
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

	@Override
	public void fileReceived(InetAddress sender, String fileName, boolean isOwner) {
		try {
			// If this node is the owner of the file and the file doesn't exist in the records, create a new record for it
			// and add the sender to the list of nodes where it is available
			if (isOwner) {
				boolean hasTheFile = false;
				int fileHash = nameServer.getShortHash(fileName);
				FileRecord newRecord = new FileRecord(fileName, fileHash);
				// Check if file exists in records
				for (FileRecord localRecord : ownedFiles) {
					if(newRecord==localRecord){
						hasTheFile=true;
					}
				}
				// If file already exists in records, replicate this file to previous node
				if(hasTheFile==true){
					InetAddress previousNode = nameServer.lookupNodeByHash(previousNodeHash);
					File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
					tcp.sendFile(previousNode, file, false);
				}
				// Else create record and add sender to downloadlocations
				else{
					newRecord.addNode(sender);
					ownedFiles.add(newRecord);
				}
			}
			// If node is not the owner, add the file to the replicated files
			else{
				localFiles.add(fileName);
			}
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * This method handles new files that are found on the current node by sending them to the right location and creating a fileRecord if needed
	 * 
	 * @param file The new file that has been found
	 */
	public void newFileFound(File file) {
		try {
			String fileName = file.getName();
			InetAddress fileOwner = nameServer.getFilelocation(fileName);

			// If the file file is owned by this node, create a new record for it,
			// send it to its previous neighbour and add that neighbour to the list of available nodes
			// Else send it the owner
			if (this.getAddress().equals(fileOwner)) {
				InetAddress previousNode = nameServer.lookupNodeByHash(getPreviousNodeHash());
				tcp.sendFile(previousNode, file, false);

				int fileHash = nameServer.getShortHash(fileName);
				FileRecord record = new FileRecord(fileName, fileHash);
				record.addNode(previousNode);
				ownedFiles.add(record);
				file.renameTo(new File(OWNED_FILE_PATH + fileName));
			} else {
				tcp.sendFile(fileOwner, file, true);
			}
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * This method is triggered when a new node join the system
	 * The current node checks if the new node should be the owner of any of the current node's owned files
	 * 
	 */
	public void recheckOwnedFiles() {
		InetAddress owner;
		String fileName;
		for (FileRecord record : ownedFiles) {
			try {
				fileName = record.getFileName();
				owner = nameServer.getFilelocation(fileName);
				if (!this.getAddress().equals(owner)) {
					File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
					tcp.sendFile(owner, file, true);
					ownedFiles.remove(record);
					file.delete();
				}
			} catch (RemoteException e) {
				System.err.println("Unable to contact nameServer");
				e.printStackTrace();
			}
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
			System.err.println("Host not found");
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
		return "Previous: " + getPreviousNodeHash() + "\nLocal: " + getHash() + "\nNext: " + getNextNodeHash();
	}

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public void sendFileTest(String client, String fileName) {
		try {
			InetAddress host = nameServer.lookupNode(client);
			tcp.sendFile(host, new File(filedir.toFile(), fileName), true);
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}
	
	/**
	 * This method makes sure the owners of the replicated files update their file records by sending a udp message
	 */
	private void warnFileOwner(){
		try {
			for (String fileName : localFiles) {
				// First let owner of file update file record
				InetAddress fileOwner = nameServer.getFilelocation(fileName);
				udp.sendMessage(fileOwner, Client.UDP_CLIENT_PORT, Protocol.UPDATE_FILERECORD, name + " " + fileName);
			}
		} catch (IOException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * This method moves all of its owned files to the previous node
	 */
	private void moveFilesToPrev() {
		try {
			for (FileRecord record : ownedFiles) {
				String fileName = record.getFileName();
				
				InetAddress previousNode = nameServer.lookupNodeByHash(previousNodeHash);
				File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
				tcp.sendFile(previousNode, file, true);
			}
		} catch (IOException e) {
			System.err.println("Moving local files to previous node failed");
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * This method is triggered when another node shuts down, checks if own list of download locations on
	 * the file record is empty, either removes file itself or removes address of node that is
	 * shutting down if list is not empty
	 * 
	 * @param otherNode		Address of node that is shutting down
	 */
	private void updateOwnedFiles(String disconnectingNode, String fileName){
		try {
			for(int i=0;i<ownedFiles.size();i++){
				// Search for file in owned files list
				if(ownedFiles.get(i).getFileName() == fileName){
					FileRecord record = ownedFiles.get(i);
					// Remove file from owned files list + file itself
					if(record.getNodes().isEmpty()){
						ownedFiles.remove(i);
						File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
						file.delete();
					}
					// Remove download location of file
					else{
						InetAddress dcNode = nameServer.lookupNode(disconnectingNode);
						record.removeNode(dcNode);
					}
				}
			}
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * @return the previousNodeHash
	 */
	public int getPreviousNodeHash() {
		return previousNodeHash;
	}

	/**
	 * @param previousNodeHash the previousNodeHash to set
	 */
	public void setPreviousNodeHash(int previousNodeHash) {
		this.previousNodeHash = previousNodeHash;
	}

	/**
	 * @return the nextNodeHash
	 */
	public int getNextNodeHash() {
		return nextNodeHash;
	}

	/**
	 * @param nextNodeHash the nextNodeHash to set
	 */
	public void setNextNodeHash(int nextNodeHash) {
		this.nextNodeHash = nextNodeHash;
	}

	/**
	 * @return the hash
	 */
	public int getHash() {
		return hash;
	}

	/**
	 * @param hash the hash to set
	 */
	public void setHash(int hash) {
		this.hash = hash;
	}
}
