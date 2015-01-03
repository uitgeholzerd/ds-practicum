package be.uantwerpen.ds.system_y.gui;

import java.util.HashSet;
import java.util.TreeSet;

import javax.swing.DefaultListModel;

import be.uantwerpen.ds.system_y.client.*;

/**
 * Class that calls the methods from the client based on the input given from the controller
 *
 */
public class Model{
	Client client;
	HashSet<String> map;
	DefaultListModel<String> list;
	
	public Model(Client client){
		map = new HashSet<String>();
		list = new DefaultListModel<String>();
		this.client = client;
	}
	
	/**
	 * Call the disconnect function when clicking the log out button or the X button
	 */
	public void logOut(){
		client.disconnect();
	}
	
	/**
	 * First clear the list, then add all the files that are available on the File Agent's list to the GUI list
	 */
	private void getFilesFromDB(){
		HashSet<String> map = client.getAvailableFiles();
		for (String entry : map) {
			list.addElement(entry);
		}
	}
	
	/**
	 * Getter for the files list
	 * 
	 * @return		The list to return
	 */
	public DefaultListModel<String> getList(){
		list.clear();
		getFilesFromDB();
		System.out.println(list);
		return list;
	}
	
	/**
	 * Start the process at the client to lock the file and download it, eventually open the file
	 * 
	 * @param fileName		The file to open
	 * @return				Error message for the client if it can't be opened
	 */
	public String openFile(String fileName){
		client.requestDownload(fileName);
		return client.openFile(fileName);
	}
	
	/**
	 * Actions for deleting a local file
	 * 
	 * @param fileName		File to delete locally
	 * @return				Error/confirmation message
	 */
	public String deleteLocalFile(String fileName){
		if(client.hasOwnedFile(fileName)){// if owned file
			return "Your own file cannot be removed.";
		}
		else if (client.hasLocalFile(fileName)){// if local file
			client.requestFileLocationDelete(fileName, client.getHash());
			return "File " + fileName + " removed from your local files.";
		}
		else {
			return "File cannot be removed.";
		}
	}
	
	/**
	 * Start the process at the client to lock the file and delete it all over the network
	 * 
	 * @param fileName		File to delete
	 * @return				Error/confirmation message
	 */
	public String deleteNetworkFile(String fileName){
		if(client.hasOwnedFile(fileName)){// if owned file
			return "Your own file cannot be removed.";
		}
		else{
			client.requestDeletionNetworkFile(fileName);
			return "Network file " + fileName + " delete request sent.";
		}
	}
	
	/**
	 * Change the clickability of the button to delete a local file
	 * 
	 * @param fileName		File to check
	 * @return				Set the button active/inactive
	 */
	public boolean changeDeleteLocalBtn(String fileName){
		if (client.hasLocalFile(fileName) && !client.hasOwnedFile(fileName)){
    		return true;
    	}
    	else{
    		return false;
    	}
	}
}
