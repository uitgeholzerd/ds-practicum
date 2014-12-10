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

	/**
	 * 
	 * @param failureHash The node that failed
	 * @param clientHash The node on which the agent started
	 */
	public FailureAgent(int clientHash, int failedNodeHash, InetAddress failedNodeLocation) {
		firstRun = true;
		this.startNodeHash = clientHash;
		this.failedNodeHash = failedNodeHash;
		this.failedNodeLocation = failedNodeLocation;
	}

	@Override
	public boolean setCurrentClient(Client client) {
		if (!firstRun && client.getHash() == startNodeHash) {
			return false;
		} else {
			this.client = client;
			this.nameServer = client.getNameServer();
			firstRun = false;
			return true;
		}
	}

	@Override
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
					// TODO hoe controleren of nieuwe eigenaar het bestand al heeft?
					boolean hasFile = true;
					if (!hasFile) {
							client.getTCPHandler().sendFile(newOwner, new File(file), true);
					} else {
						try {
							client.getUDPHandler().sendMessage(newOwner, Client.UDP_CLIENT_PORT, Protocol.FILE_LOCATION_AVAILABLE, file);
						} catch (IOException e) {
							System.err.println("Error while sending UDP message");
							e.printStackTrace();
						}
					}

				}
			}
		} catch (RemoteException e) {
			System.err.println("Error while contacting nameserver");
			e.printStackTrace();
		}

	}
	
	@Override
	public void prepareToSend() {
		this.client = null;
		//this.tcp = null;
	}
	
}
