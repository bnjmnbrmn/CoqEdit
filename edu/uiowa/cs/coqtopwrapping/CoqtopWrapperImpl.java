package edu.uiowa.cs.coqtopwrapping;

import edu.uiowa.cs.itpwrapping.ITPListener;
import edu.uiowa.cs.itpwrapping.ITPOutputEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Harley Eades and Benjamin Berman
 */
public class CoqtopWrapperImpl implements CoqtopWrapper {
    private Process           coq;                    // coqtop.
    private PrintWriter       coqtop_writer;          // coqtop's writer (STDIN).
    private BufferedReader    coqtop_reader;          // coqtop's reader (STDOUT).
    protected List<CoqSentence> steList;                // List of sentences to send to coqtop.
    protected CoqtopRWRunnable  coqtopRWRunnable;       // Reader/Writer class.
    private Thread            coqtopRWThread;         // Reader/Writer thread.
    private boolean           coqtopRWThreadLoopFlag; // Controls the main loop of the R/W-thread.
    private List<ITPListener> listeners;              // The listeners.    
    protected int             current_state_depth;    // Holds the last known state depth.
    private boolean           pauseEvaluation;        // Pauses the sentence evlauation thread.
    protected boolean           sent_evaluating;
    private final String      prompt_regex = "<prompt>.*</prompt>"; // The coqtop prompt pattern.
    
    // Debugging:
    protected PrintWriter logger;
    protected boolean show_debug_info = false;

    /** Starts up coqtop using the shells PATH env variable. */
    public CoqtopWrapperImpl() throws IOException {
        ProcessBuilder coqtopProcBuilder = new ProcessBuilder("coqtop", "-emacs");
        
	initialize(coqtopProcBuilder);
    }

    /** Starts up coqtop using the given path. 
     * 
     * @param coqtoppath the absolute path to coqtop.
     */ 
    public CoqtopWrapperImpl(String coqtoppath) throws IOException {
	ProcessBuilder coqtopProcBuilder = new ProcessBuilder(coqtoppath, "-emacs");
	initialize(coqtopProcBuilder);
    }

    /** Initialization process. 
     * 
     * @param coqtopProcBuilder the coqtop ProcessBuilder.
     */
    private void initialize(ProcessBuilder coqtopProcBuilder) throws IOException {
        // Combine coqtop's STDOUT and STDERR.
        coqtopProcBuilder.redirectErrorStream(true);
        // Startup the coqtop external process.
        try {
            this.coq = coqtopProcBuilder.start();
        } catch (IOException ex) {
            throw new IOException("ERROR: coqtop failed to start.");
        }
        // Get coqtop's writer.
        coqtop_writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(this.coq.getOutputStream())));
        // Get coqtop's reader.
        coqtop_reader = new BufferedReader(
                new InputStreamReader(this.coq.getInputStream()));

        // Skip coqtop's welcome message.
        try {
            this.skipWelcome();
        } catch (IOException ex) {
            throw new IOException("ERROR: failed to read from coqtop.");
        }
        // Allocate listener list.
        this.listeners = Collections.synchronizedList(new LinkedList<ITPListener>());

        // Allocate a collection to store our coptop commands.
        steList = Collections.synchronizedList(new LinkedList<CoqSentence>());

        // Startup the read/write thread.
        coqtopRWRunnable = this.new CoqtopRWRunnable();
        coqtopRWThread = new Thread(this.new CoqtopRWRunnable());
        coqtopRWThread.setDaemon(true);
        this.coqtopRWThreadLoopFlag = true;
        this.pauseEvaluation = false;
        coqtopRWThread.start();
        
        // Debugging
        if (this.show_debug_info) {
            FileWriter outFile = new FileWriter("/tmp/COQTOP.log");

            this.logger = new PrintWriter(outFile);
        }
    }

    /** Skips coqtop's welcome message. */
    @SuppressWarnings("empty-statement")
    private void skipWelcome() throws IOException {
        // Skip welcome
        for (int c = coqtop_reader.read(); c != '<'; c = coqtop_reader.read());
        // Skip Prompt
        this.skipPrompt();
    }

    /** Skips coqtop's prompt. */
    private void skipPrompt() throws IOException {
        String l = "<";
        int c;
        while ((c = coqtop_reader.read()) != -1) {
            l += (char) c;
            if (l.matches(this.prompt_regex)) {
                break;
            }
        }
    }

    /** Adds a coqtop sentence to the sentence list.
     * Everything in this list needs to be evaluated.     
     * 
     * @param sentence the sentence to be added.
     * @return -1      if the sentence is one of "Undo.", "Back _.", "Quit.", or "Rest _.".
     * @return 0       if the sentence was successfully added to the list.
     */
    private synchronized int addToSTEList(CoqSentence sentence) {
        // Guards against the user trying to use these commands themselves.
        if (sentence.getSentence().equalsIgnoreCase("undo.")
                || sentence.getSentence().startsWith("Back ")
                || sentence.getSentence().equalsIgnoreCase("quit.")
                || sentence.getSentence().startsWith("Reset ")) {
            return -1;
        }

        // Add the sentence to the queue.
        sentence.setSentence(changeNewlinesToSpaces(sentence.getSentence()));
        steList.add(steList.size(), sentence);
        coqtopRWThread.interrupt();

        return 0;
    }

    /** Replaces all newlines with a space in a string. 
     *
     * @param str the string to do the replace on.
     */
    private String changeNewlinesToSpaces(String str) {
        return str.replaceAll("\n", " ");
    }

    /** Shuts down the coqtop process. */
    @Override
    public void shutdownITP() {
        coqtopRWThreadLoopFlag = false;
        coq.destroy();
    }

    /** Registers a new listener. 
     * 
     * @param listener the listener to be added.
     */
    @Override
    public void registerListener(ITPListener listener) {
        this.listeners.add(listener);
    }

    /** Sends a new command to coqtop.
     * 
     * @param sen the command to send to coqtop.
     * @param state_depth the state depth of sen.
     */
    @Override
    public void sendToITP(String sen, int state_depth) {
        CoqSentence sent = new CoqSentence (sen,state_depth);
        if (this.addToSTEList(sent) == -1) {
            ITPOutputEvent error = new ITPOutputEvent("Unsupported Command: " + sen, this.current_state_depth);
            notifyListeners(error, true);
        }
    }

    /** Gets the PID of a UNIX process that we spawned.
     * @param process the process you want the PID of.
     * @return the pid.
     * @throws Exception if the process is not a UNIX process we fail.
     */
    private static int getUnixPID(Process process) throws Exception {
        if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
            Class cl = process.getClass();
            Field field = cl.getDeclaredField("pid");
            field.setAccessible(true);
            Object pidObject = field.get(process);
            return (Integer) pidObject;
        } else {
            throw new IllegalArgumentException("Needs to be a UNIXProcess");
        }
    }

    /** Sends a SIGINT to a UNIX process.
     * @param process the process you want the PID of.
     * @return the return fail of the kill command.
     */
    private static int sigIntUnixProcess(Process process) throws Exception {
      int pid = getUnixPID(process);
      return Runtime.getRuntime().exec("kill -2 " + pid).waitFor();
    }
    
    /** Interrupts the sentence coqtop is currently evaluating.  
     * This still needs to be implemented. 
     */
    @Override
    public synchronized void interruptCurrentCommand() {
        if (sent_evaluating) {
            // Empty the queue.
            this.steList.clear();

            try {
                sigIntUnixProcess(this.coq);
            } catch (Exception ex) {
                Logger.getLogger(CoqtopWrapperImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            this.steList.clear();
        }
    }
    
    /** Notifies all the listeners of an event. 
     * 
     * @param event the even which took place.
     * @param error true if this is an error event false otherwise.
     */
    private synchronized void notifyListeners(ITPOutputEvent event, boolean error) {
        final boolean err = error;
        final ITPOutputEvent e = event;
        
        Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < listeners.size(); i++) {
            if (err) {
                listeners.get(i).errorEventReceived(e);
            } else {
                listeners.get(i).standardEventReceived(e);
            }
        }
        }
        
        });
        t.start();
    }

    /** Finds the a given state depth in the queue and removes everything after it. 
     * 
     * @param state_depth the state depth to find.
     */
    private void cutQueueAfterState(int state_depth) {
        /* Let the coqtop thread know to wait if it
           finishes before we get done cutting the queue. */
        this.pauseEvaluation = true;
        
        // Cut the queue.
        for (int i = 0; i < this.steList.size(); i++) {
            CoqSentence s = this.steList.get(i);
            if (s.getState_depth() >= state_depth)
                this.steList.remove(i);
        }
        
        // Okay, coqtop can resume.
        this.pauseEvaluation = false;
    }
    
    /** Issues the "Back _" coqtop command. 
     * 
     * @param back_to_state_depth the state depth to move back to.
     */
    @Override
    @SuppressWarnings("empty-statement")
    public synchronized void back(int back_to_state_depth) {
        back(back_to_state_depth, true);
    }
    
    public synchronized void back(int back_to_state_depth, boolean notify) {
        if (this.show_debug_info)
            this.log("Back Method Hit: "+back_to_state_depth+":"+current_state_depth);
        
      // Build the coqtop sentence using state_depth.
      String sentence = "BackTo "+back_to_state_depth+".";
      
      /* If the given state depth is before the command coqtop is 
       * currently evaluating, then inturrupt coqtop and have it
       * move back.  If the given state depth is after the command
       * coqtop is currently evaluating, then simply remove
       * all the commands in the sentence list after the given state depth.
       * 
       * The current implementation does not do the former where we
       * inturrupt coqtop it just adds the back to list of sentences
       * and evaluates everything up the back command.
       */
      if (back_to_state_depth <= this.current_state_depth) {
        if (this.sent_evaluating) this.interruptCurrentCommand();
        this.sendToITP(sentence, this.current_state_depth+1);
      } else if (back_to_state_depth > this.current_state_depth) {
        if (this.show_debug_info) {
            log("Back Method: Hit >-case.");
            log_queue();
        }
        this.cutQueueAfterState(back_to_state_depth);
        
        if (this.show_debug_info) {
            log_queue();
        }
        
        if(notify)
            // Tell the listerners that we cut the queue.
            notifyListeners(new ITPOutputEvent("Cutting queue...", back_to_state_depth), false);
      }
    }
    

    /** Returns the coqtop's current state depth. */
    @Override
    public int getCurrentITPState() {
        return this.current_state_depth;
    }

    private String  parse_sent(String msg) {            
        return msg;
    }
    
    private Boolean sent_type(String msg) {                
        
        return msg.toLowerCase().contains("error");
    }
    
    protected void log(String msg) {
        // Write text to file
        logger.println(msg);
        logger.flush();
    }
    
    protected void log_queue() {
        log("The Queue: ");
        for (int i = 0; i < this.steList.size(); i++) {
            log("\t"+this.steList.get(i).getState_depth()+"::"+this.steList.get(i).getSentence());
        }
        log("End Queue");
    }
    
        // Inline class which is the heart of our RW-thread.
    protected class CoqtopRWRunnable implements Runnable {

        /**
         * The threads main loop.
         */
        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            while (coqtopRWThreadLoopFlag) {
                // Check to make sure no one else is reading.
                while (!steList.isEmpty()) {
                    synchronized (this) {
                        // Get the next sentence to send to coqtop.
                        String sent = steList.get(0).getSentence();
                        // Remove it from the queue.
                        steList.remove(0);
                        // Send it to coqtop.
                        sendToCoq(sent);
                        if (show_debug_info) {
                            log("Sent: " + sent);
                        }
                        sent_evaluating = true;
                    }

                    try {
                        String msg = parse_sent(readFromCoq());
                        // The current sentence is done being evaluated by coqtop.
                        sent_evaluating = false;
                        
                        if (show_debug_info)
                            log("   Current State Depth: " + current_state_depth);
                        
                        notifyListeners(new ITPOutputEvent(msg, current_state_depth), sent_type(msg));
                    } catch (IOException ex) {
                        notifyListeners(
                                new ITPOutputEvent("Failed to read from coqtop.", current_state_depth), true);
                    }

                    synchronized (this) {
                        sent_evaluating = false;
                    }

                    /* The queue is being trimmed.  We must wait. 
                     * You shall not PASS! */
                    while (pauseEvaluation);
                }
            }
        }

        /**
         * Sends a message to the coqtop process.
         *
         * @param msg the message to send.
         */
        public synchronized void sendToCoq(String msg) {
            if (show_debug_info)
                log("sendToCoq: "+msg);
            coqtop_writer.println(msg);
            coqtop_writer.flush();
            // Trigger that the sentence is begin run by coqtop.
            sent_evaluating = true;
        }

        /** Parses the state depth out of coqtop's prompt.
         * 
         * @param prmt the prompt.
         */
        @SuppressWarnings("empty-statement")
        private synchronized int parse_prompt(String prmt) {
            // Skip the "<prompt>" part of the prompt.
            prmt = prmt.substring(8);
            
            // Skip everything up until we hit '<'.
            int i = 0;
            for (; i < prmt.length() && prmt.charAt(i) != '<'; i++);
            // Skip '<' and the space after.
            prmt = prmt.substring(i + 2);
            
            // Grab out the state depth.
            prmt = prmt.substring(0, prmt.indexOf(" "));
            
            // Convert the state depth into an integer and return it.
            return Integer.parseInt(prmt);
        }

        /** Reads output from the coqtop process. */
        public synchronized String readFromCoq() throws IOException {
            String output_msg = "";                   // The message we will return.
            int cchar         = coqtop_reader.read(); // The current character.
            String line       = (char) cchar + "";    // The current line.

            /* Keep slurping until we either run out of input or we
               hit the prompt. */
            while (cchar != -1) {
                int next = coqtop_reader.read();

                /* We read the output in line by line that
                 * way we can properly test for the prompt. */
                if (next == '\n') {
                    output_msg += line + (char) next;
                    line = "";
                } else {
                    line += (char) next + "";
                    /* As soon as we hit '>' we start checking to see if we
                     * have hit the prompt. Not every efficent, but it work. */
                    if (next == '>') {
                        // When we hit the prompt grab the state depth and we are done.
                        if ((line.trim()).matches(prompt_regex)) {
                            current_state_depth = parse_prompt(line);
                            if (show_debug_info)
                                log("Read prompt: "+current_state_depth);
                            break;
                        }
                    }
                }
                cchar = next;
            }
            return output_msg.trim();
        }
    }
}
