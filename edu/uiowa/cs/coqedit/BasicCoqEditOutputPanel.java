/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.awt.LayoutManager;
import javax.swing.JPanel;

/**
 *
 * @author bnjmnbrmn
 */
public abstract class BasicCoqEditOutputPanel extends JPanel {
	public abstract void setTopPaneText(String text);
	
	public abstract void setBottomPaneText(String text);
	
	public abstract void setBufferName(String text);
	
	public BasicCoqEditOutputPanel() {
		super();
	}
	
	public BasicCoqEditOutputPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}
	
	public BasicCoqEditOutputPanel(LayoutManager layoutManager) {
		super(layoutManager);
	}
	
	public BasicCoqEditOutputPanel(LayoutManager layoutManager, 
			boolean isDoubleBuffered) {
		super(layoutManager,isDoubleBuffered);
	}
}


