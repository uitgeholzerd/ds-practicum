package be.uantwerpen.ds.system_y.client;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import be.uantwerpen.ds.system_y.agent.FailureAgent;
import be.uantwerpen.ds.system_y.agent.FileAgent;
import be.uantwerpen.ds.system_y.agent.IAgent;
import be.uantwerpen.ds.system_y.connection.DatagramHandler;
import be.uantwerpen.ds.system_y.connection.FileReceiver;
import be.uantwerpen.ds.system_y.connection.MessageHandler;
import be.uantwerpen.ds.system_y.connection.MulticastHandler;
import be.uantwerpen.ds.system_y.connection.PacketListener;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.connection.TCPHandler;
import be.uantwerpen.ds.system_y.file.FileRecord;
import be.uantwerpen.ds.system_y.gui.*;
import be.uantwerpen.ds.system_y.server.INameServer;

/**
 * Class that represents a node in the network.
 * Can communicate with the nameserver and other nodes, handles owned and replicated files and sends/receives agents
 *
 */
public class Client extends UnicastRemoteObject implements PacketListener, FileReceiver, IClient {

	private static final long serialVersionUID = 5234233628726984521L;
	public static final int UDP_CLIENT_PORT = 3456;
	public static final int TCP_CLIENT_PORT = 4567;
	public static final String LOCAL_FILE_PATH = "files/";
	public static final String OWNED_FILE_PATH = "files/owned/";
	public static final int rmiPort = 1098;
	private static final String bindLocation = "Client";

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
	private Set<String> localFiles;
	private List<FileRecord> ownedFiles;
	private HashSet<String> availableFiles;
	private TreeMap<String, Boolean> lockRequests;
	private List<String> receivedPings;
	private boolean pingSuccess;
	private Path filedir;

	//private View view;
	//private Model model;
	private Controller controller;
	private boolean isDownloading = false;
	private boolean isDeleting = false;

	public Client() throws RemoteException {
		super();
		ownedFiles = Collections.synchronizedList(new ArrayList<FileRecord>());
		receivedPings = Collections.synchronizedList(new ArrayList<String>());
		localFiles = Collections.synchronizedSet(new TreeSet<String>());
		availableFiles = new HashSet<String>();
		lockRequests = new TreeMap<String, Boolean>();
		createDirectory(LOCAL_FILE_PATH);
		createDirectory(OWNED_FILE_PATH);
		rmiBind();
		connect();
		System.out.println("Client started on " + getAddress().getHostName());
	}

	public TCPHandler getTCPHandler() {
		return tcp;
	}

	public DatagramHandler getUDPHandler() {
		return udp;
	}

	public Set<String> getLocalFiles() {
		return localFiles;
	}

	public String getName() {
		return name;
	}

	public INameServer getNameServer() {
		return nameServer;
	}

	public void setNameServer(INameServer nameServer) {
		this.nameServer = nameServer;
	}

	public int getPreviousNodeHash() {
		return previousNodeHash;
	}

	public void setPreviousNodeHash(int previousNodeHash) {
		this.previousNodeHash = previousNodeHash;
	}

	public int getNextNodeHash() {
		return nextNodeHash;
	}

	public void setNextNodeHash(int nextNodeHash) {
		this.nextNodeHash = nextNodeHash;
	}

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public List<FileRecord> getOwnedFiles() {
		return ownedFiles;
	}

	public HashSet<String> getAvailableFiles() {
		return availableFiles;
	}

	public void setAvailableFiles(HashSet<String> files) {
		this.availableFiles = files;
	}

	public TreeMap<String, Boolean> getLockRequests() {
		return lockRequests;
	}
	
	public boolean getIsDownloading(){
		return isDownloading;
	}
	
	public void setIsDownloading(boolean downloading){
		this.isDownloading = downloading;
	}
	
	public boolean getIsDeleting(){
		return isDeleting;
	}
	
	public void setIsDeleting(boolean deleting){
		this.isDeleting = deleting;
	}

	/**
	 * Joins the multicast group, sends a discovery message and starts listening for replies.
	 * Check the connections with the nameserver after a delay and start the regular scanning for new files
	 */
	public void connect() {
		try {
			// set up UDP socket and receive messages
			udp = new DatagramHandler(UDP_CLIENT_PORT, this);
			// join multicast group
			group = new MulticastHandler(this);
			this.name = getAddress().getHostName()  + randomString();
			messageHandler = new MessageHandler(this, udp, group);
			group.sendMessage(Protocol.DISCOVER, getName() + " " + getAddress().getHostAddress());
			timer = new Timer();
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

	// TODO Debug
	public String randomString() {
		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < random.nextInt(10) + 1; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		String output = sb.toString();
		return output;
	}

	/**
	 * Bind this client to the location and port on its address for RMI
	 */
	private void rmiBind() {
		try {
			LocateRegistry.createRegistry(Client.rmiPort);
			Naming.bind("//" + getAddress().getHostAddress() + ":" + Client.rmiPort + "/" + Client.bindLocation, this);
		} catch (MalformedURLException | AlreadyBoundException e) {
			System.err.println("java RMI registry already exists.");
		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e.getMessage());
		}
	}

	/**
	 * Scan a path for files and compare them to previously found files If the file was not found before, execute the newFileFound method
	 * 
	 * @param path Path to scan
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
					updateGUI();
				}
			}
		}
	}

	/**
	 * Creates a directory needed for the program if it does not exist yet
	 * 
	 * @param dir Name of the directory
	 */
	private void createDirectory(String dir) {
		filedir = Paths.get(dir);
		if (!Files.exists(filedir)) {
			try {
				Files.createDirectories(filedir);
			} catch (IOException e) {
				System.err.println("Failed to create directory " + filedir.toAbsolutePath() + ": " + e.getMessage());
			}

		}
	}

	/**
	 * This method is used to disconnect the node. It will updateGUI its neighbour node, inform the nameserver and close all connections
	 * 
	 * @throws IOException
	 */
	public void disconnect() {
		try {
			timer.cancel();
			// Make the previous node the new owner of the files owned by the current node
			moveFilesToNode(previousNodeHash);
			// Warn the owner of the replicated files of the current node to updateGUI its file records
			notifyLocationUnavailable();

			// Make previous node and next node neighbours
			InetAddress prevNode = nameServer.lookupNode(getPreviousNodeHash());
			udp.sendMessage(prevNode, UDP_CLIENT_PORT, Protocol.SET_NEXTNODE, Integer.toString(getNextNodeHash()));

			InetAddress nextNode = nameServer.lookupNode(getNextNodeHash());
			udp.sendMessage(nextNode, UDP_CLIENT_PORT, Protocol.SET_PREVNODE, Integer.toString(getPreviousNodeHash()));

			// Unregister the node on the nameserver
			nameServer.unregisterNode(this.hash);
		} catch (IOException e) {
			System.err.println("Disconnect failed: " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Close connections
			try {
				if (udp != null)
					udp.closeClient();
				if (group != null)
					group.closeClient();
				if (tcp != null)
					tcp.closeClient();
			} catch (Exception e) {
				System.err.println("Closing sockets failed: " + e.getMessage());
			}
			System.out.println("Disconnected from network");
			udp = null;
			group = null;
			tcp = null;
			nameServer = null;
		}
	}

	/**
	 * This method is triggered when a package is sent to this client (uni- or multicast).
	 * Depending on the command contained in the message, the client will perform different actions
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
		case NODE_JOINED:
			messageHandler.processNODE_JOINED(sender, message);
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

		case FILE_LOCATION_UNAVAILABLE:
			removeFileLocation(Integer.parseInt(message[1]));
			break;

		case FILE_LOCATION_AVAILABLE:
			addFileCopy(Integer.parseInt(message[1]), message[2]);
			break;

		case DOWNLOAD_REQUEST:
			File file = Paths.get(OWNED_FILE_PATH + message[1]).toFile();
			try {
				tcp.sendFile(sender, file, false);
			} catch (IOException e) {
				e.printStackTrace();

				try {
					// Remote node could not be reached and should be removed
					removeFailedNode(nameServer.reverseLookupNode(sender.getHostAddress()));
				} catch (RemoteException e1) {
					System.err.println("Error while contacting nameServer");
					e1.printStackTrace();
				}
			}
			break;
		case DELETE_LOCAL_REQUEST:
			deleteFileLocation(message[1], Integer.parseInt(message[2]));
			break;
		case DELETE_NETWORK_REQUEST:
			deleteFile(message[1]);
			break;
		case FILE_LOCATIONS_REQUEST:
			String answer = getDownloadLocations(message[2]);
			try {
				InetAddress returnAddr = nameServer.lookupNodeByName(message[1]);
				udp.sendMessage(returnAddr, UDP_CLIENT_PORT, Protocol.FILE_LOCATIONS_ACK, answer);
			} catch (IOException e) {
				System.err.println("Error while sending message");
				e.printStackTrace();
			}
			break;
		case FILE_LOCATIONS_ACK:
			TreeSet<Integer> set = new TreeSet<Integer>();
			for(int i = 1; i < message.length;i++){
					set.add(Integer.parseInt(message[i]));
			}
			deleteNetworkFile(message[1], set);
			break;
		case DISCOVER:
			// ignore
			break;
		default:
			System.err.println("Command not found: " + message[0]);
			break;
		}

	}

	/**
	 * Remediates a failed node, updates its neighbours and the server and starts the FailureAgent
	 * 
	 * @param nodeName Name of the failed node
	 */
	public void removeFailedNode(int nodeHash) {
		System.out.printf("Detected failed node with hash %d.%n", nodeHash);
		try {
			InetAddress[] neighbours = nameServer.lookupNeighbours(nodeHash);
			if (neighbours != null) {
				InetAddress prevNodeAddress = neighbours[0];
				InetAddress nextNodeAddress = neighbours[1];

				// Send the previous node of the failed node to the next node of the failed note and vice versa
				int failedNodeNextHash = nameServer.reverseLookupNode(neighbours[1].getHostAddress());
				if (nodeHash == nextNodeHash) {
					nextNodeHash = failedNodeNextHash;
				} else {
					udp.sendMessage(prevNodeAddress, Client.UDP_CLIENT_PORT, Protocol.SET_NEXTNODE, "" + failedNodeNextHash);
				}
				int failedNodePrevHash = nameServer.reverseLookupNode(neighbours[0].getHostAddress());
				if (nodeHash == previousNodeHash) {
					previousNodeHash = failedNodePrevHash;
				} else {
					udp.sendMessage(nextNodeAddress, Client.UDP_CLIENT_PORT, Protocol.SET_PREVNODE, "" + failedNodePrevHash);
				}

				// Retrieve the location of the failed node, then remove it from the nameserver
				InetAddress failedNodeLocation = nameServer.lookupNode(nodeHash);
				nameServer.unregisterNode(nodeHash);

				// Initilize and start the FailureAgent
				receiveAgent(new FailureAgent(this.hash, nodeHash, failedNodeLocation));
			}
		} catch (IOException e) {
			System.err.println("Failed to remediate failed node (hash) " + nodeHash + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void receiveFile(InetAddress sender, String fileName, boolean isOwner) {
		try {
			// If this node is the owner of the file and the file doesn't exist in the records, create a new record for it
			// and add the sender to the list of nodes where it is available
			int senderHash = nameServer.reverseLookupNode(sender.getHostAddress());
			if (isOwner) {
				boolean hasTheFile = false;
				int fileHash = nameServer.getShortHash(fileName);
				// Check if file exists in records
				for (FileRecord record : ownedFiles) {
					if (fileName.equals(record.getFileName())) {
						hasTheFile = true;
						record.addNode(senderHash);
						break;
					}
				}
				
				
				InetAddress previousNode = nameServer.lookupNode(previousNodeHash);

				// If file doesn't exists in records, create one and add sender to available locations
				if (!hasTheFile){
					FileRecord newRecord = new FileRecord(fileName, fileHash);
					newRecord.addNode(senderHash);
					ownedFiles.add(newRecord);
				}
				// Else file already exists in records, replicate this file to previous node (if that was not the sender)
				else if (!sender.equals(previousNode)) {
					File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
					try {
						tcp.sendFile(previousNode, file, false);
					} catch (IOException e) {
						// Remote node could not be reached and should be removed
						removeFailedNode(senderHash);
					}

				}
			}
			// If node is not the owner, add the file to the replicated files
			else {
				localFiles.add(fileName);
			}

			// If this node requested a lock for the file, request a release
			if (lockRequests.containsKey(fileName) && (lockRequests.get(fileName) == null)) {
				lockRequests.put(fileName, false);
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
				int fileHash = nameServer.getShortHash(fileName);
				FileRecord record = new FileRecord(fileName, fileHash);

				if (this.hash != previousNodeHash) {
					InetAddress previousNode = nameServer.lookupNode(previousNodeHash);
					try {
						tcp.sendFile(previousNode, file, false);
					} catch (IOException e) {
						e.printStackTrace();
						// Remote node could not be reached and should be removed
						removeFailedNode(previousNodeHash);
					}
					record.addNode(previousNodeHash);
					System.out.println(previousNode.getHostAddress() + " added to record " + fileName);
				}
				
				System.out.println("Record added to owned files");
				ownedFiles.add(record);
				
				// Add file to owned files directory and remove it from the local files
				file.renameTo(new File(OWNED_FILE_PATH + fileName));
				localFiles.remove(fileName);
			} else {
				try {
					tcp.sendFile(fileOwner, file, true);
				} catch (IOException e) {
					e.printStackTrace();
					// Remote node could not be reached and should be removed
					removeFailedNode(nameServer.reverseLookupNode(fileOwner.getHostAddress()));
				}
			}
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * This method is triggered when a new node join the system.
	 * The current node checks if the new node should be the owner of any of the current node's owned files
	 * 
	 */
	public void recheckOwnedFiles() {
		InetAddress owner;
		String fileName;
		
		// For each owned, files check if the current node is still the owner
		// If not, send the file to the new owner
		for (Iterator<FileRecord> iterator = ownedFiles.iterator(); iterator.hasNext();) {
			FileRecord record = (FileRecord) iterator.next();
			try {
				fileName = record.getFileName();
				owner = nameServer.getFilelocation(fileName);
				
				System.out.println("*** Rechecking file:" + fileName);
				
				File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
				if (!this.getAddress().equals(owner)) {
					System.out.println("*** New owner, sending file to :" + owner.getHostAddress());
					try {
						tcp.sendFile(owner, file, true);
						
						// Pass the previously available locations to the new owner
						for (Integer nodeHash : record.getNodeHashes()) {
							udp.sendMessage(owner, Client.UDP_CLIENT_PORT, Protocol.FILE_LOCATION_AVAILABLE, nodeHash + " " + fileName);
						}
					} catch (IOException e) {
						e.printStackTrace();
						removeFailedNode(nameServer.reverseLookupNode(owner.getHostAddress()));
					}
					
					iterator.remove();

					// Add file to local files
					file.renameTo(new File(LOCAL_FILE_PATH + fileName));
					localFiles.add(fileName);
				}
				// If this node is the file owner but the filed hasn't been cloned to it's previous neighbour yet, send it to them
				else if (record.getNodeHashes().isEmpty() && this.hash != previousNodeHash) {
					InetAddress previousNode = nameServer.lookupNode(previousNodeHash);
					System.out.println("*** List empty and diff prev node hash, copying to " + previousNode.getHostAddress());
					try {
						tcp.sendFile(previousNode, file, false);
					} catch (IOException e) {
						e.printStackTrace();
						// Remote node could not be reached and should be removed
						removeFailedNode(previousNodeHash);
					}
					record.addNode(previousNodeHash);
				}
			} catch (RemoteException e) {
				System.err.println("Unable to contact nameServer");
				e.printStackTrace();
			}
		}

	}

	@Override
	public boolean hasOwnedFile(String fileName) {
		for (Iterator<FileRecord> iterator = ownedFiles.iterator(); iterator.hasNext();) {
			FileRecord record = (FileRecord) iterator.next();
			if (record.getFileName().equalsIgnoreCase(fileName))
				return true;
		}
		return false;
	}

	/**
	 * Sends a PING message to another client
	 * 
	 * @param nodeName The host to ping
	 * @throws IOException
	 */
	public boolean pingNode(final int nodeHash) throws IOException {
		if (nameServer == null)
			throw new IOException("Not connected to RMI server");
		final InetAddress host = nameServer.lookupNode(nodeHash);
		if (udp == null) {
			System.err.println("Can't ping if not connected!");
			pingSuccess = false;
		} else {
			final String uuid = UUID.randomUUID().toString();
			udp.sendMessage(host, UDP_CLIENT_PORT, Protocol.PING, uuid);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (receivedPings.remove(uuid)) {
						System.out.println("Ping reply from " + host.getHostAddress());
						pingSuccess = true;
					} else {
						System.err.println("Ping timeout from " + host.getHostAddress());
						removeFailedNode(nodeHash);
						pingSuccess = false;
					}
				}
			}, 3 * 1000);
		}
		return pingSuccess;
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

	// TODO debugging
	public String debugLookup(String name) {
		InetAddress result = null;
		try {
			result = nameServer.lookupNodeByName(name);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return result.getHostAddress();
	}

	// TODO debugging
	public String debugNodes() {
		return "Previous: " + getPreviousNodeHash() + "\nLocal: " + getHash() + "\nNext: " + getNextNodeHash();
	}

	// TODO debugging
	public void debugSendFile(String client, String fileName) {
		try {
			InetAddress host = nameServer.lookupNodeByName(client);
			try {
				tcp.sendFile(host, new File(filedir.toFile(), fileName), true);
			} catch (IOException e) {
				e.printStackTrace();
				removeFailedNode(nameServer.reverseLookupNode(host.getHostAddress()));
			}
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	// TODO debugging
	public String debugAvailableFiles() {
		String result = "Available files:\n";
		for (String entry : getAvailableFiles()) {
			result += entry + "\n";
		}
		return result;
	}

	// TODO debugging
	public String debugLocalFiles() {
		String result = "Local files:\n";
		for (String entry : localFiles) {
			result += entry + "\n";
		}
		return result;
	}

	//TODO debugging
	public String debugOwnedFiles() {
		String result = "Owned files:\n";
		for (FileRecord entry : ownedFiles) {
			result += entry.getFileName() + ": " + entry.getNodeHashes().toString() + "\n";
		}
		return result;
	}

	//TODO debugging
	public String debugFile(String name) {
		String result = "";
		for (FileRecord entry : ownedFiles) {
			if (entry.getFileName().equalsIgnoreCase(name)) {
				result += "File record:\n";
				result += " Name:" + entry.getFileName() + "\n";
				result += " Hash:" + entry.getFileHash() + "\n";
				result += " Nodes:" + entry.getNodeHashes().toString() + "\n";
			}
		}
		return result;
	}
	
	//TODO debugging
	public void pingNodeName(String nodeName) throws IOException{
		try {
			int hash = nameServer.getShortHash(nodeName);
			pingNode(hash);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * This method informs the owners of the replicated files to mark this location as unavailable
	 */
	private void notifyLocationUnavailable() {
		try {
			for (String fileName : localFiles) {
				// First let owner of file updateGUI file record
				InetAddress fileOwner = nameServer.getFilelocation(fileName);
				udp.sendMessage(fileOwner, Client.UDP_CLIENT_PORT, Protocol.FILE_LOCATION_UNAVAILABLE, Integer.toString(this.hash));
			}
		} catch (IOException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * This method moves all of its owned files to the node
	 */
	private void moveFilesToNode(int nodeHash) {
		if (nodeHash == getHash()) {
			System.out.println("Not moving files to myself. Am I the last node?");
		} else {
			try {
				for (FileRecord record : ownedFiles) {
					String fileName = record.getFileName();

					InetAddress nodeLocation = nameServer.lookupNode(nodeHash);
					File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
					tcp.sendFile(nodeLocation, file, true);
				}
			} catch (IOException e) {
				System.err.println("Moving local files to previous node failed");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Removes the given node from the list of available locations of the owned files
	 * 
	 * @param dcNode Address of node unavailable node
	 */
	public synchronized void removeFileLocation(int nodeHash) {
		//synchronized (ownedFiles) {
			for (FileRecord record : ownedFiles) {
				// Remove download location of file
				record.removeNode(nodeHash);

				// Remove file from owned files list + file itself
				if (record.getNodeHashes().isEmpty()) {
					ownedFiles.remove(record);
					File file = Paths.get(OWNED_FILE_PATH + record.getFileName()).toFile();
					file.delete();
				}
			}

		//}
	}

	/**
	 * This methodes will be called when a file is received.
	 * The node will check if it's the owner of the file and if so, add the sender to the list of avialable location
	 * 
	 * @param nodeHash Hash of the node on which the file is available
	 * @param fileName Name of the file
	 */
	private void addFileCopy(int nodeHash, String fileName) {
		for (FileRecord record : ownedFiles) {
			// Search for file in owned files list
			if (record.getFileName().equals(fileName)) {
				record.addNode(nodeHash);
			}
		}
	}

	/**
	 * Request a download by placing a lock on a file and waiting for the FileAgent to initiate the download
	 * 
	 * @param fileName Name of the file
	 */
	public void requestDownload(String fileName) {
		setIsDownloading(true);
		lockRequests.put(fileName, true);
		
	}

	/**
	 * This method will be called by the FileAgent when the client can download a file
	 * 
	 * @param fileName Name of the file
	 */
	public void startDownload(String fileName) {
		try {
			InetAddress fileOwner = nameServer.getFilelocation(fileName);
			udp.sendMessage(fileOwner, UDP_CLIENT_PORT, Protocol.DOWNLOAD_REQUEST, fileName);
		} catch (IOException e) {
			System.err.println("Error while requesting file download");
			e.printStackTrace();
		}
	}
	
	/**
	 * Request a deletion by placing a lock on a file and waiting for the FileAgent to initiate the deletion
	 * 
	 * @param fileName	The file to delete
	 */
	public void requestDeletionNetworkFile(String fileName){
		setIsDeleting(true);
		lockRequests.put(fileName, true);
	}

	@Override
	public void receiveAgent(final IAgent agent) {
		System.out.printf("Received agent %s.%n", agent);
		final Client thisClient = this;

		Runnable run = new Runnable() {
			public void run() {
				boolean failed = false;
				try {
					InetAddress nextNodeLocation = nameServer.lookupNode(nextNodeHash);
					String nextNodeAddress;
					if (nextNodeLocation == null) {
						nextNodeAddress = thisClient.getAddress().getHostAddress();
					}
					else {
						nextNodeAddress = nextNodeLocation.getHostAddress();
					}

					boolean sendAgent = agent.setCurrentClient(thisClient);
					Thread agentThread = new Thread(agent);
					agentThread.start();
					agentThread.join();

					// As long as there are no other nodes in the network, don't send the agent
					while (thisClient.getAddress().getHostAddress().equals(nextNodeAddress)) {
						Thread.sleep(10000);
						nextNodeAddress = nameServer.lookupNode(nextNodeHash).getHostAddress();
					}

					Thread.sleep(5000);
					if (sendAgent) {
						try {
							agent.prepareToSend();
							Registry registry = LocateRegistry.getRegistry(nextNodeAddress, Client.rmiPort);
							IClient nextClient = (IClient) registry.lookup(Client.bindLocation);
							nextClient.receiveAgent(agent);
						} catch (ConnectException e) {
							// assuming next client has failed
							removeFailedNode(getNextNodeHash());
						}
					}
				} catch (InterruptedException e) {
					System.err.println("Interrupted while waiting for agent thread");
					e.printStackTrace();
					failed = true;
				} catch (RemoteException e) {
					System.err.println("Error while locating registry or client");
					e.printStackTrace();
					failed = true;
				} catch (NotBoundException e) {
					System.err.println("Error while looking up remote client");
					e.printStackTrace();
					failed = true;
				} finally { 
					//TODO is dit goed?
					if (failed && agent instanceof FileAgent) {
						thisClient.receiveAgent(new FileAgent()); 
					}
				 }

			}
		};

		Thread wrapperThread = new Thread(run);
		wrapperThread.start();

	}

	//TODO debugging
	public String debugInfo() {
		return "Name: " + this.getName() + " Hash: " + this.getHash() + " IP: " + this.getAddress().getHostAddress();
	}

	//TODO debugging
	public String debugLocks() {
		String result = "Locks: \n";
		for (Entry<String, Boolean> request : lockRequests.entrySet()) {
			result += "Filename :" + request.getKey() + " - " + request.getValue() + "\n";
		}
		return result;
	}

	//TODO debugging
	public String debugRequestLock(String filename) {
		lockRequests.put(filename, true);
		return "Placed lock on file: " + filename;
	}

	//TODO debugging
	public String debugRequestUnlock(String filename) {
		lockRequests.put(filename, false);
		return "Removed lock from file: " + filename;
	}

	//TODO debugging
	/**
	 * Necessary for GUI to know
	 * @param fileName		file to check
	 * @return				true if file is available locally
	 */
	public boolean hasLocalFile(String fileName) {
		for (String file : localFiles) {
			if (fileName == file) {
				return true;
			}
		}
		return false;
	}

	//TODO debugging
	/**
	 * Open a local file
	 * @param fileName	file to open
	 * @return			error message for gui
	 */
	public String openFile(String fileName) {
		File file = null;
		if(hasLocalFile(fileName)){
			file = new File(LOCAL_FILE_PATH + fileName);
		}
		else if(hasOwnedFile(fileName)){
			file = new File(OWNED_FILE_PATH + fileName);
		}
		try {
			Desktop.getDesktop().open(file);
			return null;
		} catch (Exception e) {
			return "Failed to open file. " + e.getMessage();
		}
	}
	
	/**
	 * Link the GUI with the data logic
	 * 
	 * @param view		The view class
	 * @param model		The model class
	 * @param controller	The controller class
	 */
	public void setGUI(View view, Model model, Controller controller){
		//this.view = view;
		//this.model = model;
		this.controller = controller;
		updateGUI();
	}
	
	/**
	 * Update the file list
	 */
	public void updateGUI(){
		controller.updateView();
	}
	
	/**
	 * Send a request to the owner to delete download location in file record node list
	 * 
	 * @param fileName		The file to remove locally
	 * @param location		The location to remove from the file record
	 */
	public void requestFileLocationDelete(String fileName, int location){
		location = getHash();
		try {
			InetAddress fileOwner = nameServer.getFilelocation(fileName);
			udp.sendMessage(fileOwner, UDP_CLIENT_PORT, Protocol.DELETE_LOCAL_REQUEST, fileName + " " + location);
			for (String localfile : localFiles){
				if(fileName == localfile){
					File file = Paths.get(LOCAL_FILE_PATH + fileName).toFile();
					file.delete();
					localFiles.remove(localfile);
				}
			}
		} catch (RemoteException e) {
			System.out.println("Error while getting file location from server.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error while requesting deletion of file location.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Delete a given location from the filerecord node list
	 * 
	 * @param fileName		The file delete the location from
	 * @param location		The location to delete
	 */
	public void deleteFileLocation(String fileName, int location){
		for (FileRecord record : ownedFiles) {
			if (fileName.equals(record.getFileName())) {
				if(record.getNodeHashes().size()>1){
					record.removeNode(location);
				}
			}
		}
	}
	
	/**
	 * When receiving a request to delete a file, the node searches first if it's in its local files,
	 * then in its owned and finally deletes it
	 * 
	 * @param fileName		File to delete
	 */
	public void deleteFile(String fileName){
		for (String localfile : localFiles){
			if(fileName == localfile){
				File file = Paths.get(LOCAL_FILE_PATH + fileName).toFile();
				file.delete();
				localFiles.remove(localfile);
			}
		}
		for (FileRecord record : ownedFiles) {
			if (fileName.equals(record.getFileName())) {
				File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
				file.delete();
				ownedFiles.remove(record);
			}
		}
	}
	
	/**
	 * After receiving a request for the download locations of a file, the node returns them in a string
	 * 
	 * @param fileName	File to get download locations from
	 * @return			Download locations in string form
	 */
	public String getDownloadLocations(String fileName){
		String str = null;
		for (FileRecord record : ownedFiles) {
			if (fileName.equals(record.getFileName())) {
				TreeSet<Integer> hashes = record.getNodeHashes();
				for(Integer i : hashes){
					str += i + " ";
				}
			}
		}
		return str;
	}
	
	/**
	 * After receiving the download locations, the client asks all the locations to delete the file, then the owner
	 * 
	 * @param fileName		File to delete
	 * @param locations		Locations to send request to
	 */
	public void deleteNetworkFile(String fileName, TreeSet<Integer> locations){
		TreeSet<Integer> locs = locations;
		for(Integer hash : locs){
			try {
				InetAddress nodeAddress = nameServer.lookupNode(hash);
				udp.sendMessage(nodeAddress, UDP_CLIENT_PORT, Protocol.DELETE_NETWORK_REQUEST, fileName);
			} catch (RemoteException e) {
				System.err.println("Error while contacting server." + e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Delete network file request failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
		try {
			InetAddress ownerAddress = nameServer.getFilelocation(fileName);
			udp.sendMessage(ownerAddress, UDP_CLIENT_PORT, Protocol.DELETE_NETWORK_REQUEST, fileName);
		} catch (RemoteException e) {
			System.err.println("Error while contacting server." + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("'Delete network file request' failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Ask the owner for the download locations of a file
	 * 
	 * @param fileName	File of which you want to know the nodes
	 */
	public void requestFileLocations(String fileName){
		try {
			InetAddress ownerAddress = nameServer.getFilelocation(fileName);
			udp.sendMessage(ownerAddress, UDP_CLIENT_PORT, Protocol.FILE_LOCATIONS_REQUEST, getAddress() + " " + fileName);
		} catch (IOException e) {
			System.err.println("'File download locations request' failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
