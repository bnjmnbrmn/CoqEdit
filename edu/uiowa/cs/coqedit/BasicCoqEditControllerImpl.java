package edu.uiowa.cs.coqedit;

import edu.uiowa.cs.coqtopwrapping.CoqtopListener;
import edu.uiowa.cs.coqtopwrapping.CoqtopWrapper;
import edu.uiowa.cs.coqtopwrapping.CoqtopWrapperImpl;
import edu.uiowa.cs.itpwrapping.ITPOutputEvent;
import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.BufferListener;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.TextAreaPainter;

/**
 *
 * @author Benjamin Berman
 */
public class BasicCoqEditControllerImpl implements BasicCoqEditController {

	private Buffer buffer;
	private BasicCoqEditModel coqEditModel;
	private BasicCoqEditView coqEditView;
	private BasicCoqtopRWRunnableImpl rwRunnable;
	private Thread rwThread;
	private BasicCoqtopWrapper coqtopWrapper;

	public BasicCoqEditControllerImpl(Buffer buffer) throws IOException {
		this.buffer = buffer;
		this.coqEditModel = new BasicCoqEditModelImpl(this);
		this.coqEditView = new BasicCoqEditViewImpl(this);
		this.coqtopWrapper = new BasicCoqtopWrapperImpl(this);
		this.rwRunnable = new BasicCoqtopRWRunnableImpl(this, this.coqtopWrapper, this.coqEditModel);
		this.rwThread = new Thread(this.rwRunnable);
		this.rwThread.setDaemon(true);
		this.buffer.addBufferListener(this);
	}

	@Override
	public void startRWThread() {
		rwThread.start();
	}

	@Override
	public void stopRWThread() {
		rwRunnable.setLoopingFalse();
		rwThread.interrupt();
	}

	/**
	 * Moves which sentence is currently being viewed forward by one, or tries
	 * to extend the queued section by a sentence.
	 *
	 * @return -1 when there are no more sentences to evaluate or we are
	 * currently viewing an error sentence.
	 */
	@Override
	public int forwardOneSentence() {
		synchronized (coqEditModel) {

			int errorSN = coqEditModel.getErrorSentenceNumber();
			boolean noErrorSentence = errorSN == -1;
			int lesn = coqEditModel.getLastEvaluatedSentenceNumber(); //last evaluated sentence number
			int oldcvsn = coqEditModel.getCurrentlyViewingSentenceNumber(); //old currently viewing sentence number
			boolean currentlyViewingLastEvaluatedSentence = lesn == oldcvsn;

			//if we are currently viewing the sentence at the end of the evaluated region, and there is no error sentence
			if (currentlyViewingLastEvaluatedSentence && noErrorSentence) {

				//try to extend the queued section
				int nqseo; //new queued section end offset
				try {
					nqseo = coqEditModel.getEndOffset(
							coqEditModel.getLastQueuedSentenceNumber() + 1);
				} catch (NoOffsetForSentenceNumberException ex) {
					//pumsg("Out of sentences to evaluate");
					//pumsg("0");
					return -1;
				}

//				pumsg("last queued sentence number: "+
//						coqEditModel.getLastQueuedSentenceNumber());
//				pumsg("new queued section end offset: "+nqseo);

				//get sentence
				int lqsso; //last queued sentence start offset
				String sentence = "";
				try {
					if (coqEditModel.getLastQueuedSentenceNumber() >= 0) {
						//assign end offset of the old lastQueuedSentenceNumber to lqsso
						lqsso = coqEditModel.getEndOffset(coqEditModel.getLastQueuedSentenceNumber());
					} else {
						lqsso = 0;
					}

					sentence = buffer.getText(lqsso, nqseo - lqsso);
				} catch (NoOffsetForSentenceNumberException ex) {
					pumsg("Could not get the start offset for the last queued sentence"); //this should never be invoked
				}

				//add sentence to coqEditModel's to-evaluate queue--changes last queued sentence number
				coqEditModel.enqueueSentence(sentence);

				//update view
				this.updateCoqEditView();

				//move caret, with electrice scroll
				JEditTextArea textArea = jEdit.getActiveView().getTextArea();
				try {
					textArea.moveCaretPosition(coqEditModel.getEndOffset(
							coqEditModel.getLastQueuedSentenceNumber()), true);
				} catch (NoOffsetForSentenceNumberException ex) {
					//Logger.getLogger(BasicCoqEditControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
				}

				//pumsg("1");
				return 0;

			} else if ((oldcvsn < lesn)
					|| (oldcvsn == lesn && !noErrorSentence)) { //if we are not viewing the sentence at the end of the evaluated region 
				//or there is an error sentence

				coqEditModel.setCurrenlyViewingSentenceNumber(coqEditModel.getCurrentlyViewingSentenceNumber() + 1);

				this.updateCoqEditView();

				//move caret, with electrice scroll
				JEditTextArea textArea = jEdit.getActiveView().getTextArea();
				try {
					textArea.moveCaretPosition(coqEditModel.getEndOffset(
							coqEditModel.getCurrentlyViewingSentenceNumber()), true);
				} catch (NoOffsetForSentenceNumberException ex) {
				}

				//pumsg("2");
				return 0;

			} else {
				//pumsg("3");
				return -1;
			}
		}
	}

	@Override
	public void backOneSentence() {
		synchronized (coqEditModel) {
			if (coqEditModel.getCurrentlyViewingSentenceNumber() > 0) {
				coqEditModel.setCurrenlyViewingSentenceNumber(coqEditModel.getCurrentlyViewingSentenceNumber() - 1);
				this.updateCoqEditView();

				//move caret, with electrice scroll
				JEditTextArea textArea = jEdit.getActiveView().getTextArea();
				try {
					textArea.moveCaretPosition(coqEditModel.getEndOffset(
							coqEditModel.getCurrentlyViewingSentenceNumber()), true);
				} catch (NoOffsetForSentenceNumberException ex) {
				}
			}
		}
	}

	@Override
	public void goToCursor() {
		synchronized (coqEditModel) {
			JEditTextArea textArea = jEdit.getActiveView().getTextArea();
			int caretPosition = textArea.getCaretPosition();
			int caretSection = coqEditModel.getSection(caretPosition);
			int sn = coqEditModel.getSentenceNumberForOffset(caretPosition);

			//to do
			switch (caretSection) {
				case BasicCoqEditModel.EVALUATED_SECTION:
					coqEditModel.setCurrenlyViewingSentenceNumber(sn);
					updateCoqEditView();
					break;
				case BasicCoqEditModel.QUEUED_SECTION_HEAD:
					coqEditModel.removeSentenceQueueTailInclusive(0);
					updateCoqEditView();
					break;
				case BasicCoqEditModel.QUEUED_SECTION_TAIL:
					//dequeue everything after the sn
					int ndlqsn = sn; //new desired last queued sentence number
					int stkiqs = ndlqsn - coqEditModel.getLastEvaluatedSentenceNumber(); //sentences to keep in queued section
					int dqc = coqEditModel.getDequeueCount(); //dequeue count
					coqEditModel.removeSentenceQueueTailInclusive(stkiqs - dqc);
					updateCoqEditView();
					break;
				case BasicCoqEditModel.ERROR_SECTION:
					coqEditModel.setCurrenlyViewingSentenceNumber(sn);
					updateCoqEditView();
					break;
				case BasicCoqEditModel.UNHIGHLIGHTED_SECTION:

					if (sn == -1) { //if we are beyond the last dot-whitespace
						goToEnd();
						return;
					}

					while (coqEditModel.getLastQueuedSentenceNumber() < sn) {
						if (forwardOneSentence() == -1) {
							break;
						}
					}

//				int sn = coqEditModel.getSentenceNumberForOffset(caretPosition);
//				while (forwardOneSentence() != -1) {
//					if (coqEditModel.getLastQueuedSentenceNumber() >= sn)
//						break;
//				}

					//to do

					break;
				default:
					pumsg("Something went wrong while trying to go to the cursor");
					break;
			}
		}

	}

	@Override
	public void goToStart() {
		synchronized (coqEditModel) {
			if (coqEditModel.getLastEvaluatedSentenceNumber() >= 0) {
				coqEditModel.setCurrenlyViewingSentenceNumber(0);
			} else if (coqEditModel.getLastEvaluatedSentenceNumber() < 0) {
				coqEditModel.setCurrenlyViewingSentenceNumber(-1);
			}

			updateCoqEditView();

			//move caret, with electrice scroll
			JEditTextArea textArea = jEdit.getActiveView().getTextArea();
			try {
				textArea.moveCaretPosition(coqEditModel.getEndOffset(
						coqEditModel.getCurrentlyViewingSentenceNumber()), true);
			} catch (NoOffsetForSentenceNumberException ex) {
			}
		}
	}

	@Override
	public void goToEnd() {
		while (forwardOneSentence() != -1) {
			//do nothing
		}
	}

	@Override
	public void interruptEvaluation() {
		//try {
		synchronized (coqEditModel) {

			//clear sentenceQueue (resets last queued sentence number
			coqEditModel.removeSentenceQueueTailInclusive(0);

			//send coqtop process an interrupt
			coqtopWrapper.interruptCoqtopProcess();

			coqEditModel.incrementInterruptCount();

			//interrupt rwThread
//			rwThread.interrupt();

			//notify the rwThread that there is something to do
			coqEditModel.notifyAll();

		}




//				CoqtopResponse response = coqtopWrapper.readFromCoqtop();
//				while (!coqtopWrapper.isInterruptResponse(response)) {
//					response = coqtopWrapper.readFromCoqtop();
//				}





		//update the view (in particular, the highlighting)
		//this.updateCoqEditView(); //this should be done in response to the rwthread
		//} catch (IOException ex) {
		//	pumsg("IOException while getting response from an interrupt");
		//}

	}

	@Override
	public void destroyCoqtopProcess() {
		coqtopWrapper.destroyCoqtopProcess();
	}

	//Should the buffer be set-able?
	@Override
	public void setBuffer(Buffer buf) {
		this.buffer = buf;
	}

	@Override
	public Buffer getBuffer() {
		return this.buffer;
	}

	@Override
	public BasicCoqEditModel getCoqEditModel() {
		return coqEditModel;
	}

	@Override
	public void setCoqEditModel(BasicCoqEditModel model) {
		this.coqEditModel = model;
	}

	@Override
	public void receiveLatestRespose(CoqtopResponse response) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// pop-up message
	void pumsg(String msg) {
		msg = System.currentTimeMillis() / 1000 + "\n" + msg;
		Macros.message(jEdit.getActiveView(), msg);
	}

	@Override
	public void addTrackedTextArea(JEditTextArea textArea) {
		coqEditView.addToTrackedTextAreas(textArea);
	}

	@Override
	public void addTrackedJEditView(View view) {
		coqEditView.addToTrackedJEditViews(view);
	}

	@Override
	public void removeTrackedJEditView(View view) {
		//todo
	}

	@Override
	public void setTopOutputPanelText(String text) {
		coqEditView.setTopOutputText(text);
	}

	@Override
	public void setBottomOutputPanelText(String text) {
		coqEditView.setBottomOutputText(text);
	}

	@Override
	public BasicCoqEditView getCoqEditView() {
		return coqEditView;
	}

	@Override
	public void setCoqEditView(BasicCoqEditView coqEditView) {
		this.coqEditView = coqEditView;
	}

	@Override
	public void updateCoqEditView() {
		synchronized (coqEditModel) {
//			pumsg("currentlyViewingSentenceNumber: "
//					+ coqEditModel.getCurrentlyViewingSentenceNumber()
//					+ "\ncurrentlyViewingSentenceEndOffset:"
//					+ coqEditModel.getCurrentlyViewingSentenceEndOffset()
//					+ "\nlastEvaluatedSentenceNumber: "
//					+ coqEditModel.getLastEvaluatedSentenceNumber()
//					+ "\nevaluatedSectionEndOffset: "
//					+ coqEditModel.getEvaluatedSectionEndOffset());

			//update highlighting

			//update viewingSentenceHighlighting
			int viewingSentenceStartOffset;
			int viewingSentenceEndOffset;
			int viewingSentenceNumber = coqEditModel.getCurrentlyViewingSentenceNumber();
			if (viewingSentenceNumber >= 0) {
				viewingSentenceStartOffset = coqEditModel.getCurrentlyViewingSentenceStartOffset();
				viewingSentenceEndOffset = coqEditModel.getCurrentlyViewingSentenceEndOffset();
			} else {
				viewingSentenceStartOffset = 0;
				viewingSentenceEndOffset = 0;
			}
			coqEditView.setCurrentlyViewingHighlighting(viewingSentenceStartOffset, viewingSentenceEndOffset);

			//update evaluatedSection highlighighting
			int evaluatedSectionStartOffset = 0;
			int evaluatedSectionEndOffset;
			int lastEvaluatedSentenceNumber = coqEditModel.getLastEvaluatedSentenceNumber();
			if (lastEvaluatedSentenceNumber >= 0) { // this check is probably redundant, since we are already checking the viewingSentenceNumber
				evaluatedSectionEndOffset = coqEditModel.getEvaluatedSectionEndOffset();
			} else {
				evaluatedSectionEndOffset = 0;
			}
			coqEditView.setEvaluatedHighlighting(evaluatedSectionStartOffset, evaluatedSectionEndOffset);

			//update queuedSection highlighting
			int queuedSectionStartOffset = evaluatedSectionEndOffset;
			int queuedSectionEndOffset;
			int lastQueuedSentenceNumber = coqEditModel.getLastQueuedSentenceNumber();
			if (lastQueuedSentenceNumber >= 0) {
				queuedSectionEndOffset = coqEditModel.getQueuedSectionEndOffset();
			} else {
				queuedSectionEndOffset = queuedSectionStartOffset;
			}
			coqEditView.setQueuedHighlighting(queuedSectionStartOffset, queuedSectionEndOffset);

			//update errorSentence highlighting
			int errorSentenceStartOffset;
			int errorSentenceEndOffset;
			int errorSentenceNumber = coqEditModel.getErrorSentenceNumber();
			if (errorSentenceNumber >= 0) {
				errorSentenceStartOffset = coqEditModel.getErrorSentenceStartOffset();
				errorSentenceEndOffset = coqEditModel.getErrorSentenceEndOffset();
			} else {
				errorSentenceStartOffset = 0;
				errorSentenceEndOffset = 0;
			}
			coqEditView.setErrorHighlighting(errorSentenceStartOffset, errorSentenceEndOffset);


			if (coqEditModel.getCurrentlyViewingSentenceNumber() >= 0) {

				//update outputPanels
				CoqtopResponse cvResponse;
				try {
					cvResponse =
							coqEditModel.getCachedResponseForSentence(
							coqEditModel.getCurrentlyViewingSentenceNumber());
				} catch (NoCachedSentenceException ex) {
					//for debugging
					pumsg("Something went wrong! There should be a cached\n"
							+ "response for the sentence currently being viewed!"); //this point should never be reached
					return;
				}
				if (coqtopWrapper.isGoalResponse(cvResponse)) {
					coqEditView.setTopOutputText(cvResponse.getMessage());
					coqEditView.setBottomOutputText("");
				} else {
					coqEditView.setBottomOutputText(cvResponse.getMessage());
					if (!coqtopWrapper.isProofModeResponse(cvResponse)) {
						coqEditView.setTopOutputText("");
					} else {
						//to do:
						//search for most recent goal response with matching lemma name stack and print that in the top window
					}
				}
			} else {
				coqEditView.setTopOutputText("");
				coqEditView.setBottomOutputText("");
			}

		}

//		pumsg("\nlast evaluated sentence number: " + lastEvaluatedSentenceNumber +
//				"\nlast queued sentence number: " + lastQueuedSentenceNumber);
	}

	@Override
	public BasicCoqtopWrapper getCoqtopWrapper() {
		return coqtopWrapper;
	}

	@Override
	public Runnable createCoqtopResponseResponder(CoqtopResponse response) {
		return new BasicCoqtopResponseResponder(response);
	}

	protected class BasicCoqtopResponseResponder implements Runnable {

		CoqtopResponse response;

		private BasicCoqtopResponseResponder(CoqtopResponse response) {
			this.response = response;
		}

		@Override
		public void run() {

			synchronized (coqEditModel) {


				if (coqtopWrapper.isNormalNonGoalResponse(response)) {

					//pumsg("Normal non-gloal response");



					int rgsd = response.getPromptInfo().getGlobalStateDepth(); //response global state depth
					int lesnp1sd = coqEditModel.getStateDepthAfterEvaluatingSentenceNumber( //last evaluated sentence number plus 1 state depth
							coqEditModel.getLastEvaluatedSentenceNumber() + 1);
					boolean goingForwardOne = rgsd == lesnp1sd;
					int lesnsd = coqEditModel.getStateDepthAfterEvaluatingSentenceNumber(
							coqEditModel.getLastEvaluatedSentenceNumber());
					boolean goingBackOrNotMoving = rgsd <= lesnsd;


					if (goingForwardOne) { //normal forward-by-one-sentence case

						String msg = response.getMessage();
						coqEditModel.addToResponseCache(response);

						int oldFirstQueuedSentenceNumber =
								coqEditModel.getLastEvaluatedSentenceNumber() + 1;

						coqEditModel.setCurrenlyViewingSentenceNumber(oldFirstQueuedSentenceNumber);
						coqEditModel.setLastEvaluatedSentenceNumber(oldFirstQueuedSentenceNumber);

						coqEditModel.setIsProcessingSentence(false);
						coqEditModel.decrementDequeueCount();
//						pumsg("Went forward one after nongoal response."
//								+ "\nlastEvaluatedSentenceNumber: "+coqEditModel.getLastEvaluatedSentenceNumber());

					} else if (goingBackOrNotMoving) {

						int nlesn = //new last evaluated sentence number
								coqEditModel.getSentenceNumberToEvaluateForStateDepth(rgsd);

						coqEditModel.setLastEvaluatedSentenceNumber(nlesn);
						coqEditModel.setCurrenlyViewingSentenceNumber(nlesn);

						coqEditModel.removeResponsesFromCacheAtIndicesGreaterThanOrEqualTo(nlesn + 1);

						coqEditModel.setIsProcessingSentence(false);
						coqEditModel.setDequeueCount(0);
					}





				} else if (coqtopWrapper.isErrorResponse(response)) {

					//	pumsg("Error response");

					int errorSN = coqEditModel.getLastEvaluatedSentenceNumber() + 1; //error sentence number

					coqEditModel.addToResponseCache(response);

					coqEditModel.setCurrenlyViewingSentenceNumber(errorSN);

					coqEditModel.setErrorSentenceNumber(errorSN);

					coqEditModel.removeSentenceQueueTailInclusive(0);

					coqEditModel.setIsProcessingSentence(false);
					coqEditModel.setDequeueCount(0);



				} else if (coqtopWrapper.isInterruptResponse(response)) {

					coqEditModel.setIsProcessingSentence(false);
					coqEditModel.setDequeueCount(0);

				} else if (coqtopWrapper.isGoalResponse(response)) {

					String msg = response.getMessage();

					int rgsd = response.getPromptInfo().getGlobalStateDepth(); //response global state depth
					int lesnp1sd = coqEditModel.getStateDepthAfterEvaluatingSentenceNumber( //last evaluated sentence number plus 1 state depth
							coqEditModel.getLastEvaluatedSentenceNumber() + 1);
					boolean goingForwardOne = rgsd == lesnp1sd;
					int lesnsd = coqEditModel.getStateDepthAfterEvaluatingSentenceNumber(
							coqEditModel.getLastEvaluatedSentenceNumber());
					boolean goingBackOrNotMoving = rgsd <= lesnsd;

					if (goingForwardOne) {
						coqEditModel.addToResponseCache(response);

						int oldFirstQueuedSentenceNumber =
								coqEditModel.getLastEvaluatedSentenceNumber() + 1;

						coqEditModel.setCurrenlyViewingSentenceNumber(oldFirstQueuedSentenceNumber);
						coqEditModel.setLastEvaluatedSentenceNumber(oldFirstQueuedSentenceNumber);

						coqEditModel.setIsProcessingSentence(false);
						coqEditModel.decrementDequeueCount();

//						pumsg("Went forward one after goal response."
//								+ "\nlastEvaluatedSentenceNumber: "+coqEditModel.getLastEvaluatedSentenceNumber());
					} else if (goingBackOrNotMoving) {
						int nlesn = //new last evaluated sentence number
								coqEditModel.getSentenceNumberToEvaluateForStateDepth(rgsd);

						coqEditModel.setLastEvaluatedSentenceNumber(nlesn);
						coqEditModel.setCurrenlyViewingSentenceNumber(nlesn);

						coqEditModel.removeResponsesFromCacheAtIndicesGreaterThanOrEqualTo(nlesn + 1);

						coqEditModel.setIsProcessingSentence(false);
						coqEditModel.setDequeueCount(0);
					}
//				synchronized (coqEditModel) {
//					coqEditModel.addToResponseCache(response);
//
//					int oldFirstQueuedSentenceNumber =
//							coqEditModel.getLastEvaluatedSentenceNumber() + 1;
//
//					coqEditModel.setCurrenlyViewingSentenceNumber(oldFirstQueuedSentenceNumber);
//					coqEditModel.setLastEvaluatedSentenceNumber(oldFirstQueuedSentenceNumber);
//
//					coqEditModel.setIsProcessingSentence(false);
//				}
				}

				//update view
				BasicCoqEditControllerImpl.this.updateCoqEditView();

			}
		}
	}

	private void contentRemovedOrInserted(int offset) {
		//need to be sure to remember to also retract the last queued sentence
		synchronized (coqEditModel) {


			//coqEditModel.removeEndOfSentenceOffsetsGreaterThan(offset);

			int section = coqEditModel.getSection(offset);
			
			//coqEditModel.removeEndOfSentenceOffsetsGreaterThan(offset);

			if (coqEditModel.getErrorSentenceNumber() != -1) { //if there is an error sentence
				//if currently viewing error sentence, set currently viewing sentence to last evaluated sentence
				if (coqEditModel.getCurrentlyViewingSentenceNumber() == coqEditModel.getErrorSentenceNumber()) {
					coqEditModel.setCurrenlyViewingSentenceNumber(coqEditModel.getLastEvaluatedSentenceNumber());
				}
				//set error sentence to -1
				coqEditModel.setErrorSentenceNumber(-1);
				//remove last cached response
				coqEditModel.removeResponsesFromCacheAtIndicesGreaterThanOrEqualTo(
						coqEditModel.getResponseCacheSize() - 1);
			}
			
			//if within the unevaluated region, do nothing

			//else if within the queued section
			//if within the first queued sentence
			//send interrupt
			//if not within the first queued sentence
			//reduce the sentence queue appropriately & update view

			//else if within the evaluated section
			//interrupt
			//queue up "Back" request; the rw thread will wait for the response
			//	and put the appropritate event on the EDQ

			CoqtopResponse response;

			switch (section) {
				case BasicCoqEditModel.EVALUATED_SECTION:
//					pumsg("Insertion/removal: evaluated section");

					coqEditModel.setProcessingSynchronously(true);

					coqEditModel.setCurrentResponse(null);

					interruptEvaluation();
					while (coqEditModel.getCurrentResponse() == null) {
						try {
//							pumsg("interrupt sent.  waiting...");
							coqEditModel.wait();
						} catch (InterruptedException ex) {
						}
					}
					coqEditModel.setCurrentResponse(null);
					//pumsg("Done waiting for interrupt");

					int ndlesn = coqEditModel.getSentenceNumberForOffset(offset) - 1; //new desired last evaluated sentence number
					
//					pumsg("new desired last evaluated sentence number: "+ndlesn);
					
					int ndsd = coqEditModel.getStateDepthAfterEvaluatingSentenceNumber(ndlesn); //new desired state depth

//					pumsg("new desired state depth: "+ndsd);
					
					String backCommand = coqtopWrapper.getBackCommandForStateDepth(ndsd);
//					pumsg("Back command: " + backCommand);

					coqEditModel.setIsNavigating(true);
					coqEditModel.enqueueSentence(backCommand);

//					pumsg("Back command enqueued: "+backCommand);

//					//???
//					coqEditModel.setProcessingBackCommand(true);

					while ((response = coqEditModel.getCurrentResponse()) == null) {
						try {
							coqEditModel.wait();
						} catch (InterruptedException ex) {
						}
					}

					//pumsg("response acquired");

					createCoqtopResponseResponder(response).run();

					coqEditModel.setIsNavigating(false);
					coqEditModel.setProcessingSynchronously(false);

					break;
				case BasicCoqEditModel.QUEUED_SECTION_HEAD:
//					pumsg("Insertion/removal: queued section head");
					coqEditModel.setProcessingSynchronously(true);


					coqEditModel.setCurrentResponse(null);
					interruptEvaluation();
					while ((response = coqEditModel.getCurrentResponse()) == null) {
						try {
							coqEditModel.wait();
						} catch (InterruptedException ex) {
						}
					}

					createCoqtopResponseResponder(response).run();

					coqEditModel.setCurrentResponse(null);

					coqEditModel.setProcessingSynchronously(false);

					break;
				case BasicCoqEditModel.QUEUED_SECTION_TAIL:
//					pumsg("Insertion/removal: queued section tail");
					int ndlqsn = coqEditModel.getSentenceNumberForOffset(offset) - 1; //new desired last queued sentence number
					int stkiqs = ndlqsn - coqEditModel.getLastEvaluatedSentenceNumber(); //sentences to keep in queued section
//					if (coqEditModel.isProcessingSentence()) {
//						coqEditModel.removeSentenceQueueTailInclusive(stkiqs - 1);
//					} else {
//						coqEditModel.removeSentenceQueueTailInclusive(stkiqs);
//					}
					int dqc = coqEditModel.getDequeueCount(); //dequeue count
					coqEditModel.removeSentenceQueueTailInclusive(stkiqs - dqc);


					break;
				case BasicCoqEditModel.ERROR_SECTION:
//					pumsg("Insertion/removal: error section");

					//do nothing (except for what happens before the switch statement)


					break;
				case BasicCoqEditModel.UNHIGHLIGHTED_SECTION:
//					pumsg("Insertion/removal: unhighlighted section");
					//do nothing (except for what happens before the switch statement)
					break;
				default:
					pumsg("Insertion: something went wrong in determining the section");
					break;
			}

			coqEditModel.removeEndOfSentenceOffsetsGreaterThan(offset);
			
			updateCoqEditView();
		}
	}

	@Override
	public void contentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
//		pumsg("Content inserted at offset "+ offset);
		
		contentRemovedOrInserted(offset);
	}

	@Override
	public void contentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
//		pumsg("Content removed at offset "+ offset);

		contentRemovedOrInserted(offset);
	}

	//methods for BufferListener interface
	@Override
	public void foldLevelChanged(JEditBuffer buffer, int startLine, int endLine) {
	}

	@Override
	public void preContentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
	}

	@Override
	public void preContentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
	}

	@Override
	public void transactionComplete(JEditBuffer buffer) {
	}

	@Override
	public void foldHandlerChanged(JEditBuffer buffer) {
	}

	@Override
	public void bufferLoaded(JEditBuffer buffer) {
	}
}
