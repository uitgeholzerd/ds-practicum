package be.uantwerpen.ds.system_y.gui;

import java.util.HashSet;
import java.util.TreeSet;

import javax.swing.DefaultListModel;

import be.uantwerpen.ds.system_y.client.*;

public class Model{
	Client client;
	HashSet<String> map;
	DefaultListModel<String> list;
	TreeSet<String> localfiles;
	TreeSet<String> ownedfiles;
	
	public Model(Client client){
		map = new HashSet<String>();
		list = new DefaultListModel<String>();
		localfiles = new TreeSet<String>();
		ownedfiles = new TreeSet<String>();
		this.client = client;
	}
	
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
	 * @return
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
	 * @param fileName
	 * @return
	 */
	public String openFile(String fileName){
		client.requestDownload(fileName);
		return client.openFile(fileName); // geef indien nodig error message mee voor gui
	}
	
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
	 * @param fileName
	 * @return
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
	 * @param fileName
	 * @return
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
