/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author bnjmnbrmn and Harley Eades
 */
public class BasicCoqtopWrapperImpl implements BasicCoqtopWrapper {
	
	private BasicCoqEditPresenter coqEditController;
	private Process coqtopProc;
	private PrintWriter coqtopWriter;    // coqtop's writer (STDIN).
	private BufferedReader coqtopReader; // coqtop's reader (STDOUT).
	private final String prompt_regex = "<prompt>.*</prompt>"; // The coqtop prompt pattern.
	
	public BasicCoqtopWrapperImpl(BasicCoqEditPresenter coqEditController) throws IOException{
		this.coqEditController = coqEditController;
		
		ProcessBuilder coqtopProcBuilder = new ProcessBuilder("coqtop", "-emacs");
		// Combine coqtop's STDOUT and STDERR.
        coqtopProcBuilder.redirectErrorStream(true);
        // Startup the coqtop external process.
        try {
            coqtopProc = coqtopProcBuilder.start();
        } catch (IOException ex) {
            throw new IOException("ERROR: coqtop failed to start.");
        }
        // Get coqtop's writer.
        coqtopWriter = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(coqtopProc.getOutputStream())));
        // Get coqtop's reader.
        coqtopReader = new BufferedReader(
                new InputStreamReader(coqtopProc.getInputStream()));

        // Skip coqtop's welcome message.
        try {
            skipWelcome();
        } catch (IOException ex) {
            throw new IOException("ERROR: failed to read from coqtop.");
        }
	}
	
	/** Skips coqtop's welcome message. */
    @SuppressWarnings("empty-statement")
    private void skipWelcome() throws IOException {
        // Skip welcome
        for (int c = coqtopReader.read(); c != '<'; c = coqtopReader.read());
        // Skip Prompt
        this.skipPrompt();
    }

    /** Skips coqtop's prompt. */
    private void skipPrompt() throws IOException {
        String l = "<";
        int c;
        while ((c = coqtopReader.read()) != -1) {
            l += (char) c;
            if (l.matches(this.prompt_regex)) {
                break;
            }
        }
    }
	
	/** Replaces each newline in a string with a space. 
     *
     * @param str the string to do the replace on.
     */
    private String changeNewlinesToSpaces(String str) {
        return str.replaceAll("\n", " ");
    }
	

	@Override
	public void writeSentenceToCoqtop(String sentence) {
		
//		if (sentence.equalsIgnoreCase("undo.")
//				|| sentence.startsWith("Back")
//				|| sentence.equalsIgnoreCase("quit.")
//				|| sentence.startsWith("Reset ")) {
//			
//			BasicCoqEditModel model = coqEditController.getCoqEditModel();
//			CoqtopPromptInfo pi;
//			synchronized(model) {
//				int lesn = model.getLastEvaluatedSentenceNumber();
//				try {
//					pi = model.getCachedResponseForSentence(lesn).getPromptInfo();
//				} catch (NoCachedSentenceException ex) {
//					if (lesn == -1) {
//						pi = new CoqtopPromptInfo(1, 0, new ArrayList<String>());
//					} else {
//						return;
//					}
//				}
//				String msg = "Error: Command not allowed in CoqEdit";
//				
//				readBypass = true;
//				bypassCoqtopResponse = new CoqtopResponse(msg,pi);
//				return;
//			}
//		
//		}
		
		
		String modifiedSentence;
		modifiedSentence = changeNewlinesToSpaces(sentence);
		coqtopWriter.println(modifiedSentence);
		coqtopWriter.flush();
		
		//for testing
//		CoqtopPromptInfo promptInfo = new CoqtopPromptInfo(-1, -1, null);
//		response = new CoqtopResponse("Coqtop wrapper response to: "+sentence,
//							promptInfo);
		
	}


	@Override
	public Process getCoqtopProcess() {
		return coqtopProc;
	}

	@Override
	public void setCoqtopProcess(Process coqtopProc) {
		this.coqtopProc = coqtopProc;
	}
	
	private CoqtopPromptInfo parse_prompt(String prompt) {
		int globalStateDepth;
		int currentProofStateDepth;
		List<String> lemmaStack = new ArrayList<String>();
		
		int i, j;
		//consume prompt start
		i = 8;
		j = 8;
		
		//consume current theorem/lemma name
		while(prompt.charAt(j) != '<') {
			j++;
		}
		
		j++;
		i = j;
		
		//consume global state depth
		while(prompt.charAt(j) != '|') {
			j++;
		}
		globalStateDepth = Integer.parseInt(prompt.substring(i, j).trim());
		
		j++;
		i = j;
		
		//consume lemma stack
		while(prompt.charAt(j) != '<') {
			if (prompt.charAt(j) == '|') {
				String potentialLemmaName = prompt.substring(i, j).trim();
				if (potentialLemmaName.length() > 0) {
					lemmaStack.add(potentialLemmaName);
				}
				i = j+1;
			}
			j++;
		}

		//get localStateDepth
		currentProofStateDepth = Integer.parseInt(prompt.substring(i, j).trim());

		return new CoqtopPromptInfo(globalStateDepth,
				currentProofStateDepth, lemmaStack);

	}
	

	@Override
	public CoqtopResponse readFromCoqtop() throws IOException {
		
//		if (readBypass) {
//			readBypass = false;
//			return bypassCoqtopResponse;
//		}
		
			//to do
		String output_msg = "";          // The message we will return.
		int cchar = coqtopReader.read(); // The current character.
		String line = (char) cchar + ""; // The current line.
		
		CoqtopPromptInfo responsePromptInfo = null;
		
		while (cchar != -1) {
			int next = coqtopReader.read();
			
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
						responsePromptInfo = parse_prompt(line);
						
						break;
					}
				}
			}
			cchar = next;
			
		}		
		
		CoqtopResponse response = 
				new CoqtopResponse(output_msg.trim(), responsePromptInfo);
		
		
//		readBypass = false;
		
		return response;
		
		
		//for testing:
//		try {	
//			Thread.sleep(500);
//		} catch (InterruptedException ex) {
//			
//		}
//		return response;
	}

	@Override
	public boolean isInterruptResponse(CoqtopResponse response) {
		return response.getMessage().startsWith("User interrupt.");
		
		
		//for testing
		//return false;
	}

	@Override
	public boolean isNormalNonGoalResponse(CoqtopResponse response) {
		return !(isInterruptResponse(response)
				|| isErrorResponse(response)
				|| isGoalResponse(response));
		
		//for testing
//		return true;
	}

	@Override
	public boolean isErrorResponse(CoqtopResponse response) {
		return response.getMessage().contains("Error:") 
				|| response.getMessage().contains("error:") ;
		
		//for testing
//		return false;
	}

	@Override
	public boolean isGoalResponse(CoqtopResponse response) {
		String msg = response.getMessage();
		
		return msg.contains("subgoals") && msg.contains("dependent evars:");
		
//		return msg.contains("subgoals")
//				&& msg.contains("dependent evars:")
//				&& msg.contains("(ID ");
		
		//for testing
//		return false;
	}

	@Override
	public boolean isProofModeResponse(CoqtopResponse response) {
		return !response.getPromptInfo().getLemmaStack().isEmpty();
		
		//for testing:
//		return false;
	}

	@Override
	public String getBackCommandForStateDepth(int stateDepth) {
		return "BackTo "+stateDepth+".";
	}

	/** Interrupts coqtop process.  
	 * 
	 * Currently will only work if the coqtop process is a UNIXProcesses.
	 * Meant to be called from the Event Dispatch Thread.
	 */
	@Override
	public void interruptCoqtopProcess() {
		synchronized(coqEditController.getCoqEditModel()) {
			try {
				sigIntUnixProcess(coqtopProc);
			} catch (Exception ex) {
				Macros.message(jEdit.getActiveView(),"Problem interrupting coqtop");
			}
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

	@Override
	public void destroyCoqtopProcess() {
		coqtopProc.destroy();
	}
	
	
	
	
	
	
	
	
//	//for testing
//	public static void main(String[] args) {
//		BasicCoqtopWrapper ctw;
//		try {
//			ctw = new BasicCoqtopWrapperImpl(null);
//		} catch (IOException ex) {
//			System.out.println("IOException occured while"
//					+ " initializing coqtop wrapper");
//			return;
//		}
//		
//		test1(ctw);
//		
//		
//		
//		
//	}
//	
//	//for testing
//	public static void test1(BasicCoqtopWrapper ctw) {
//		wspr(ctw,"Theorem t : 2+2 = 4.");
//		wspr(ctw,"Theorem t2 : 4 = 4.");
//		wspr(ctw,"auto.");
//		wspr(ctw,"Qed.");
//		wspr(ctw,"simpl.");
//		wspr(ctw,"reflexivity.");
//		wspr(ctw,"Qed.");
//		ctw.destroyCoqtopProcess();
//	}
//	
//	//for testing
//	public static void wspr(BasicCoqtopWrapper ctw, String sentence) { //write sentence print response
//		System.out.println("SENTENCE ENTERED:");
//		System.out.println(sentence+"\n");
//		
//		ctw.writeSentenceToCoqtop(sentence);
//		CoqtopResponse response;
//		try {
//			response = ctw.readFromCoqtop();
//		} catch (IOException ex) {
//			System.out.println("IOException while reading from coqtop");
//			return;
//		}
//		pr(response);
//	}
//	
//	//for testing
//	public static void pr(CoqtopResponse response) { //print response
//		System.out.println("RESPONSE:");
//		
//		System.out.println("response.getMessage():\n"+response.getMessage());
//		System.out.println("response.getPromptInfo().getGlobalStateDepth(): "
//				+response.getPromptInfo().getGlobalStateDepth());
//		System.out.println("response.getPromptInfo().getCurrentProofStateDepth(): "
//				+response.getPromptInfo().getCurrentProofStateDepth());
//		System.out.println("response.getPromptInfo().getLemmaStack(): "
//				+response.getPromptInfo().getLemmaStack());
//		
//	}

	@Override
	public boolean isNavigationSentence(String sentence) {
		sentence = sentence.trim();
		 return sentence.equalsIgnoreCase("undo.")
				|| sentence.startsWith("Back")
				|| sentence.equalsIgnoreCase("quit.")
				|| sentence.startsWith("Reset ");
	}

}
