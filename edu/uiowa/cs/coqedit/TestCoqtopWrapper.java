/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import edu.uiowa.cs.coqtopwrapping.CoqtopWrapper;
import edu.uiowa.cs.itpwrapping.ITPListener;
import edu.uiowa.cs.itpwrapping.ITPOutputEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author bnjmnbrmn
 */
public class TestCoqtopWrapper implements CoqtopWrapper {
	private List<ITPListener> listeners;
	private boolean testCoqtopRWThreadLoopFlag;
	private List<String> steList;
	private final TestCoqtopRWRunnable testCoqtopRWRunnable;
	private final Thread testCoqtopRWThread;
	
	private int current_state_depth = 1;
	
	
	//Probably a more elegant way of doing this...
	private volatile boolean currently_processing_a_sentence = false;
	private final Object CPASLock = new Object();

	private final Object STEListLock = new Object();
	
	public TestCoqtopWrapper(String unusedCoqtopPath) {
		this.listeners = Collections.synchronizedList(
			new LinkedList<ITPListener>());
		
		// Allocate a collection to store our coptop commands.
		steList = Collections.synchronizedList(
			new LinkedList<String>());
		
		testCoqtopRWRunnable = this.new TestCoqtopRWRunnable();
		testCoqtopRWThread = new Thread(testCoqtopRWRunnable);
		testCoqtopRWThreadLoopFlag = true;
		testCoqtopRWThread.start();
	}
	
	@Override
	public void back(int state_depth) {
	}
	
	@Override
	public int getCurrentITPState() {
		return -1;
	}
	
	/* 
	 * 
	 * If the steList is not empty
	 *    remove the most recent sentence that was added to the list,
	 * else	if coqtop is currently processing a sentence
	 *         interrupt coqtop,
	 *      else if the state depth is greater than 1
	 *              send coqtop: "BackTo " + (current_state_depth - 1)
	 *           else
	 *		(do nothing).
	 * 
	 * 
	 */
	public void backOneSentence() {
		synchronized(STEListLock) {
			if (!steList.isEmpty()) {
				steList.remove(steList.size()-1);
			}
		}
		
	}

	@Override
	public void registerListener(ITPListener listener) {
		listeners.add(listener);
	}

	@Override
	public void sendToITP(String msg, int state_depth) {
		if (this.addToSTEList(msg) == -1) {
			ITPOutputEvent error = new ITPOutputEvent(
				"Unsupported Command: " + msg, current_state_depth);
			notifyListeners(error, true);
		}
	}

	@Override
	public void shutdownITP() {
		testCoqtopRWThreadLoopFlag = false;
	}

	/*
	 * Clear steList.
	 * Send coqtop process a ctrl-c
	 *   --will simulate this with an interrupt of the testCoqtopRWThread
	 * 
	 */
	@Override
	public void interruptCurrentCommand() {
		steList.clear();
		testCoqtopRWThread.interrupt();
		
	}
	
	private void notifyListeners(ITPOutputEvent event, boolean error) {
		for (int i = 0; i < this.listeners.size(); i++) {
			if (error) {
				this.listeners.get(i)
					.errorEventReceived(event);
			} else {
				this.listeners.get(i)
					.standardEventReceived(event);
			}
		}
	}

	private synchronized int addToSTEList(String sentence) {
		// Guards against the user trying to use these commands themselves.
		if (sentence.equalsIgnoreCase("undo.")
			|| sentence.equalsIgnoreCase("back.")
			|| sentence.equalsIgnoreCase("quit.")
			|| sentence.startsWith("Reset ")) {
			return -1;
		}

		sentence = changeNewlinesToSpaces(sentence);
		steList.add(steList.size(), sentence);
		//coqtopRWThread.interrupt();

		return 0;
	}
	
	private String changeNewlinesToSpaces(String sentence) {
		return sentence.replaceAll("\n", " ");
	}

	
	private class TestCoqtopRWRunnable implements Runnable {
		String msg;
		boolean msgRead = false;
		
		@Override
		public void run() {
			View view = jEdit.getActiveView();
			
			while (testCoqtopRWThreadLoopFlag) {
				try {

					String sentence;
					sentence = null;
					
					
					
					synchronized(STEListLock) {
						if (!steList.isEmpty()) {
							sentence = steList.get(0);
							
						}
					}
					
					if (sentence != null) { //If we were able to get a sentence from the steList
						//Start of sentence processing
						synchronized(CPASLock) {
							currently_processing_a_sentence = true;
						}
						
						testSendToCoq(sentence);
						String output = testReadFromCoq();
						notifyListeners(
							new ITPOutputEvent(
								output,
							        current_state_depth),
							false);
						synchronized(STEListLock) {
							steList.remove(0);
						}
						
						synchronized(CPASLock) {
							currently_processing_a_sentence = false;
						}
					}
					
					
					
				} catch (InterruptedException ex) {
					
					//this would not be included in the actual wrapper implementation...?
					msg = "\"Coqtop\" interrupted!";
					notifyListeners(new ITPOutputEvent(msg, current_state_depth), true);
					synchronized(CPASLock) {
						currently_processing_a_sentence = false;
					}
				}
			}
		}

		private void testSendToCoq(String msg) {
			this.msg = msg;
			msgRead = true;
		}
		
		private String testReadFromCoq() throws InterruptedException {
			Thread.sleep(1500);
			
			msg = msg+" was sent"
				+ " to \"Coq\".";
			msgRead = false;
			
			current_state_depth++;
			
			return msg;
		}
		
	}
}
