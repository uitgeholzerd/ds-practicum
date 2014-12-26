package be.uantwerpen.ds.system_y.connection;

import java.net.InetAddress;

public interface FileReceiver {
	/**
	 * This method is triggered when the TCPHandler receives a file and passes it to the FileReceiver
	 * 
	 * @param fileHash Hash of the file
	 * @param fileName Name of the file
	 */
	void receiveFile(InetAddress sender, String fileName, boolean isOwner);

	boolean isFileOwner(String fileName);
}
