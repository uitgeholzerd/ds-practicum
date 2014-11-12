package be.uantwerpen.ds.system_y.agents;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.uantwerpen.ds.system_y.client.Client;
import be.uantwerpen.ds.system_y.file.FileRecord;

public class FileAgent implements Runnable, Serializable {

	private static final long serialVersionUID = -7644508104728738008L;

	private TreeMap<String, Boolean> availableFiles;
	private Client client;

	public FileAgent() {
		availableFiles = new TreeMap<String, Boolean>();
	}

	public void setCurrentClient(Client client) {
		this.client = client;
	}

	@Override
	public void run() {
		Map<String, Boolean> clientAvailableFiles = client.getAvailableFiles();
		
		// Add new files to the list
		List<FileRecord> ownedFiles = client.getOwnedFiles();
		for (FileRecord fileRecord : ownedFiles) {
			if (!availableFiles.containsKey(fileRecord.getFileName())) {
				clientAvailableFiles.put(fileRecord.getFileName(), false);
				availableFiles.put(fileRecord.getFileName(), false);
			}
		}
		
		Map<String, Boolean> clientLockrequests = client.getLockRequests();
		for (Entry<String, Boolean> entry : clientLockrequests.entrySet()) {
			//If the node has a lock request and the file is not locked,
			//lock the file and start the download. Else do nothing
			if (entry.getValue() && !availableFiles.get(entry.getKey())) {
				//TODO start download on client
				availableFiles.put(entry.getKey(), true);
				clientLockrequests.remove(entry);
			}
			//If the node has an unlock request, unlock the file
			else if (!entry.getValue()) {
				availableFiles.put(entry.getKey(), false);
				clientLockrequests.remove(entry);
			}
		}

	}

}
