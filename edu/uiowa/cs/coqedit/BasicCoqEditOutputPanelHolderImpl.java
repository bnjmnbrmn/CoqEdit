/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;

/**
 *
 * @author bnjmnbrmn
 */
public class BasicCoqEditOutputPanelHolderImpl extends JPanel implements BasicCoqEditOutputPanelHolder{
	BasicCoqEditOutputPanel outputPanel = null;
	
	public BasicCoqEditOutputPanelHolderImpl(View view) {
		super(new BorderLayout(), true);
		
		CoqEditPlugin cep = CoqEditPlugin.getInstance();
		
		DockableWindowManager dwm = view.getDockableWindowManager();
		
		if (dwm.getDockable("coqeditoutput") != null) {
			JLabel jlab = new JLabel("Cloned windows not allowed for this plugin");
			this.add(jlab, BorderLayout.CENTER);
			return;
		}
		
		cep.addOutputPanelHolder(view, this); //might be able to put this into the beanshell code to avoid "Leaking this in constructor" 
		//(this warning might not matter, since it is the last statement in the constructor)
	}

	@Override
	public void setOutputPanel(BasicCoqEditOutputPanel outputPanel) {
		if (this.outputPanel != null) {
			this.remove(this.outputPanel);
		}
		this.outputPanel = outputPanel;
		this.add(outputPanel, BorderLayout.CENTER);
	}
}
