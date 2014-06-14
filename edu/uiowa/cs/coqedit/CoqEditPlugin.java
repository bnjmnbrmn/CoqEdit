package edu.uiowa.cs.coqedit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.BufferChanging;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextArea;

/**
 *
 * @author Benjamin Berman
 */
public class CoqEditPlugin extends EBPlugin {

	public static String NAME = "CoqEdit";
	public static String AUTHOR = "Benjamin Berman and Harley Eades";
	public static String VERSION = "0.0.1";
	public static String OPTION_PREFIX = "options.coqedit.";
	private static CoqEditPlugin INSTANCE;

	private List<BasicCoqEditController> coqEditControllers = 
		new ArrayList<BasicCoqEditController>();
	private BasicCoqEditController currentCoqEditController;
	
	private Map<View,BasicCoqEditOutputPanelHolder> outputPanelHolders = new HashMap<View,BasicCoqEditOutputPanelHolder>();
	
	@Override
	public void start() {
		INSTANCE = this;
		
		for(View view : jEdit.getViews()) {
			DockableWindowManager dwm = view.getDockableWindowManager();
			dwm.addDockableWindow("coqeditoutput");
		}
		
	}
	
	@Override
	public void stop() {
		//to do
		
		for (BasicCoqEditController coqEditController : coqEditControllers) {
			coqEditController.getCoqEditView().removeHighlighters(); //remove sentenceHighlighters
			
			coqEditController.getBuffer().removeBufferListener(coqEditController);//remove bufferListener
			
			coqEditController.stopRWThread();
			
			coqEditController.destroyCoqtopProcess();
		}
	}

	public static CoqEditPlugin getInstance() {
		return INSTANCE;
	}
	
	private void createAndStartControllerIfNecessary() {
		if (currentCoqEditController == null) { 
			try {
				Buffer currentBuffer = jEdit.getActiveView().getBuffer();
				currentCoqEditController =
					new BasicCoqEditControllerImpl(currentBuffer);
				currentCoqEditController.startRWThread();
				coqEditControllers.add(currentCoqEditController);
			} catch (IOException ex) {
				pumsg("IOException while creating controller");
			}
		}
	}

	public void forwardOneSentence() {
		
		createAndStartControllerIfNecessary();
		
		//go forward one sentence for the current controller/buffer
		currentCoqEditController.forwardOneSentence();
		
	}
	
	public void backOneSentence() {
		if (currentCoqEditController != null) {
			currentCoqEditController.backOneSentence();
		}
	}

	public void goToCursor() {
		createAndStartControllerIfNecessary();
		
		currentCoqEditController.goToCursor();
		
	}

	public void goToStart() {
		if (currentCoqEditController != null) {
			currentCoqEditController.goToStart();
		}
	}

	public void goToEnd() {

		createAndStartControllerIfNecessary();

		currentCoqEditController.goToEnd();

	}

	public void interruptEvaluation() {
		if (currentCoqEditController != null) {
			currentCoqEditController.interruptEvaluation();
		}
	}
	
	@Override
	public void handleMessage(EBMessage msg) {
		if (msg instanceof EditPaneUpdate) {
			handleEditPaneUpdate((EditPaneUpdate) msg);
		} 
		if (msg instanceof ViewUpdate) {
			handleViewUpdate((ViewUpdate) msg);
		} 
		if (msg instanceof BufferChanging) {
			handleBufferChanging((BufferChanging) msg);
		}
	}
	
	private void handleEditPaneUpdate(EditPaneUpdate msg) {
		Object what = msg.getWhat();
		
		if (what == EditPaneUpdate.BUFFER_CHANGED) {
			pumsg("EditPaneUpdate.BUFFER_CHANGED");
			handleBufferChanged();
		} else if (what == EditPaneUpdate.BUFFER_CHANGING) {
			pumsg("EditPaneUpdate.BUFFER_CHANGING");
		} else if (what == EditPaneUpdate.CREATED) {
			pumsg("EditPaneUpdate.CREATED");  //???this case covered by ViewUpdate.EDIT_PANE_CHANGED (in handleViewUpdate())
		} else if (what == EditPaneUpdate.DESTROYED) {
			pumsg("EditPaneUpdate.DESTROYED");
		}
	}
	
	private void handleViewUpdate(ViewUpdate msg) {
		Object what = msg.getWhat();
		
		if (what == ViewUpdate.CREATED) {
			pumsg("ViewUpdate.CREATED");
		} else if (what == ViewUpdate.ACTIVATED) {
		//	pumsg("ViewUpdate.ACTIVATED");
		} else if (what == ViewUpdate.CLOSED) {
			pumsg("ViewUpdate.CLOSED");
		} else if (what == ViewUpdate.EDIT_PANE_CHANGED) {
			pumsg("ViewUpdate.EDIT_PANE_CHANGED");
		}
	}
	
	private void handleBufferChanging(BufferChanging msg) {
		Object what = msg.getWhat();
		
		if (what == BufferChanging.BUFFERSET_CHANGED) {
			pumsg("BufferChanging.BUFFERSET_CHANGED");
		} else if (what == BufferChanging.BUFFER_CHANGED) {
			pumsg("BufferChanging.BUFFER_CHANGED");
		} else if (what == BufferChanging.BUFFER_CHANGING) {
			pumsg("BufferChanging.BUFFER_CHANGING");
		} else if (what == BufferChanging.CREATED) {
			pumsg("BufferChanging.CREATED");
		} else if (what == BufferChanging.DESTROYED) {
			pumsg("BufferChanging.DESTROYED");
		}
	}
	
	//
	private void handleBufferChanged() {
		JEditTextArea textArea = jEdit.getActiveView().getEditPane().getTextArea(); //
		
		//If we were evaluating the old buffer
		if (currentCoqEditController != null) { 
			BasicCoqEditView oldCoqEditView = currentCoqEditController.getCoqEditView();
			oldCoqEditView.removeFromTrackedTextAreas(textArea); //remove textArea from the set that gets highlighted by the old CoqEditView
			oldCoqEditView.setHighlightingColorsFaded(true); //fade highlighting colors for the remaining textAreas being tracked
			oldCoqEditView.removeFromTrackedJEditViews(jEdit.getActiveView()); //remove active view from the set the set of views with dockables that get updated by the old CoqEditView
			
		} 
		
		//Regardless of whether or not we were evaluating the old buffer
		currentCoqEditController = getProofController(jEdit.getActiveView().getBuffer()); //update the currentProofController (possibly to null)
		if (currentCoqEditController != null) { //if we had been evaluating the new buffer
			pumsg("currentCoqEditController not null");//for debugging
			
			
			BasicCoqEditView newCoqEditView = currentCoqEditController.getCoqEditView();
			newCoqEditView.addToTrackedTextAreas(textArea);//add the textArea to the set being highlighted by the new CoqEditView
			newCoqEditView.setHighlightingColorsFaded(false);//brighten highlighting colors for text areas tracked by the new CoqEditView
			newCoqEditView.addToTrackedJEditViews(jEdit.getActiveView());//add the view to the set being tracked by the new CoqEditView
			
			currentCoqEditController.updateCoqEditView();
			
		} else { //for debugging
			pumsg("currentCoqEditController updated to null");
		}
		
	}
	
	private BasicCoqEditController getProofController(Buffer buffer) {
		for (BasicCoqEditController proofController : coqEditControllers) {
			if (proofController.getBuffer().equals(buffer))
				return proofController;
		}
		return null;
	}
	
	// pop-up message
	private void pumsg(String msg) {
		Macros.message(jEdit.getActiveView(),msg);
	}

//	private void switchEvaluationBuffer(BasicCoqEditController pc) {
//		currentProofController = pc;
//		
//		
//		//add outputPanels to dockables, as appropriate
////		List<BasicCoqEditOutputPanel> outputPanels =
////			currentProofController.getOutputPanels();
//		View activeView = jEdit.getActiveView();
//		int i = 0;
//		for (View view : jEdit.getViews()) {
//			if (view.getBuffer() == activeView.getBuffer()) {
//				JPanel dockable = (JPanel) view.
//					getDockableWindowManager().
//					getDockableWindow("coqeditoutput");
//				dockable.removeAll();
//				dockable.add(
//					outputPanels.get(i));
//				i++;
//			}
//		}
//		
//	}

	
	
	public BasicCoqEditOutputPanelHolder getOutputHolder(View view) {
		return outputPanelHolders.get(view);
	}

	void addOutputPanelHolder(View view, BasicCoqEditOutputPanelHolder outputHolder) {
		outputPanelHolders.put(view, outputHolder);
	}

	public BasicCoqEditController getCurrentController() {
		return currentCoqEditController;
	}


}
