/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 *
 * @author bnjmnbrmn
 */
public class BasicCoqEditViewImpl implements BasicCoqEditView {

	//these colors should eventually be set using the CoqEdit.props file
	private Color brightEvaluatedColor = new Color(180, 255, 180);
	private Color brightQueuedColor = new Color(180, 180, 255);
	private Color brightCurrentlyViewingColor = new Color(50, 200, 50);
	private Color brightErrorColor = new Color(255, 80, 80);
	private Color fadedEvaluatedColor = new Color(220, 255, 220);
	private Color fadedQueuedColor = new Color(220, 220, 255);
	private Color fadedCurrentlyViewingColor = new Color(120, 255, 120);
	private Color fadedErrorColor = new Color(255, 120, 120);
	private boolean fadedColors;
	private Color evaluatedColor;
	private Color queuedColor;
	private Color currentlyViewingColor;
	private Color errorColor;
	private Buffer buf;
	
	private List<JEditTextArea> trackedTextAreas = new ArrayList<JEditTextArea>();
	private Map<JEditTextArea, SectionHighlighter> evaluatedHighlighters =
		new HashMap<JEditTextArea, SectionHighlighter>();
	private Map<JEditTextArea, SectionHighlighter> queuedHighlighters =
		new HashMap<JEditTextArea, SectionHighlighter>();
	private Map<JEditTextArea, SectionHighlighter> currentlyViewingHighlighters =
		new HashMap<JEditTextArea, SectionHighlighter>();
	private Map<JEditTextArea, SectionHighlighter> errorHighlighters =
		new HashMap<JEditTextArea, SectionHighlighter>();
	
	private List<View> trackedJEditViews = new ArrayList<View>();
	private Map<View,BasicCoqEditOutputPanel> outputPanels =
		new HashMap<View,BasicCoqEditOutputPanel>();
	
	
	private BasicCoqEditPresenter controller;
	
	public BasicCoqEditViewImpl(BasicCoqEditPresenter controller) {
		this.controller = controller;
		buf = this.controller.getBuffer();
		
		evaluatedColor = brightEvaluatedColor;
		queuedColor = brightQueuedColor;
		currentlyViewingColor = brightCurrentlyViewingColor;
		errorColor = brightErrorColor;
		
		fadedColors = false;
		
		//initialize trackedJEditViews and trackedTextAreas
		for(View view : jEdit.getViews()) {
			if (view.getBuffer() == buf) {
				trackedJEditViews.add(view);
			}
			for (EditPane editPane : view.getEditPanes()) {
				if (editPane.getBuffer() == buf) {
					trackedTextAreas.add(editPane.getTextArea());
				}
			}
		}
		
		//initialize outputPanels for trackedJEditViews
		for (View view : trackedJEditViews) {
			setOutputPanelForView(view);
		}
		
		//initialize highlighters for trackedTextAreas
		for (JEditTextArea textArea : trackedTextAreas) {
			evaluatedHighlighters.put(textArea, new SectionHighlighter(textArea, evaluatedColor));
			queuedHighlighters.put(textArea, new SectionHighlighter(textArea, queuedColor));
			currentlyViewingHighlighters.put(textArea, new SectionHighlighter(textArea, currentlyViewingColor));
			errorHighlighters.put(textArea, new SectionHighlighter(textArea, errorColor));
		}
	}
	
	
	private void setOutputPanelForView(View view) {
		BasicCoqEditOutputPanelHolder outputHolder =
				(BasicCoqEditOutputPanelHolderImpl) view.
				getDockableWindowManager().
				getDockableWindow("coqeditoutput");
		
		if (outputHolder == null) { //sometimes the dockable window seems to get removed completely, making the result of getDockableWindow() null
			view.getDockableWindowManager().addDockableWindow("coqeditoutput");
			
			outputHolder = (BasicCoqEditOutputPanelHolderImpl) view.
					getDockableWindowManager().
					getDockableWindow("coqeditoutput");
		}
		
		BasicCoqEditOutputPanel outputPanel = new BasicCoqEditOutputPanelImpl(buf.getName());
		outputPanels.put(view, outputPanel);
		outputHolder.setOutputPanel(outputPanel);
	}
	
	
	
	@Override
	public void setEvaluatedHighlighting(int startOffset, int endOffset) {
		setHighlighting(evaluatedHighlighters, startOffset, endOffset);
	}

	@Override
	public void setQueuedHighlighting(int startOffset, int endOffset) {
		setHighlighting(queuedHighlighters, startOffset, endOffset);
	}

	@Override
	public void setCurrentlyViewingHighlighting(int startOffset, int endOffset) {
//		Macros.message(jEdit.getActiveView(), "setting currentlyViewingHighlighting"+
//				"\nstartOffset: "+startOffset
//			+"endOffset"+endOffset);
		setHighlighting(currentlyViewingHighlighters, startOffset, endOffset);
	}

	@Override
	public void setErrorHighlighting(int startOffset, int endOffset) {
		setHighlighting(errorHighlighters, startOffset, endOffset);
	}

	protected void setHighlighting(Map<JEditTextArea, SectionHighlighter> highlighters, int startOffset, int endOffset) {
		for (JEditTextArea textArea : trackedTextAreas) {
			highlighters.get(textArea).highlightSection(startOffset, endOffset);
		}
	}

	@Override
	public void setTopOutputText(String text) {
		for (View view : trackedJEditViews) {
			 BasicCoqEditOutputPanel outputPanel = outputPanels.get(view);
			 outputPanel.setTopPaneText(text);
		}
		
	}

	@Override
	public void setBottomOutputText(String text) {
		for (View view : trackedJEditViews) {
			 BasicCoqEditOutputPanel outputPanel = outputPanels.get(view);
			 outputPanel.setBottomPaneText(text);
		}
		
	}


	@Override
	public void addToTrackedJEditViews(View view) {
	//to do?
		BasicCoqEditOutputPanel newOutputPanel = new BasicCoqEditOutputPanelImpl(buf.getName());
		
		outputPanels.put(view, newOutputPanel);
		trackedJEditViews.add(view);
		
		BasicCoqEditOutputPanelHolder outputHolder = 
				(BasicCoqEditOutputPanelHolderImpl) view.
				getDockableWindowManager().
				getDockableWindow("coqeditoutput");
			
			if (outputHolder == null) { //sometimes the dockable window seems to get removed completely, making the result of getDockableWindow() null
				view.getDockableWindowManager().addDockableWindow("coqeditoutput");
				
				outputHolder = (BasicCoqEditOutputPanelHolderImpl) view.
				getDockableWindowManager().
				getDockableWindow("coqeditoutput");
			}
		
		outputHolder.setOutputPanel(newOutputPanel);
	}
	
	
	@Override
	public void removeFromTrackedJEditViews(View view) {
	//to do?
		outputPanels.remove(view);
		trackedJEditViews.remove(view);
	}

	@Override
	public void addToTrackedTextAreas(JEditTextArea textArea) {
		trackedTextAreas.add(textArea);
		
		evaluatedHighlighters.put(textArea, new SectionHighlighter(textArea, evaluatedColor));
		queuedHighlighters.put(textArea, new SectionHighlighter(textArea, queuedColor));
		currentlyViewingHighlighters.put(textArea, new SectionHighlighter(textArea, currentlyViewingColor));
		errorHighlighters.put(textArea, new SectionHighlighter(textArea, errorColor));
	}

	@Override
	public void removeFromTrackedTextAreas(JEditTextArea textArea) {
		trackedTextAreas.remove(textArea);
		
		evaluatedHighlighters.get(textArea).removeFromTextArea();
		queuedHighlighters.get(textArea).removeFromTextArea();
		currentlyViewingHighlighters.get(textArea).removeFromTextArea();
		errorHighlighters.get(textArea).removeFromTextArea();
		
		evaluatedHighlighters.remove(textArea);
		queuedHighlighters.remove(textArea);
		currentlyViewingHighlighters.remove(textArea);
		errorHighlighters.remove(textArea);
	}

	@Override
	public void setHighlightingColorsFaded(boolean fadedColors) {
		if (this.fadedColors != fadedColors) {

			if (fadedColors) {
				evaluatedColor = fadedEvaluatedColor;
				queuedColor = fadedQueuedColor;
				currentlyViewingColor = fadedCurrentlyViewingColor;
				errorColor = fadedErrorColor;
			} else {
				evaluatedColor = brightEvaluatedColor;
				queuedColor = brightQueuedColor;
				currentlyViewingColor = brightCurrentlyViewingColor;
				errorColor = brightErrorColor;
			}


			for (JEditTextArea textArea : trackedTextAreas) {
				evaluatedHighlighters.get(textArea).setHighlightColor(evaluatedColor);
				queuedHighlighters.get(textArea).setHighlightColor(queuedColor);
				currentlyViewingHighlighters.get(textArea).setHighlightColor(currentlyViewingColor);
				errorHighlighters.get(textArea).setHighlightColor(errorColor);
			}
			
			this.fadedColors = fadedColors;
			
		}
	}
	
	@Override
	public Buffer getBuffer() {
		return buf;
	}
	
//	@Override 
//	public void setBuffer(Buffer buf) {
//		this.buf = buf;
//	}

	@Override
	public void removeHighlighters() {
		for (JEditTextArea textArea : trackedTextAreas) {
			evaluatedHighlighters.get(textArea).removeFromTextArea();
			evaluatedHighlighters.remove(textArea);
			
			queuedHighlighters.get(textArea).removeFromTextArea();
			queuedHighlighters.remove(textArea);
			
			currentlyViewingHighlighters.get(textArea).removeFromTextArea();
			currentlyViewingHighlighters.remove(textArea);
			
			errorHighlighters.get(textArea).removeFromTextArea();
			errorHighlighters.remove(textArea);
		}
	}

}
