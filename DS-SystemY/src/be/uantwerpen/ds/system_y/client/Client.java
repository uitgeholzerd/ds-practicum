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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import be.uantwerpen.ds.system_y.agent.FailureAgent;
import be.uantwerpen.ds.system_y.agent.IAgent;
import be.uantwerpen.ds.system_y.connection.DatagramHandler;
import be.uantwerpen.ds.system_y.connection.FileReceiver;
import be.uantwerpen.ds.system_y.connection.MessageHandler;
import be.uantwerpen.ds.system_y.connection.MulticastHandler;
import be.uantwerpen.ds.system_y.connection.PacketListener;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.connection.TCPHandler;
import be.uantwerpen.ds.system_y.file.FileRecord;
import be.uantwerpen.ds.system_y.server.INameServer;

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
	private Path filedir;

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

	/**
	 * Joins the multicast group, sends a discovery message and starts listening for replies. Check the connections with the nameserver after a delay and start the
	 * regular scanning for new files
	 */
	public void connect() {
		try {
			// set up UDP socket and receive messages
			System.out.println("Connecting to network...");
			udp = new DatagramHandler(UDP_CLIENT_PORT, this);
			// join multicast group
			group = new MulticastHandler(this);
			this.name = getAddress().getHostName();
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
		System.out.println("Scanning files");
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
			timer.cancel();
			// Make the previous node the new owner of the files owned by the current node
			moveFilesToNode(previousNodeHash);
			// Warn the owner of the replicated files of the current node to update its file records
			warnFileOwner();

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
				System.out.println("Closing sockets failed: " + e.getMessage());
			}
			System.out.println("Disconnected from network");
			udp = null;
			group = null;
			tcp = null;
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
			removeFileLocation(message[1]);
			break;
			
		case FILE_LOCATION_AVAILABLE:
			addFileCopy(message[1], message[2]);
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
		System.out.printf("Removing failed node with hash %d", nodeHash);
		try {
			InetAddress[] neighbours = nameServer.lookupNeighbours(1);
			if (neighbours != null) {
				InetAddress prevNodeAddress = neighbours[0];
				InetAddress nextNodeAddress = neighbours[1];

				// Send the previous node of the failed node to the next node of the failed note and vice versa
				udp.sendMessage(prevNodeAddress, Client.UDP_CLIENT_PORT, Protocol.SET_NEXTNODE, "" + nameServer.getShortHash(neighbours[1]));
				udp.sendMessage(nextNodeAddress, Client.UDP_CLIENT_PORT, Protocol.SET_PREVNODE, "" + nameServer.getShortHash(neighbours[0]));

				//Retrieve the location of the failed node, then remove it from the nameserver
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
					if (newRecord.equals(localRecord)) {
						hasTheFile = true;
						break;
					}
				}
				// If file already exists in records, replicate this file to previous node
				if (hasTheFile) {
					InetAddress previousNode = nameServer.lookupNode(previousNodeHash);
					File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
					try {
						tcp.sendFile(previousNode, file, false);
					} catch (IOException e) {
						// Remote node could not be reached and should be removed
						removeFailedNode(nameServer.reverseLookupNode(sender.getHostAddress()));
					}
					
				}
				// Else create record and add sender to downloadlocations
				else {
					newRecord.addNode(sender);
					ownedFiles.add(newRecord);
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
					record.addNode(previousNode);
				}

				ownedFiles.add(record);
				file.renameTo(new File(OWNED_FILE_PATH + fileName));
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
	 * This method is triggered when a new node join the system The current node checks if the new node should be the owner of any of the current node's owned files
	 * 
	 */
	public void recheckOwnedFiles() {
		System.out.println("Rechecking owned files...");
		InetAddress owner;
		String fileName;
		for (Iterator<FileRecord> iterator = ownedFiles.iterator(); iterator.hasNext();) {
			FileRecord record = (FileRecord) iterator.next();
			try {
				fileName = record.getFileName();
				owner = nameServer.getFilelocation(fileName);
				if (!this.getAddress().equals(owner)) {
					File file = Paths.get(OWNED_FILE_PATH + fileName).toFile();
					try {
						tcp.sendFile(owner, file, true);
					} catch (IOException e) {
						e.printStackTrace();
						removeFailedNode(nameServer.reverseLookupNode(owner.getHostAddress()));
					}
					iterator.remove();
					file.delete();
				}
			} catch (RemoteException e) {
				System.err.println("Unable to contact nameServer");
				e.printStackTrace();
			}
		}

	}
	public boolean isFileOwner(String fileName){
		for (Iterator<FileRecord> iterator = ownedFiles.iterator(); iterator.hasNext();) {
			FileRecord record = (FileRecord) iterator.next();
			if (record.getFileName().equalsIgnoreCase(fileName)) return true;
		}
		return false;
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
		InetAddress host = nameServer.lookupNodeByName(nodeName);
		if (udp == null) {
			System.err.println("Can't ping if not connected!");
			return;
		}

		final String uuid = UUID.randomUUID().toString();
		final int nodeHash = nameServer.getShortHash(nodeName);
		udp.sendMessage(host, UDP_CLIENT_PORT, Protocol.PING, uuid);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (receivedPings.remove(uuid)) {
					System.out.println("Ping reply from " + nodeName);
				} else {
					System.err.println("Ping timeout from " + nodeName);
					removeFailedNode(nodeHash);
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
	public String debugLookup(String name) {
		InetAddress result = null;
		try {
			result = nameServer.lookupNodeByName(name);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return result.getHostAddress();
	}

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
	public String debugNodes() {
		return "Previous: " + getPreviousNodeHash() + "\nLocal: " + getHash() + "\nNext: " + getNextNodeHash();
	}

	// TODO Wordt enkel voor testing gebruikt, mag uiteindelijk weg
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

	public String debugAvailableFiles() {
		String result = "Available files:\n";
		for (String entry : getAvailableFiles()) {
			result += entry + "\n";
		}
		return result;
	}

	public String debugLocalFiles() {
		String result = "Local files:\n";
		for (String entry : localFiles) {
			result += entry + "\n";
		}
		return result;
	}

	public String debugOwnedFiles() {
		String result = "Owned files:\n";
		for (FileRecord entry : ownedFiles) {
			result += entry.getFileName() + ": " + entry.getNodes().toString() + "\n";
		}
		return result;
	}

	public String debugFile(String name) {
		String result = "";
		for (FileRecord entry : ownedFiles) {
			if (entry.getFileName().equalsIgnoreCase(name)) {
				result += "File record:\n";
				result += " Name:" + entry.getFileName() + "\n";
				result += " Hash:" + entry.getFileHash() + "\n";
				result += " Nodes:" + entry.getNodes().toString() + "\n";
			}
		}
		return result;
	}

	/**
	 * This method makes sure the owners of the replicated files update their file records by sending a udp message
	 */
	private void warnFileOwner() {
		try {
			for (String fileName : localFiles) {
				// First let owner of file update file record
				InetAddress fileOwner = nameServer.getFilelocation(fileName);
				udp.sendMessage(fileOwner, Client.UDP_CLIENT_PORT, Protocol.FILE_LOCATION_UNAVAILABLE, name);
			}
		} catch (IOException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * This method moves all of its owned files to the previous node
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
	public void removeFileLocation(InetAddress dcNode) {
			for (FileRecord record : ownedFiles) {
				// Remove download location of file
				record.removeNode(dcNode);

				// Remove file from owned files list + file itself
				if (record.getNodes().isEmpty()) {
					ownedFiles.remove(record);
					File file = Paths.get(OWNED_FILE_PATH + record.getFileName()).toFile();
					file.delete();
				}
			}
	}

	/**
	 * Removes the given node from the list of available locations of the owned files
	 * 
	 * @param otherNode Address of node that is shutting down
	 */
	public void removeFileLocation(String otherNode) {
		try {
			InetAddress dcNode = nameServer.lookupNodeByName(otherNode);
			removeFileLocation(dcNode);
			
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	private void addFileCopy(String otherNode, String fileName) {
		try {
			for (FileRecord record : ownedFiles) {
				// Search for file in owned files list
				if (record.getFileName() == fileName) {

					InetAddress node = nameServer.lookupNodeByName(otherNode);
					record.addNode(node);
				}
			}
		} catch (RemoteException e) {
			System.err.println("Unable to contact nameServer");
			e.printStackTrace();
		}
	}

	/**
	 * Request a download by placing a lock on a file and waiting for the FileAgent to initiate the download
	 * 
	 * @param fileName Name of the file
	 */
	public void requestDownload(String fileName) {
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

	@Override
	public void receiveAgent(final IAgent agent) {
		System.out.printf("Received agent %s.", agent);
		final Client thisClient = this;

		Runnable run = new Runnable() {
			public void run() {
				try {
					String nextClientAddress = nameServer.lookupNode(nextNodeHash).getHostAddress();
					System.out.printf("receiveAgent: next client address=%s hash=%d%n", nextClientAddress, nextNodeHash);
					boolean sendAgent = agent.setCurrentClient(thisClient);

					Thread agentThread = new Thread(agent);
					agentThread.start();
					agentThread.join();

					// As long as there are no other nodes in the network, don't send the agent
					while (thisClient.getAddress().getHostAddress().equals(nextClientAddress)) {
						Thread.sleep(10000);
						nextClientAddress = nameServer.lookupNode(nextNodeHash).getHostAddress();
					}
					
					Thread.sleep(5000);
					if (sendAgent) {
						
						try {
							agent.prepareToSend();
							Registry registry = LocateRegistry.getRegistry(nextClientAddress, Client.rmiPort);
							IClient nextClient = (IClient) registry.lookup(Client.bindLocation);
							nextClient.receiveAgent(agent);
						} catch (ConnectException e) {
							// assuming next client has failed
							removeFailedNode(getNextNodeHash());
						}
					}
				} catch (InterruptedException e) {
					System.err.println("Interrupted while waiting for agent thread");
				} catch (RemoteException e) {
					System.err.println("Error while locating registry or client");
					e.printStackTrace();
				} catch (NotBoundException e) {
					System.err.println("Error while looking up remote client");
					e.printStackTrace();
				}
				//TODO
				/*
				 finally {
				 	this.receiveAgent(new FileAgent());
				 }
				 */

			}
		};

		Thread wrapperThread = new Thread(run);
		wrapperThread.start();

	}

	public String debugInfo() {
		return "Name: " + this.getName() + " Hash: " + this.getHash() + " IP: " + this.getAddress().getHostAddress();
	}

	public String debugLocks() {
		String result = "Locks: \n";
		for (Entry<String, Boolean> request : lockRequests.entrySet()) {
			result += "Filename :" + request.getKey() + " - " + request.getValue() + "\n";
		}
		return result;
	}

	public String debugRequestLock(String filename) {
		lockRequests.put(filename, true);
		return "Placed lock on file: " + filename;
	}

	public String debugRequestUnlock(String filename) {
		lockRequests.put(filename, false);
		return "Removed lock from file: " + filename;
	}

	public boolean checkOwnedFiles(String fileName) {
		for (FileRecord record : ownedFiles) {
			if (fileName == record.getFileName()){
				return true;
			}
		}
		return false;
	}
	
	public boolean checkLocalFiles(String fileName) {
		for (String file : localFiles) {
			if (fileName == file){
				return true;
			}
		}
		return false;
	}
	
	public String openFile(String fileName){
		File file = new File(LOCAL_FILE_PATH + fileName);
		try {
			Desktop.getDesktop().open(file);
			return null;
		} catch (Exception e) {
			return "Failed to open file. " + e.getMessage();
		}
	}

}
