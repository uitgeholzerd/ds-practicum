package be.uantwerpen.ds.system_y;

import java.util.ArrayList;

import be.uantwerpen.ds.system_y.client.Client;

public class FileRecord {
	private String fileName;
	private String fileHash;
	private ArrayList<Client> nodes;
	
	public FileRecord(String fileName, String fileHash) {
		this.fileName = fileName;
		this.fileHash = fileHash;
	}
	
	public String getFileName() {
		return fileName;
	}

	public String getFileHash() {
		return fileHash;
	}

	public boolean addNode(Client node) {
		return nodes.add(node);
	}
	
	public boolean removeNode(Client node) {
		return nodes.remove(node);
	}
	
	public ArrayList<Client> getNodes() {
		return nodes;
	}

}
