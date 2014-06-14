/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

/**
 *
 * @author bnjmnbrmn
 */
public class CoqtopResponse {
	private final String message;
	private final CoqtopPromptInfo promptInfo;

	public CoqtopResponse(String message, CoqtopPromptInfo promptInfo) {
		this.message = message;
		this.promptInfo = promptInfo;
	}
	
	public String getMessage() {
		return message;
	}
	
	public CoqtopPromptInfo getPromptInfo() {
		return promptInfo;
	}
	
	//normal response? method
	
	
}
