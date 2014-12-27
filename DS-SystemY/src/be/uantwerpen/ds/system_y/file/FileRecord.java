package be.uantwerpen.ds.system_y.file;

import java.net.InetAddress;
import java.util.ArrayList;


public class FileRecord {
	private String fileName;
	private int fileHash;
	private ArrayList<Integer> nodeHashes;
	
	public FileRecord(String fileName, int fileHash) {
		this.fileName = fileName;
		this.fileHash = fileHash;
		this.nodeHashes = new ArrayList<Integer>();
	}
	
	public String getFileName() {
		return fileName;
	}

	public int getFileHash() {
		return fileHash;
	}

	public boolean addNode(Integer nodeHash) {
		return nodeHashes.add(nodeHash);
	}
	
	public boolean removeNode(Integer nodeHash) {
		return nodeHashes.remove(nodeHash);
	}
	
	public ArrayList<Integer> getNodeHashes() {
		return nodeHashes;
	}
	

    public boolean equals(Object obj) {
       if (!(obj instanceof FileRecord))
            return false;
        if (obj == this)
            return true;

        FileRecord rec = (FileRecord) obj;
        return (this.getFileName().equals(rec.getFileName())&&this.getFileHash() == rec.getFileHash());
    }

}
