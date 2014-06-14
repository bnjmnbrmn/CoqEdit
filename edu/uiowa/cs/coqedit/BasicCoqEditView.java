/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.util.List;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 *
 * @author bnjmnbrmn
 */
public interface BasicCoqEditView {
	//public void addHighlightersToTextAreas(List<JEditTextArea> textAreas);
	
	//public void removeHighlightersFromTextAreas();
	
	public void setEvaluatedHighlighting(int startOffset, int endOffset);
	
	public void setQueuedHighlighting(int startOffset, int endOffset);
	
	public void setCurrentlyViewingHighlighting(int startOffset, int endOffset);
	
	public void setErrorHighlighting(int startOffset, int endOffset);
	
	public void setTopOutputText(String text);
	
	public void setBottomOutputText(String text);
	
	//public List<BasicCoqEditOutputPanel> getOutputPanels();

	//public void addOutputPanel();

	//public void removeOutputPanel();

	public void addToTrackedJEditViews(View view);
	
	public void removeFromTrackedJEditViews(View view);
	
	public void addToTrackedTextAreas(JEditTextArea textArea);

	public void removeFromTrackedTextAreas(JEditTextArea textArea);
	
	public void setHighlightingColorsFaded(boolean fade);
	
	public Buffer getBuffer();
	
	
	
	//public void setBuffer(Buffer buf);

	public void removeHighlighters();
	
}
