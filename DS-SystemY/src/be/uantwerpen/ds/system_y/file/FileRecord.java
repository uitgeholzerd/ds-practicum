package be.uantwerpen.ds.system_y.file;

import java.net.InetAddress;
import java.util.ArrayList;


public class FileRecord {
	private String fileName;
	private int fileHash;
	private ArrayList<InetAddress> nodes;
	
	public FileRecord(String fileName, int fileHash) {
		this.fileName = fileName;
		this.fileHash = fileHash;
		this.nodes = new ArrayList<InetAddress>();
	}
	
	public String getFileName() {
		return fileName;
	}

	public int getFileHash() {
		return fileHash;
	}

	public boolean addNode(InetAddress node) {
		return nodes.add(node);
	}
	
	public boolean removeNode(InetAddress node) {
		return nodes.remove(node);
	}
	
	public ArrayList<InetAddress> getNodes() {
		return nodes;
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
