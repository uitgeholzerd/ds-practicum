package be.uantwerpen.ds.system_y.agents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.file.FileRecord;

public class FileAgent implements Runnable, Serializable {

	private static final long serialVersionUID = -7644508104728738008L;
	
	private TreeMap<String, Boolean> availableFiles;
	private Client client;
	
	public void setCurrentClient(Client client){
		this.client = client;
	}
	
	@Override
	public void run() {
		TreeMap<String, Boolean> clientAvailableFiles = client.getAvailableFiles();

		//Check if there are any lock requests from this owner
		for (Entry<String, Boolean> entry : clientAvailableFiles.entrySet()) {
			if (entry.getValue()) {
				boolean isLocked = availableFiles.get(entry.getKey());
				if (!isLocked) {
					// TODO allow client to download file
					//availableFiles.
				}
			}
		}
		
		//Add new files to the list
		ArrayList<FileRecord> ownedFiles = client.getOwnedFiles();
		for (FileRecord fileRecord : ownedFiles) {
			if (!availableFiles.containsKey(fileRecord.getFileName())) {
				clientAvailableFiles.put(fileRecord.getFileName(), false);
			}
		}
		client.setAvailableFiles(availableFiles);
		
	}

}
