/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqtopwrapping;

import edu.uiowa.cs.itpwrapping.ITPOutputEvent;
import java.io.IOException;

/**
 *
 * @author hde
 */
public class TestCoqtopWrapperImp {

    public static class Listener implements CoqtopListener {

        @Override
        public void errorEventReceived(ITPOutputEvent event) {
            System.out.println(event.getPayload());
            //System.out.println(event.getState());
        }

        @Override
        public void standardEventReceived(ITPOutputEvent event) {
            System.out.println(event.getPayload());
            //System.out.println(event.getState());
        }
    }
       
    public static void main(String[] args) throws IOException, InterruptedException {
        CoqtopWrapperImpl coqtop = new CoqtopWrapperImpl("coqtop");
        
        coqtop.registerListener(new Listener());
        
        //test1(coqtop);
        test3(coqtop);
        
        // Wait for the output.
        Thread.sleep(5000);
        System.out.println("Hit shutdown.");
        coqtop.shutdownITP();
    }
    
    public static void test1(CoqtopWrapperImpl coqtop) throws InterruptedException {
        coqtop.sendToITP("Inductive Coqbool :  Set := true:Coqbool | false:Coqbool.", 1);
        coqtop.sendToITP("Definition succ := fun (x : nat) => x + 1.", 2);
        coqtop.sendToITP("Print succ.", 3);
        Thread.sleep(500);
        coqtop.back(2);
        coqtop.sendToITP("Print succ.", 2);
        
        coqtop.sendToITP("Axiom rw1 : 1 = 2.", 3);
        coqtop.sendToITP("Axiom rw2 : 2 = 1.", 4);
        coqtop.sendToITP("Hint Rewrite rw1 : db1.", 5);
        coqtop.sendToITP("Hint Rewrite rw2 : db1.", 6);
        coqtop.sendToITP("Lemma t : 2 = 1.", 7);
        coqtop.sendToITP("Proof.", 8);
        coqtop.sendToITP("autorewrite with db1.", 9);
        Thread.sleep(500);
        //coqtop.interruptCurrentCommand();
        coqtop.back(2);
        coqtop.sendToITP("Print succ.", 2);
        
        coqtop.sendToITP("admit.", 3);
        coqtop.sendToITP("Qed.", 4);
        
        coqtop.sendToITP("Lemma a : 1 = 1.", 5); 
        coqtop.sendToITP("Proof.", 6);
        coqtop.sendToITP("auto.", 7); 
        coqtop.sendToITP("Qed.", 8);
	
        coqtop.sendToITP("Print a.", 9);
        coqtop.sendToITP("Print a.",10);
        coqtop.sendToITP("Print a.",11);
        coqtop.sendToITP("Print a.",12);
    }
    
    public static void test3(CoqtopWrapperImpl coqtop) throws InterruptedException {
        coqtop.sendToITP("Inductive Coqbool :  Set := true:Coqbool | false:Coqbool.", 1);
        coqtop.sendToITP("Definition succ := fun (x : nat) => x + 1.", 2);
        coqtop.sendToITP("Print succ.", 3);
        Thread.sleep(500);
        coqtop.back(2);
        coqtop.sendToITP("Print succ.", 2);
        
        coqtop.sendToITP("Axiom rw1 : 1 = 2.", 3);
        coqtop.sendToITP("Axiom rw2 : 2 = 1.", 4);
        coqtop.sendToITP("Hint Rewrite rw1 : db1.", 5);
        coqtop.sendToITP("Hint Rewrite rw2 : db1.", 6);
        coqtop.sendToITP("Lemma t : 2 = 1.", 7);
        coqtop.sendToITP("Proof.", 8);
        coqtop.sendToITP("autorewrite with db1.", 9);
        Thread.sleep(500);
        coqtop.back(8);
        coqtop.sendToITP("Show.", 9);
        coqtop.sendToITP("admit.", 10);
        coqtop.sendToITP("Qed.", 11);
    }
    
    public static void test2(CoqtopWrapperImpl coqtop) throws InterruptedException {
        coqtop.sendToITP("Inductive Coqbool :  Set := true:Coqbool | false:Coqbool.", 1);
        coqtop.sendToITP("Definition succ := fun (x : nat) => x + 1.", 2);
        coqtop.sendToITP("Print succ.", 3);
        
        coqtop.sendToITP("Axiom rw1 : 1 = 2.", 4);
        coqtop.sendToITP("Axiom rw2 : 2 = 1.", 5);
        coqtop.sendToITP("Hint Rewrite rw1 : db1.", 6);
        coqtop.sendToITP("Hint Rewrite rw2 : db1.", 7);
        coqtop.sendToITP("Lemma t : 2 = 1.", 8);
        coqtop.sendToITP("Proof.", 9);
        coqtop.sendToITP("autorewrite with db1.", 10);
        coqtop.back(2);        
        coqtop.sendToITP("Print succ.", 2);
        coqtop.sendToITP("Lemma a : 1 = 1.", 3); 
        coqtop.sendToITP("Proof.", 4);
        coqtop.sendToITP("auto.", 5); 
        coqtop.sendToITP("Qed.", 6);
	
        coqtop.sendToITP("Print a.",7);
        coqtop.sendToITP("Print a.",8);
        coqtop.sendToITP("Print a.",9);
        coqtop.sendToITP("Print a.",10);
    }
}
