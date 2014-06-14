/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

/**
 *
 * @author bnjmnbrmn
 */
public interface CoqtopResponseResponderFactory {
	public Runnable createCoqtopResponseResponder(CoqtopResponse response);
}
