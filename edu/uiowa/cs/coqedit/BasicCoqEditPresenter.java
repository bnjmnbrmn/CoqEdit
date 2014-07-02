/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.awt.PopupMenu;
import java.util.List;
import javax.swing.JPanel;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.BufferListener;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextArea;

/**
 *
 * @author bnjmnbrmn
 */
public interface BasicCoqEditPresenter extends BufferListener, CoqtopResponseResponderFactory {
	public int forwardOneSentence();
	public void backOneSentence();
	public void goToCursor();
	public void goToStart();
	public void goToEnd();
	public void interruptEvaluation();
	
	
	public void setBuffer(Buffer buf);
	public Buffer getBuffer();
	public void destroyCoqtopProcess();
	public BasicCoqEditModel getCoqEditModel();
	public void setCoqEditModel(BasicCoqEditModel coqEditModel);
	public BasicCoqEditView getCoqEditView();
	public void setCoqEditView(BasicCoqEditView coqEditView);
	
	
	
	public void receiveLatestRespose(CoqtopResponse response);

	
	public void updateCoqEditView();
	
	//public List<TextArea> getTextAreas();
	
	
	public void addTrackedTextArea(JEditTextArea textArea);
	
	
	//public void setTextAreas(List<TextArea> textAreas);
	
	
	
	//public List<View> getViews();
	
	//public void setViews(List<View> views);
	
	public void addTrackedJEditView(View view);
	public void removeTrackedJEditView(View view);
	

	public BasicCoqtopWrapper getCoqtopWrapper();
	
	//public void addOutputPanel();
	
	//public void removeOutputPanel();
	
	public void setTopOutputPanelText(String text);
	
	public void setBottomOutputPanelText(String text);

	public void startRWThread();
	
	public void stopRWThread();
	
	//public Runnable createCoqtopResponseResponder(CoqtopResponse response);

}
