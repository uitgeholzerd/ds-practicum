package be.uantwerpen.ds.system_y.connection;

public interface FileReceiver {
	
	
	/**
	 * This method is triggered when the TCPHandler receives a file and passes it to the FileReceiver
	 * 
	 * @param fileHash	Hash of the file
	 * @param fileName	Name of the file
	 */
	void fileReceived(int fileHash, String fileName);
}
