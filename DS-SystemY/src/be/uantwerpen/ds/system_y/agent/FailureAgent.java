package be.uantwerpen.ds.system_y.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.server.INameServer;

public class FailureAgent implements IAgent {

	/**
	 * Generated serial ID
	 */
	private static final long serialVersionUID = 4679865478722711921L;

	private Client client;
	private INameServer nameServer;

	private InetAddress failedNodeLocation;
	private int failedNodeHash;
	private int startNodeHash;

	private boolean firstRun;
	private boolean lastRun;

	/**
	 * Construtor for the failureAgent, save needed data in Agent:
	 * @param failureHash The node that failed
	 * @param clientHash The node on which the agent started
	 */
	public FailureAgent(int clientHash, int failedNodeHash, InetAddress failedNodeLocation) {
		System.out.printf("Failure agent created for node with hash %d (%s).%n", failedNodeHash, failedNodeLocation.getHostAddress());
		firstRun = true;
		this.startNodeHash = clientHash;
		this.failedNodeHash = failedNodeHash;
		this.failedNodeLocation = failedNodeLocation;
	}

	@Override
	/**
	 * Set the agent on a client
	 */
	public boolean setCurrentClient(Client client) {
		if (!firstRun && client.getHash() == startNodeHash) {
			lastRun = true;
			return false;
		} else {
			this.client = client;
			this.nameServer = client.getNameServer();
			firstRun = false;
			return true;
		}
	}

	@Override
	/**
	 * Run from the agent, here he will do the next steps:
	 * 		Get neighbours from failurenode
	 * 		Remove file location from failurenode
	 * 		Check for ownerfiles from failurenode
	 * 			Lookup from nameserver
	 * 			Check if owner or not
	 * 			Change failureowner to newowner
	 * 		Remove failurenode
	 */
	public void run() {
		try {
			InetAddress newOwner = nameServer.lookupNeighbours(failedNodeHash)[0];
			boolean isOwner;
			
			//Remove failed node from available file locations
			client.removeFileLocation(failedNodeLocation);
			
			//If the failed node was owner of any of the local files, send the files to the new owner
			for (String file : client.getLocalFiles()) {
				isOwner = nameServer.isFileOwner(failedNodeHash, failedNodeLocation, file);

				if (isOwner) {
					newOwner = nameServer.lookupNeighbours(failedNodeHash)[0];
					// TODO hoe controleren of nieuwe eigenaar al eigenaar is?
					if (client.getTCPHandler().checkFileOwner(newOwner, file)) {
						try {
							client.getUDPHandler().sendMessage(newOwner, Client.UDP_CLIENT_PORT, Protocol.FILE_LOCATION_AVAILABLE, file);
						} catch (IOException e) {
							System.err.println("Error while sending UDP message");
							e.printStackTrace();
						}
					} else {
						try {
							client.getTCPHandler().sendFile(newOwner, new File(client.LOCAL_FILE_PATH + file), true);
						} catch (IOException e) {
							e.printStackTrace();
							// Remote node could not be reached and should be removed
							client.removeFailedNode(nameServer.reverseLookupNode(newOwner.getHostAddress()));
						}
					}

				}
			}
		if (lastRun){
				nameServer.unregisterNode(failedNodeHash);
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
