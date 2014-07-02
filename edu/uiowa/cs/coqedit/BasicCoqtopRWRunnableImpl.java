/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author bnjmnbrmn
 */
public class BasicCoqtopRWRunnableImpl implements Runnable {

	private BasicCoqEditPresenter controller;
	private boolean looping;
	private final BasicCoqtopWrapper coqtopWrapper;
	private final BasicCoqEditModel model;

	public BasicCoqtopRWRunnableImpl(BasicCoqEditPresenter controller,
			BasicCoqtopWrapper coqtopWrapper, BasicCoqEditModel coqEditModel) {
		this.controller = controller;
		this.coqtopWrapper = coqtopWrapper;
		this.model = coqEditModel;
		looping = true;
	}

	/**
	 * Set to false to stop outer loop. To stop the associated thread,
	 */
	public void setLoopingFalse() {
		looping = false;
	}
	
	
	private void waitForSomethingToDo() {
		synchronized(model) {
			while (model.getSentenceQueue().isEmpty() && model.getInterruptCount() == 0) {
				try {
					model.wait();
				} catch (InterruptedException ex) {}
//				debugmsg("!model.getSentenceQueue().isEmpty(): "+ !model.getSentenceQueue().isEmpty()
//						+"\nmodel.getInterruptCount(): "+model.getInterruptCount());
			} 
		}
	}

	@Override
	public void run() {
		try {
			
			while (looping) {
				
				boolean badNavigationDequeue = false;
				
				//wait while there is nothing to do
				debugmsg("Going to wait");
				waitForSomethingToDo();
				
				//if there is a sentence queued up for processing, 
				//send it to coqtop
				String sentence;
				synchronized (model) {
					if (!model.getSentenceQueue().isEmpty()) {
						sentence = model.dequeueSentence();
						//debugmsg("dequeued sentence");
						if (coqtopWrapper.isNavigationSentence(sentence)
								&& !model.isNavigating()) {
//							debugmsg("coqtopWrapper.isNaviagtionSentence(sentence): "
//									+coqtopWrapper.isNavigationSentence(sentence));
//							debugmsg("!model.isNavigating(): "+!model.isNavigating());
							badNavigationDequeue = true;
						} else {
//							debugmsg("coqtopWrapper.isNavigtionSentence(sentence): "
//									+coqtopWrapper.isNavigationSentence(sentence));
//							debugmsg("!model.isNavigating(): "+!model.isNavigating());
							coqtopWrapper.writeSentenceToCoqtop(sentence);
							
						}
						model.setIsProcessingSentence(true);
						model.incrementDequeueCount();
					}
				}

				CoqtopResponse response;

				debugmsg("Going to read");

				if (badNavigationDequeue) {
				//	debugmsg("badNavDequeue");
					synchronized (model) {
						int lesn = model.getLastEvaluatedSentenceNumber();
						CoqtopPromptInfo pi = null;
						try {
							pi = model.getCachedResponseForSentence(lesn).getPromptInfo();
						} catch (NoCachedSentenceException ex) {
							if (lesn == -1) {
								pi = new CoqtopPromptInfo(1, 0, new ArrayList<String>());
							} else {
				//				debugmsg("NoCachedSentenceException not handled");
							}
						}
						String msg = "Error: Command not allowed in CoqEdit";

						response = new CoqtopResponse(msg, pi);
					}
				} else {
				//	debugmsg("good dequeue, going to read");
					//this may block indefinitely
					response = coqtopWrapper.readFromCoqtop();

				}

				//debugmsg("response sd: "+response.getPromptInfo().getGlobalStateDepth());
				//debugmsg("response msg: "+response.getMessage());
				
				//check coqtop interrupt count in the controller's model
				synchronized (model) {
					while (model.getInterruptCount() > 0) {
						if (coqtopWrapper.isInterruptResponse(response)) {
							model.decrementInterruptCount();
							if (model.getInterruptCount() == 0) {
								break;
							}
						}
						response = coqtopWrapper.readFromCoqtop(); //this should not block for long, since there have been interrupts
					}
				}
				
				//debugmsg("Finished decrementing interrupt count");
				
				if (model.processingSynchronously()) { //if the event dispatch thread is waiting for the results
					synchronized (model) {
						debugmsg("Processing synchronously");
						model.setCurrentResponse(response);
						//model.setIsProcessingSentence(false);
						model.notifyAll();
					}
				} else {
					synchronized(model) {
						debugmsg("Processing asynchronously");
						Runnable responseResponder = model.getCoqtopResponseResponderFactory().createCoqtopResponseResponder(response);
						SwingUtilities.invokeLater(responseResponder);
						//SwingUtilities.invokeLater(controller.createCoqtopResponseResponder(response));
//						model.setIsProcessingSentence(false);
					}
				}



			}
			
		} catch (IOException ex) {
		}
	}

//	@Override
//	public void run() {
//		try {
//
//			while (looping) {
//				try {
//					boolean interruptToRead = false;
//					synchronized (model) {
//						if (model.getInterruptCount() > 0) {
//							interruptToRead = true;
//						}
//					}
//
//					String sentence;
//					CoqtopResponse response;
//
//					if (!interruptToRead) {
//						//get sentence from sentenceQueue in controller's model
//						//	--this should block when there are no sentences in the queue
//						synchronized (model) {
//							while (model.getSentenceQueue().isEmpty()) {
//								wait();
//							}
//							sentence = model.dequeueSentence();
//
//							//write sentence to coqtop
//							coqtopWrapper.writeSentenceToCoqtop(sentence);
//
//							model.setIsProcessingSentence(true);
//
//						}
//					}
//					//read from coqtop
//					// --method may block indefinitely
//					response = coqtopWrapper.readFromCoqtop();
//
//					
//					//check coqtop interrupt count in the controller's model
//					synchronized (model) {
//						while (model.getInterruptCount() > 0) {
//							if (coqtopWrapper.isInterruptResponse(response)) {
//								model.decrementInterruptCount();
//								if (model.getInterruptCount() == 0) {
//									break;
//								}
//							}
//							response = coqtopWrapper.readFromCoqtop(); //this should not block for long, since there have been interrupts
//						}
//					}
//	
//					
//					boolean processingBackCommand = false;
//					synchronized (model) {
//						if (model.processingBackCommand()) {
//							processingBackCommand = true;
//						}
//						if (processingBackCommand) {
//							controller.createCoqtopResponseResponder(response).run();
//							model.setProcessingBackCommand(false);
//							model.notifyAll();
//						}
//					}
//					if (!processingBackCommand) {
//						//put a new event on the EventQueue that invokes receiveLatestRespose
//						SwingUtilities.invokeAndWait(controller.createCoqtopResponseResponder(response));
//					}
//					
//
//				} catch (InterruptedException ex) {
//				} catch (IOException ex) {
//				}
//			}
//
//		} catch (InvocationTargetException ex) {
//		
//		}
//
//
//	}
	
	public static void debugmsg(String msg) {
		PrintWriter out = null;
		
		Date d = new Date(System.currentTimeMillis());
	
		
		try {
			out = new PrintWriter(new FileWriter("/Users/bnjmnbrmn/Desktop/debug.txt",true));
			out.println("\n************************************************");
			out.println(d);
			out.println(msg);
			out.println("************************************************");
			out.flush();
		} catch (IOException ex) {
			
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
}
