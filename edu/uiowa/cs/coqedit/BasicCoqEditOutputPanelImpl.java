package edu.uiowa.cs.coqedit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import org.gjt.sp.jedit.View;

/**
 *
 * @author Harley Eades and Benjamin Berman
 */
public class BasicCoqEditOutputPanelImpl extends BasicCoqEditOutputPanel {
	private JEditorPane topEditorPane;
	private JEditorPane bottomEditorPane;

	private JScrollPane bottomScrollPane;
	private JScrollPane topScrollPane;
			
	private JSplitPane splitPane;
	
	private static final int DEFAULT_TOP_EDITORPANE_WIDTH = 400; 
	private static final int DEFAULT_TOP_EDITORPANE_HEIGHT = 400;
	private static final int DEFAULT_BOTTOM_EDITORPANE_WIDTH = 400;
	private static final int DEFAULT_BOTTOM_EDITORPANE_HEIGHT = 400;
	
	private String bufferName;
	
	private BasicCoqEditOutputPanelImpl() {
		super(new BorderLayout());
		
		topEditorPane = new JEditorPane();
		bottomEditorPane = new JEditorPane();
		topEditorPane.setPreferredSize(
			new Dimension(DEFAULT_TOP_EDITORPANE_WIDTH, 
				DEFAULT_TOP_EDITORPANE_HEIGHT));
		
		topScrollPane = new JScrollPane(topEditorPane);
		bottomScrollPane = new JScrollPane(bottomEditorPane);
		
		topEditorPane.setText("");
		bottomEditorPane.setText("");
		
		topEditorPane.setEditable(false);
		bottomEditorPane.setEditable(false);
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		splitPane.setTopComponent(topScrollPane);
		splitPane.setBottomComponent(bottomScrollPane);
		
		
		this.add(splitPane,BorderLayout.CENTER);
		
		this.setVisible(true);	
		
	}
	
	public BasicCoqEditOutputPanelImpl(String bufferName) {
		this();
		
		this.bufferName = bufferName;
		
		this.add(new JLabel(bufferName),BorderLayout.NORTH);
	}
	
//	public BasicCoqEditOutputPanelImpl(View view, String position, String bufferName) {
//		this();
//		
//		this.bufferName = bufferName;
//	}
	
	public void setTopPaneText(String text) {
		topEditorPane.setText(text);
	}
	
	public void setBottomPaneText(String text) {
		bottomEditorPane.setText(text);
	}

	@Override
	public void setBufferName(String bufferName) {
		this.bufferName = bufferName;
	}
	
}
