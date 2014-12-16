package be.uantwerpen.ds.system_y.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.DefaultListModel;

import be.uantwerpen.ds.system_y.client.*;
import be.uantwerpen.ds.system_y.file.FileRecord;

public class Model {
	Client client;
	TreeMap<String, Boolean> map;
	DefaultListModel<String> list;
	TreeSet<String> localfiles;
	TreeSet<String> ownedfiles;
	
	public Model(){
		map = new TreeMap<String, Boolean>();
		list = new DefaultListModel<String>();
		localfiles = new TreeSet<String>();
		ownedfiles = new TreeSet<String>();
		addToTreemap(map);//for testing
	}
	
	public void requestDisconnect(){
		//TODO returnwaarde of niet?
		//client.disconnect();
		System.out.println("Disconnected"); // for testing
	}
	
	private void getFilesFromDB(){
		list.clear();
		//TreeMap<String, Boolean> map = client.getAvailableFiles();
		TreeMap<String, Boolean> map = getAvailableFiles(); //for testing
		for (Map.Entry<String, Boolean> entry : map.entrySet()) {
			list.addElement(entry.getKey());
		}
	}
	
	public DefaultListModel<String> getList(){
		getFilesFromDB();
		return list;
	}
	
	//for testing
	public void addToTreemap(TreeMap<String, Boolean> map){
		for(int i=0;i<10;i++){
			map.put("File" + i + ".txt", true);
			if(i%2==0){
				setLocals("File" + i + ".txt");
			}
			else{
				setOwneds("File" + i + ".txt");
			}
		}
	}
	
	public String openFile(String filename){
		if (fileOnSystem(filename)){ // for testing
			System.out.println("Opening local file"); // for testing
			return openDownloadedFile(filename); // for testing
		}
		else{ // for testing
			requestDownload(filename); // for testing
			startDownload(filename); // for testing
			return openDownloadedFile(filename); // for testing
		}
		
		//client.requestDownload(filename);
		//return client.openFile(filename); // geef error message mee voor client
	}
	
	/**
	 * Checks if file is locked or not in list of files on network
	 * 
	 * @param filename	filename to check
	 * @return			return false if locked
	 */
	public boolean isAvailable(String filename){
		boolean isAvailable = false;
		TreeMap<String, Boolean> map = getAvailableFiles(); //for testing
		for (Map.Entry<String, Boolean> entry : map.entrySet()) {
			if(filename == entry.getKey()){
				isAvailable = entry.getValue();
			}
		}
		return isAvailable;
	}
	
	/**
	 * This method gets the list of available files on the network
	 * @return
	 */
	public TreeMap<String, Boolean> getAvailableFiles(){
		return map;
	}
	
	/**
	 * This method checks if a file is owned by the client (necessary for buttons)
	 * 
	 * @param filename	file to check
	 * @return			return true if owned
	 */
	public boolean isOwnedFile(String filename){
		getFilesFromDB();
		//if(client.checkOwnedFiles(filename)){
		if(checkOwnedFiles(filename)){ // for testing
			return true;
		}
		return false;
	}
	
	// for testing opening file
	public boolean fileOnSystem(String filename){
		getFilesFromDB();
		//if(client.checkLocalFiles(filename)){
		if(checkLocalFiles(filename)){ // for testing
			//System.out.println("File is on system!");
			return true;
		}
		//System.out.println("File is NOT on system!");
		return false;
	}
	
	//for testing
	public boolean checkOwnedFiles(String filename) {
		getFilesFromDB();
		for (String file : ownedfiles) {
			if (filename.compareTo(file)==0){
				return true;
			}
		}
		return false;
	}
	
	//for testing
	private void requestDownload(String filename){
		System.out.println("Locking file");
	}
	
	//for testing
	private void startDownload(String filename){
		System.out.println("Downloading file...");
	}
	
	// for testing
	public boolean checkLocalFiles(String filename){
		for (String file : localfiles) {
			if (filename.compareTo(file)==0){
				return true;
			}
		}
		return false;
	}
	
	// for testing
	public void setLocals(String addstr){
		localfiles.add(addstr);
	}
	
	// for testing
	public void setOwneds(String addstr){
		ownedfiles.add(addstr);
	}
	
	// for testing
	public String openDownloadedFile(String fileName){
		File file = new File("files/" + fileName);
		System.out.println("Opening downloaded file...");
		try {
			Desktop.getDesktop().open(file);
			return null;
		} catch (Exception e) {
			return "Failed to open file. " + e.getMessage();
		}
	}
}
