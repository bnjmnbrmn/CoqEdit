package edu.uiowa.cs.coqedit;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;


//TO DO: Add caching for this
public class SentenceOffsetFinder {
	
	private View view; //For debugging
	private Buffer buffer;
	
	public SentenceOffsetFinder(BasicCoqEditController proofController) {
		this.buffer = proofController.getBuffer();
	}
	
	public int getEOSOffset(int sentenceNumber) {
		
		int bufferLength = buffer.getLength();
		if (bufferLength == 0) {
			return -1;
		}
			
		int currentOffset = 0;
		int sentencesSeen = 0;
		int commentDepth = 0;
		
		while (currentOffset < bufferLength - 1 
			&& sentencesSeen <= sentenceNumber) {
			if (buffer.getText(currentOffset,2).equals("(*")) {
				commentDepth++;
			}
			else if (buffer.getText(currentOffset,2).equals("*)")) {
				if (commentDepth > 0) {
					commentDepth--;
				}
			}
			else if (buffer.getText(currentOffset,1).equals(".")
				&& Character.isWhitespace(
					buffer
						.getText(currentOffset+1,1)
						.charAt(0))) {
				if (commentDepth == 0) {
					sentencesSeen++;
				}
				if (sentencesSeen == sentenceNumber + 1) {
					
					return currentOffset + 1;
				}
			}
						
			currentOffset++;
		}
		if (buffer.getText(bufferLength-1,1).equals(".") 
			&& sentencesSeen == sentenceNumber + 1
			&& commentDepth == 0) {
			return bufferLength;
		}
		return -1;
	}

}
