package be.uantwerpen.ds.system_y.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The class that connects the view with the model
 *
 */
public class Controller {
	private Model model;
    private View view;
    private ActionListener logOutAL, openFileAL, deleteFileAL, deleteLocalFileAL;
    private WindowListener windowCL;
    private ListSelectionListener listSL;
    private String selectedFile;
    
    public Controller(Model model, View view){
        this.model = model;
        this.view = view;
        this.view.setListModel(model.getList());
    }
    
    /**
     * Link the correct listeners to the right view components
     */
    public void control(){
    	initiateListeners();
        
        view.getLogOutButton().addActionListener(logOutAL);
        view.getOpenButton().addActionListener(openFileAL);
        view.getDeleteButton().addActionListener(deleteFileAL);
        view.getDeleteLocalButton().addActionListener(deleteLocalFileAL);
        view.getList().addListSelectionListener(listSL);
        view.getFrame().addWindowListener(windowCL);
    }
    
    /**
     * Initiation of the button, window, list selection listeners, what to do when it's activated
     */
    private void initiateListeners(){
    	logOutAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	if (JOptionPane.showConfirmDialog(view.getFrame(), 
        	            "Are you sure to close this window?", "Leaving System Y", 
        	            JOptionPane.YES_NO_OPTION,
        	            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
        	        
            		model.logOut();
            		System.exit(0);
        	    }
            }
    	};
    	openFileAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	String s = model.openFile(selectedFile);
        		if(s!=null){
        			view.popUp(s);
            	}
            }
    	};
    	deleteFileAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	String s = model.deleteNetworkFile(selectedFile);
        		if(s!=null){
        			view.popUp(s);
            	}
            }
    	};
    	deleteLocalFileAL = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	String s = model.deleteLocalFile(selectedFile);
        		if(s!=null){
        			view.popUp(s);
            	}
            }
    	};
    	listSL = new ListSelectionListener() {
    		@Override
			public void valueChanged(ListSelectionEvent e) {
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>)e.getSource();
    		    String sel = list.getSelectedValue();
    			if (e.getValueIsAdjusting() == false) {
    	            if (view.getList().getSelectedIndex() == -1) {
    	            	//System.out.println("No selection");
    	            	
    	            } else {
    	            	//System.out.println("Selected");
    	            	
    	            	selectedFile = sel;
    	            	view.setLabel(selectedFile);
    	            	view.setDeleteLocalButton(model.changeDeleteLocalBtn(selectedFile));
    	            }
    	        }
			}
    	};
    	
    	windowCL = new WindowAdapter() {
    	    @Override
    	    public void windowClosing(WindowEvent windowEvent) {
	    		model.logOut();
	    		System.exit(0);
    	    }
    	};
    }
    
    /**
     * To update the list
     */
    public void updateView(){
    	view.setListModel(model.getList());
    }
}
