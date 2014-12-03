package be.uantwerpen.ds.system_y.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.connection.Protocol;
import be.uantwerpen.ds.system_y.connection.TCPHandler;
import be.uantwerpen.ds.system_y.file.FileRecord;
import be.uantwerpen.ds.system_y.server.INameServer;

public class FailureAgent implements IAgent {

	/**
	 * Generated serial ID
	 */
	private static final long serialVersionUID = 4679865478722711921L;

	private Client client;
	private INameServer nameServer;
	private TCPHandler tcp;

	private InetAddress failedNodeLocation;
	private String failedNodeName;
	private int startNodeHash;

	private boolean firstRun;

	/**
	 * 
	 * @param failureHash The node that failed
	 * @param clientHash The node on which the agent started
	 */
	public FailureAgent(int clientHash, String failedNodeName, InetAddress failedNodeLocation) {
		firstRun = true;
		this.startNodeHash = clientHash;
		this.failedNodeName = failedNodeName;
		this.failedNodeLocation = failedNodeLocation;
	}

	public boolean setCurrentClient(Client client) {
		if (!firstRun && client.getHash() == startNodeHash) {
			return false;
		} else {
			this.client = client;
			this.nameServer = client.getNameServer();
			//this.tcp = tcp = new TCPHandler(Client.TCP_CLIENT_PORT, this);
			firstRun = false;
			return true;
		}
	}

	@Override
	public void run() {
		try {
			InetAddress fileLocation;
			InetAddress newOwner;
			
			for (String file : client.getLocalFiles()) {
				fileLocation = nameServer.getFilelocation(file);

				if (fileLocation.equals(failedNodeLocation)) {
					newOwner = nameServer.lookupNeighbours(failedNodeName)[0];
					// TODO hoe controleren of nieuwe eigenaar al eigenaar is?
					if (true) {
							client.getTCPHandler().sendFile(newOwner, new File(file), true);
					} else {
						try {
							client.getUDPHandler().sendMessage(newOwner, Client.UDP_CLIENT_PORT, Protocol.FILE_AVAILABLE, file);
						} catch (IOException e) {
							// TODO Auto-generated catch block
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
