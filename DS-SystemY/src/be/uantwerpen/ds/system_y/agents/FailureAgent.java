package be.uantwerpen.ds.system_y.agents;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.connection.TCPHandler;
import be.uantwerpen.ds.system_y.file.FileRecord;
import be.uantwerpen.ds.system_y.server.INameServer;

public class FailureAgent implements Serializable, Runnable {

	/**
	 * Generated serial ID
	 */
	private static final long serialVersionUID = 4679865478722711921L;
	
	private Client client;
	private INameServer nameServer;
	private TCPHandler tcp;
	
	InetAddress failedNodeLocation;
	private int startNodeHash;	
	
	private boolean succes;
	
	/**
	 * 
	 * @param failureHash 	The node that failed
	 * @param clientHash	The node on which the agent started
	 */
	public FailureAgent(int clientHash, InetAddress failedNodeLocation){
		this.startNodeHash = clientHash;
		this.failedNodeLocation = failedNodeLocation;
	}
	
	public void setCurrentClient(Client client) {
		this.client = client;
		this.nameServer = client.getNameServer();
		//TODO TCP van client ophalen
	}
	
	public int getStartNodeHash() {
		return startNodeHash;
	}


	@Override
	public void run() {
		try {
			InetAddress fileLocation;
			
			for (FileRecord record : client.getOwnedFiles()){
				fileLocation = nameServer.getFilelocation(record.getFileName());
				
				if (fileLocation.equals(failedNodeLocation)) {
					//TODO hoe controleren of nieuwe eigenaar al eigenaar is?
					if (succes) {
						
					}
					
				}
			}
		} catch (RemoteException e) {
			System.err.println("Error while contacting nameserver");
			e.printStackTrace();
		}
		
	}
	
	public void run2(){
		
		//Lijst bestanden van client opvragen
		List<FileRecord> ownedFiles = client.getOwnedFiles();
		
		//Controle uitvoeren om te zien of bestand op "failureHash" staat
		for (FileRecord record : ownedFiles){
			
			if (record.getFileHash() == failedNodeHash){
				try{
					//Remove failNode to get new owner
					String fileName = record.getFileName();
					succes = nameServer.unregisterNode(fileName);
					
					if(succes == true){
						InetAddress newOwner = nameServer.getFilelocation(fileName);
						ArrayList<InetAddress> aviableFile = record.getNodes();
						
						// Ckeck in fileRecord if file is aviable on "newOwner"
						for(InetAddress fileDirect : aviableFile){
							//If not available on "newOwner" 
							if (fileDirect != newOwner){
								// Send file to "newOwner"
								File file = Paths.get(Client.LOCAL_FILE_PATH, fileName).toFile();
								tcp.sendFile(newOwner, file, true);
								// Update fileFishe "newOwner" 
								record.addNode(newOwner);
								// Change downloadlocation file from "newOwner"
								
							}else{
								
							}
						}						
					}else{
						//couldn't remove node
						System.err.println("Unable to remove failNode with hash: "+failedNodeHash);
					}
					
				} catch (RemoteException e) {
					System.err.println("Unable to contact nameServer");
					e.printStackTrace();
				}
				
			} else{
				// there is no file owend by the "failureHash"
			}
		}
		
		/*	1. 	Stuur bestand door naar de nieuwe eigenaar, als de nieuwe eigenaar nog geen		// 
		//		eigenaar is van het bestand en niet over het bestand beschikt. Update bij de 	//
		//		nieuwe eigenaar (bestandsfiche) de informatie over waar het bestand 			//
		//		beschikbaar is in het systeem met als downloadlocatie de huidige node			*/

		/*	2. 	Als de nieuwe eigenaar al eigenaar is door vorige acties van agent, update de 	//
		//		nieuwe eigenaar met de informatie dat het bestand beschikbaar is op de 			//
		//		huidige node																	*/
		
	

		//Check voor "startupHash"
	}
}
