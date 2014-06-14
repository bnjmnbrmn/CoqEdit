/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.itpwrapping;

/**
 *
 * @author bnjmnbrmn
 */
public interface ITPWrapper {
	void registerListener(ITPListener listener);
	
	void sendToITP(String msg, int state_depth);
	
	void shutdownITP();
	
	void interruptCurrentCommand();
        
        int getCurrentITPState();
}
