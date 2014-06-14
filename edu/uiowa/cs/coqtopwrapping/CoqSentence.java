/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqtopwrapping;

/**
 *
 * @author hde
 */
public class CoqSentence {
    private String sentence;
    private int state_depth;

    public CoqSentence(String sentence, int state_depth) {
        this.sentence = sentence;
        this.state_depth = state_depth;
    }
    
    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public int getState_depth() {
        return state_depth;
    }

    public void setState_depth(int state_depth) {
        this.state_depth = state_depth;
    }
    
}
