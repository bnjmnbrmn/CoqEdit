/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqtopwrapping;

import edu.uiowa.cs.itpwrapping.ITPListener;
import edu.uiowa.cs.itpwrapping.ITPWrapper;

/**
 *
 * @author bnjmnbrmn
 */
public interface CoqtopWrapper extends ITPWrapper {
    public void back(int state_depth);
}
