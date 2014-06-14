/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.io.IOException;

/**
 *
 * @author bnjmnbrmn
 */
public interface BasicCoqtopWrapper {
	public void writeSentenceToCoqtop(String sentence);
	
	public CoqtopResponse readFromCoqtop() throws IOException;
	
	//these methods should probably be static...
	//also, CoqtopResponse and CoqtopPromptInfo should perhaps be static nested classes
	public boolean isInterruptResponse(CoqtopResponse response);
	public boolean isNormalNonGoalResponse(CoqtopResponse response);
	public boolean isGoalResponse(CoqtopResponse response);
	public boolean isErrorResponse(CoqtopResponse response);
	public boolean isProofModeResponse(CoqtopResponse cvResponse);
	public String getBackCommandForStateDepth(int stateDepth); //returns the message to enqueue for going back to a particular state depth
	
	public void interruptCoqtopProcess();
	public void destroyCoqtopProcess();
	
	public Process getCoqtopProcess();
	
	public void setCoqtopProcess(Process coqtopProc);
	
	public boolean isNavigationSentence(String sentence);

	
	
}
