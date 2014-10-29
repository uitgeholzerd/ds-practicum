package be.uantwerpen.ds.system_y;

import java.util.ArrayList;


public class FileRecord {
	private String fileName;
	private int fileHash;
	private ArrayList<String> nodes;
	
	public FileRecord(String fileName, int fileHash) {
		this.fileName = fileName;
		this.fileHash = fileHash;
		this.nodes = new ArrayList<String>();
	}
	
	public String getFileName() {
		return fileName;
	}

	public int getFileHash() {
		return fileHash;
	}

	public boolean addNode(String node) {
		return nodes.add(node);
	}
	
	public boolean removeNode(String node) {
		return nodes.remove(node);
	}
	
	public ArrayList<String> getNodes() {
		return nodes;
	}

}
