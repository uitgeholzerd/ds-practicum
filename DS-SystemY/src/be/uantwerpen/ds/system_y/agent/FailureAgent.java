package be.uantwerpen.ds.system_y.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.server.INameServer;

/**
 * The agent that will be sent around when a failed node is detected.
 * It's job is to make sure file references to the failed are corrected
 *
 */
public class FailureAgent implements IAgent {

	private static final long serialVersionUID = 4679865478722711921L;

	private Client client;
	private INameServer nameServer;

	private InetAddress failedNodeLocation;
	private int failedNodeHash;
	private int startNodeHash;

	private boolean firstRun;

	/**
	 * Construtor for the failureAgent, save needed data in Agent:
	 * @param failureHash The node that failed
	 * @param clientHash The node on which the agent started
	 */
	public FailureAgent(int clientHash, int failedNodeHash, InetAddress failedNodeLocation) {
		System.out.printf("Failure agent created for node with hash %d (%s).\n", failedNodeHash, failedNodeLocation.getHostAddress());
		firstRun = true;
		this.startNodeHash = clientHash;
		this.failedNodeHash = failedNodeHash;
		this.failedNodeLocation = failedNodeLocation;
	}
	
	@Override
	public boolean setCurrentClient(Client client) {
		if (!firstRun && client.getHash() == startNodeHash) {
			/*
			try {
				nameServer.unregisterNode(failedNodeHash);
			} catch (RemoteException e) {
				System.err.println("Failed to contact name server");
				e.printStackTrace();
			}
			*/
			this.client = client;
			return false;
		} else {
			this.client = client;
			this.nameServer = client.getNameServer();
			firstRun = false;
			return true;
		}
	}

	/**
	 * Run from the agent, here he will do the next steps:
	 * 		Get neighbours from failurenode
	 * 		Remove file location from failurenode
	 * 		Check for ownerfiles from failurenode
	 * 		Lookup from nameserver
	 * 		Check if owner or not
	 * 		Change failureowner to newowner
	 * 		Remove failurenode
	 */
	@Override
	public void run() {
		// Dont perform actions if the cycle is complete (these actions were already performed during the first run)
		if (!firstRun && client.getHash() == startNodeHash) {
			return;
		}
		
		try {
			boolean failedNodeWasOwner;
			
			//Remove failed node from available file locations
			client.removeFileLocation(failedNodeHash);
			
			//If the failed node was owner of any of the local files, send the files to the new owner
			for (String fileName : client.getLocalFiles()) {
				failedNodeWasOwner = nameServer.isFileOwner(failedNodeHash, failedNodeLocation, fileName);
				InetAddress newOwner = nameServer.getFilelocation(fileName);
				
				if (failedNodeWasOwner) {
					// If the new owner already has the file, let him know the file is available at the current location
					if (client.getTCPHandler().checkFileOwner(newOwner, fileName)) {
						try {
							client.getUDPHandler().sendMessage(newOwner, Client.UDP_CLIENT_PORT, Protocol.FILE_LOCATION_AVAILABLE, fileName);
						} catch (IOException e) {
							System.err.println("Error while sending UDP message");
							e.printStackTrace();
						}
					} 
					// If the new owner doesn't of the file yet, send it to him
					else {
						try {
							client.getTCPHandler().sendFile(newOwner, new File(Client.LOCAL_FILE_PATH + fileName), true);
						} catch (IOException e) {
							e.printStackTrace();
							// Remote node could not be reached and should be removed
							client.removeFailedNode(nameServer.reverseLookupNode(newOwner.getHostAddress()));
						}
					}

				}
			}
		} catch (RemoteException e) {
			System.err.println("Error while contacting nameserver");
			e.printStackTrace();
		}

		System.out.println("Failure agent run complete.");

	}

	@Override
	public void prepareToSend() {
		this.client = null;
	}
	
}
