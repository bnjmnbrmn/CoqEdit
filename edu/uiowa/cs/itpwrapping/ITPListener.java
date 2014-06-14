/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.itpwrapping;

/**
 *
 * @author bnjmnbrmn
 */
public interface ITPListener {
	void errorEventReceived(ITPOutputEvent event);
	
	void standardEventReceived(ITPOutputEvent event);
}
