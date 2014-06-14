/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.util.List;

/**
 *
 * @author bnjmnbrmn
 */
public class CoqtopPromptInfo {
	private final int globalStateDepth;
	private final int currentProofStateDepth;
	private final List<String> lemmaStack; /* for lemmas/theorems proved 
					 within other lemmas/theorems */
	
	public CoqtopPromptInfo(int globalStateDepth, 
		int currentProofStateDepth, List<String> lemmaStack) {
		
		this.globalStateDepth = globalStateDepth;
		this.currentProofStateDepth = currentProofStateDepth;
		this.lemmaStack = lemmaStack;
	}
	
	public int getGlobalStateDepth() {
		return globalStateDepth;
	}
	
	public int getCurrentProofStateDepth() {
		return currentProofStateDepth;
	}
	
	public List<String> getLemmaStack() {
		return lemmaStack;
	}
	
}
