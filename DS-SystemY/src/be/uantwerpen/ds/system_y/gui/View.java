package be.uantwerpen.ds.system_y.gui;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.html.ListView;

import com.sun.xml.internal.ws.org.objectweb.asm.Label;

public class View {
	private JFrame frame;
    private JButton logOutBtn, openBtn, deleteBtn, deleteLocalBtn;
    private JToolBar topTB, bottomTB;
    private JList<String> list;
    private DefaultListModel<String> listmodel;
    private String[] files = {"een", "twee", "drie"};
    private JLabel label;
    
    public View(){
        frame = new JFrame("System Y");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(400, 400, 400, 200);
        frame.setVisible(true);
        
        //buttons
        logOutBtn = new JButton("Log out");
        openBtn = new JButton("Open file");
        deleteBtn = new JButton("Delete file");
        deleteLocalBtn = new JButton("Delete local file");
        
        //list
        listmodel = new DefaultListModel<String>();
        list = new JList<String>(listmodel);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        
        //label
        label = new JLabel("File...");
        
        //toolbars
        topTB = new JToolBar("Top");
        bottomTB = new JToolBar("Bottom");
        
        //add components to frame
        frame.getContentPane().add(topTB, BorderLayout.PAGE_START);
        frame.getContentPane().add(list, BorderLayout.CENTER);
        frame.getContentPane().add(new JScrollPane(list));
        frame.getContentPane().add(bottomTB, BorderLayout.PAGE_END);
        
        //add components to toolbars
        topTB.add(logOutBtn);
        bottomTB.add(openBtn);
        bottomTB.add(deleteBtn);
        bottomTB.add(deleteLocalBtn);
        bottomTB.add(label);
    }
        
    public JButton getLogOutButton(){
        return this.logOutBtn;
    }
    
    public JButton getOpenButton(){
        return this.openBtn;
    }
    
    public JButton getDeleteButton(){
        return this.deleteBtn;
    }
    
    public JButton getDeleteLocalButton(){
        return this.deleteLocalBtn;
    }
    
    public void setDeleteLocalButton(boolean enabled){
    	this.deleteLocalBtn.setEnabled(enabled);
    }
    
    public void setListModel(DefaultListModel<String> newmodel, int index){
    	this.listmodel = newmodel;
    	list.setModel(listmodel);
    	//list.setSelectedIndex(index-1);
    	//list.ensureIndexIsVisible(index);
    }
    
    public JList<String> getList(){
    	return this.list;
    }
    
    public void setLabel(String filename){
    	label.setText(filename);
    }
    
    public void popUp(String message){
    	JPanel popupPanel = new JPanel(new GridLayout(0,1));
    	popupPanel.add(new JLabel("Message:"));
    	popupPanel.add(new JLabel(message));
    	JOptionPane.showMessageDialog(null, popupPanel);
    }
}
