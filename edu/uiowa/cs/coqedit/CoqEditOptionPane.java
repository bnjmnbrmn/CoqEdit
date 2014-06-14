package edu.uiowa.cs.coqedit;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author bnjmnbrmn
 */
public class CoqEditOptionPane 
  extends AbstractOptionPane
  /* implements ActionListener */ {
	
	private JTextField pathField;
	private String path;
	
	public CoqEditOptionPane() {
		super(CoqEditPlugin.NAME);
	}
	
	@Override
	public void _init() {
		path = jEdit.getProperty("options.coqedit.coqtoppath");
		pathField = new JTextField(path);
		
		JPanel pathPanel = new JPanel(new BorderLayout(0, 0));
		pathPanel.add(pathField, BorderLayout.CENTER);
		
		addComponent("Coqtop Path", pathPanel);
	}
	
	@Override
	public void _save() {
		path = pathField.getText();
		
		if (!path.equals(jEdit.getProperty("options.coqedit.coqtoppath"))){
			jEdit.setProperty("options.coqedit.coqtoppath", path);
			//TO DO: restart wrappers for buffers
		}
	}

	/*
	@Override
	public void actionPerformed(ActionEvent ae) {
		throw new UnsupportedOperationException("Not supported yet.");
	}*/
}
