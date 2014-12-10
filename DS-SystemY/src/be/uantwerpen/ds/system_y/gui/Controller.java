package be.uantwerpen.ds.system_y.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class Controller {
	private Model model;
    private View view;
    private ActionListener logOutAL, openFileAL, deleteFileAL, deleteOwnedFileAL;
    private ListSelectionListener listSL;
    private String selectedFile;
    
    public Controller(Model model, View view){
        this.model = model;
        this.view = view;
        view.setListModel(model.getList(), 1);
    }
    
    public void control(){
    	initiateListeners();
        
        view.getLogOutButton().addActionListener(logOutAL);
        view.getOpenButton().addActionListener(openFileAL);
        view.getDeleteButton().addActionListener(deleteFileAL);
        view.getDeleteLocalButton().addActionListener(deleteOwnedFileAL);
        view.getList().addListSelectionListener(listSL);
    }
    
    private void initiateListeners(){
    	logOutAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                logOut();
            }
    	};
    	openFileAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                openFile();
            }
    	};
    	deleteFileAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                deleteFile();
            }
    	};
    	deleteOwnedFileAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                deleteLocalFile();
            }
    	};
    	listSL = new ListSelectionListener() {
    		@Override
			public void valueChanged(ListSelectionEvent e) {
    			JList<String> list = (JList<String>)e.getSource();
    		    int selected = list.getSelectedIndex();
    		    String sel = list.getSelectedValue();
    			if (e.getValueIsAdjusting() == false) {
    	            if (view.getList().getSelectedIndex() == -1) {
    	            	//System.out.println("No selection");
    	            	
    	            } else {
    	            	//System.out.println("Selected");
    	            	
    	            	view.setListModel(model.getList(), selected);
    	            	selectedFile = sel;
    	            	view.setLabel(selectedFile);
    	            	//view.setDeleteLocalButton(model.isOwnedFile(selectedFile));
    	            	checkButtonVisibility();
    	            }
    	        }
			}
    	};
    }
    
    private void logOut(){
    	//TODO ook als window wordt gesloten
    	model.requestDisconnect();
    	System.exit(0);
    	//System.out.println("Log out");
    }
    
    private void openFile(){
    	//System.out.println("Open file");
    	if(!model.isAvailable(selectedFile)){
    		view.popUp("File is locked. Try again.");
    	} else {
    		String s = model.openFile(selectedFile);
    		if(s!=null){
    			view.popUp(s);
    		}
    	}
    }
    
    private void deleteFile(){ 
    	//System.out.println("Delete file");
    }
    
    private void deleteLocalFile(){ 
    	//System.out.println("Delete owned file");
    }
    
    private void checkButtonVisibility(){
    	if (model.fileOnSystem(selectedFile) && !model.isOwnedFile(selectedFile)){
    		view.setDeleteLocalButton(true);
    	}
    	else{
    		view.setDeleteLocalButton(false);
    	}
    }
}
