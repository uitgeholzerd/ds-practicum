package be.uantwerpen.ds.system_y.agent;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.file.FileRecord;

public class FileAgent implements IAgent {

	private static final long serialVersionUID = -7644508104728738008L;

	private TreeMap<String, Boolean> availableFiles;
	private Client client;

	public FileAgent() {
		availableFiles = new TreeMap<String, Boolean>();
	}

	@Override
	public boolean setCurrentClient(Client client) {
		this.client = client;
		return true;
	}

	@Override
	public void run() {	
		System.out.println("FileAgent started on " + client.getName());
		// Add new files to the file agents list
		List<FileRecord> ownedFiles = client.getOwnedFiles();
		for (FileRecord fileRecord : ownedFiles) {
			if (!availableFiles.containsKey(fileRecord.getFileName())) {
				System.out.println("FileAgent found new file  " + fileRecord.getFileName());
				availableFiles.put(fileRecord.getFileName(), false);
			}
		}

		// Update the clients file list
		for (Entry<String, Boolean> entry : availableFiles.entrySet()) {
			if (!client.getAvailableFiles().contains(entry.getKey())) {
				client.getAvailableFiles().add(entry.getKey());
			}
		}
		
		//If the node has a lock request and the file is not locked, lock the file and start the download. 
		//If the node has a unlock request, release the lock
		TreeMap<String, Boolean> clientLockrequests = client.getLockRequests();
		for (Entry<String, Boolean> entry : clientLockrequests.entrySet()) {
			if (entry.getValue() == null || availableFiles.get(entry.getKey()) == null) {
				// This if-statement is included to prevent the next ones from failing when the value is null
				// The value of the entry.getValue() is null when the file agent has granted the node a lock on the file
			}
			else if (entry.getValue() && !availableFiles.get(entry.getKey())) {
				availableFiles.put(entry.getKey(), true);
				clientLockrequests.put(entry.getKey(), null);
				System.out.println("FileAgent locked file '" + entry.getKey() + "' and started the download on client");
				client.startDownload(entry.getKey());
			}
			else if (!entry.getValue()) {
				availableFiles.put(entry.getKey(), false);
				clientLockrequests.remove(entry.getKey());
				System.out.println("FileAgent unlocked file '" + entry.getKey() + "'");
			}
		}
	}
	
	@Override
	public void prepareToSend() {
		this.client = null;
	}

}
