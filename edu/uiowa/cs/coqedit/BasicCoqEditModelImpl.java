/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author bnjmnbrmn
 */
public class BasicCoqEditModelImpl implements BasicCoqEditModel {

	private List<Integer> endOfSentenceOffsets = new ArrayList<Integer>();
	private BasicCoqEditController controller;
	private Buffer buf;
	private int currentlyViewingSentenceNumber;
	private int lastEvaluatedSentenceNumber;
	private int errorSentenceNumber;
	
	
	
	//private SentenceQueue sentenceQueue;  
	
	private List<String> sentenceQueue;
	private int interruptCount;
	
	private List<CoqtopResponse> responseCache;
	private boolean isProcessingSentence;
	
	private boolean processingBackCommand = false;
	private boolean processingSynchronously = false;
	private CoqtopResponse currentResponse = null;
	
	private boolean isNavigating = false;
	
	private int dequeueCount = 0;
	
	private CoqtopResponseResponderFactory responseResponderFactory;
	
	
	public BasicCoqEditModelImpl(BasicCoqEditController controller) {
		this.controller = controller;
		this.buf = controller.getBuffer();
		
		this.currentlyViewingSentenceNumber = -1; //initially no sentences have been evaluated, so the output from no sentence can be inspected
		this.lastEvaluatedSentenceNumber = -1; 
		this.errorSentenceNumber = -1; //initially no sentences have produced errors
		
		//this.sentenceQueue = new SentenceQueue();
		
		this.sentenceQueue = new ArrayList<String>();
		this.isProcessingSentence = false;
		
		responseCache = new ArrayList<CoqtopResponse>(); //cache of the output of sentence evaluation, indexed by sentence number
		
		interruptCount = 0;
		
		responseResponderFactory = this.controller;
	}

	@Override
	public synchronized List<Integer> getEndOfSentenceOffsets() {
		return endOfSentenceOffsets;
	}

	@Override
	public synchronized void addEndOfSentenceOffset(int sentenceOffset) {
		endOfSentenceOffsets.add(
			endOfSentenceOffsets.size(), 
			sentenceOffset);
	}

	@Override
	public synchronized void removeEndOfSentenceOffsetsGreaterThan(int sentenceOffset) {
		int i = 0;
		
		
		for (Integer endOfSentenceOffset : endOfSentenceOffsets) {
			if (endOfSentenceOffset > sentenceOffset) {
				endOfSentenceOffsets = 
					endOfSentenceOffsets.subList(0, i);
				return;
			}
			i++;
		}
	}
	
	//A potentially more efficient implementation:
//	@Override
//	public void removeEndOfSentenceOffsetsGreaterThan(int sentenceOffset) {
//		//I think this is right, but it needs testing...
//		
//		
//		if (endOfSentenceOffsets.size() == 0 )
//			return;
//		
//		if (sentenceOffset >= 
//			endOfSentenceOffsets.get(endOfSentenceOffsets.size() - 1)
//		    )
//			return;
//		
//		if (sentenceOffset < endOfSentenceOffsets.get(0)) {
//			endOfSentenceOffsets.clear();
//			return;
//		}
//		
//		//if we reach here, endOfSentenceOffsets.size() > 1
//			
//	
//		int high = endOfSentenceOffsets.size() - 1;
//		
//		int low = 0; 
//		
//		int mid = high/2;
//		
//		while (high >= low) {
//			int midOffset = endOfSentenceOffsets.get(mid);
//			int midPlusOneOffset = endOfSentenceOffsets.get(mid+1);
//			if (midOffset < sentenceOffset && sentenceOffset <= midPlusOneOffset) {
//				endOfSentenceOffsets = endOfSentenceOffsets.subList(0, mid+1);
//				return;
//			} else if (midOffset >= sentenceOffset) {
//				high = mid;
//				mid = low + (high - low)/2;
//			} else if (midPlusOneOffset < sentenceOffset) {
//				low = mid;
//				mid = low + (high - low)/2;
//			}
//		}
//		
//			
//		
//	}

	@Override
	public synchronized int getEndOffset(int sentenceNumber) throws NoOffsetForSentenceNumberException {
		if (sentenceNumber == -1) { //
			return 0;
		}
		
		if (sentenceNumber < -1) {
			throw new NoOffsetForSentenceNumberException();
		}
		
		if (sentenceNumber >= endOfSentenceOffsets.size()) {
			int diff = sentenceNumber - 
				(endOfSentenceOffsets.size() - 1);
			
			while (diff > 0) {
				try {
					getNextEOSOffset();
					diff--;
				} catch (NoMoreSentencesException ex) {
					throw new NoOffsetForSentenceNumberException();
				}
				
			}
			
		}
		return endOfSentenceOffsets.get(sentenceNumber);
		
	}

	@Override
	public synchronized int getEndOfSentenceOffsetsSize() {
		return endOfSentenceOffsets.size();
	}

	/**
	 * Adds the next endOfSentenceOffset to the endOfSentenceOffsets list.
	 * 
	 * @throws NoMoreSentencesException 
	 */
	private synchronized void getNextEOSOffset() throws NoMoreSentencesException {
		
		int cd = 0; //comment depth
		
		int co; //current offset
		int oll = endOfSentenceOffsets.size(); //offsets list length
		if (oll == 0) {
			co = 0;
		} else {
			co = endOfSentenceOffsets
				.get(oll - 1);
		}
		
		int bl = buf.getLength();
		
		
		while (true) {
			if (co == bl) throw new NoMoreSentencesException();
			
			
			if(cd == 0 && buf.getText(co, 1).equals(".")) {
				if(co == bl - 1) {
					endOfSentenceOffsets.add(co + 1);
					return;
				} else {
					char c = buf.getText(co+1, 1).charAt(0);
					if ( Character.isWhitespace(c) ) {
						endOfSentenceOffsets.add(co + 1);
						return;
					}
				}
			}
			
			if (co <= bl - 2) {
				String s = buf.getText(co,2);
				if (s.equals("(*")) {
					cd++;
					co++; //to prevent "(*)" from being read as a comment
				} else if (cd > 0 && s.equals("*)")) {
					cd--;
				}
			}
			
			co++;
		}

		
	}

	@Override
	public synchronized int getCurrentlyViewingSentenceNumber() {
		return currentlyViewingSentenceNumber;
	}

	@Override
	public synchronized int getLastEvaluatedSentenceNumber() {
		return lastEvaluatedSentenceNumber;
	}

	/**"Queued" sentences include those that are currently being processed*/
	@Override
	public synchronized int getLastQueuedSentenceNumber() {
		return lastEvaluatedSentenceNumber + dequeueCount + sentenceQueue.size();
		
		
//		if (isProcessingSentence) {
//			return lastEvaluatedSentenceNumber + sentenceQueue.size() + 1;
//		} else {
//			return lastEvaluatedSentenceNumber + sentenceQueue.size();
//		}
		
				
		//return lastQueuedSentenceNumber;
	}

	@Override
	public synchronized int getErrorSentenceNumber() {
		return errorSentenceNumber;
	}

	@Override
	public synchronized void setCurrenlyViewingSentenceNumber(int newSentenceNumber) {
		currentlyViewingSentenceNumber = newSentenceNumber;
	}

	@Override
	public synchronized void setLastEvaluatedSentenceNumber(int newSentenceNumber) {
		lastEvaluatedSentenceNumber = newSentenceNumber;
	}


	@Override
	public synchronized void setErrorSentenceNumber(int newSentenceNumber) {
		errorSentenceNumber = newSentenceNumber;
	}

	@Override
	public synchronized int getCurrentlyViewingSentenceStartOffset() {
		try {
			return getEndOffset(currentlyViewingSentenceNumber - 1);
		} catch (NoOffsetForSentenceNumberException ex) {
			//Logger.getLogger(BasicCoqEditModelImpl.class.getName()).log(Level.SEVERE, null, ex);
			return -1;
		}
	}
	
	@Override
	public synchronized int getCurrentlyViewingSentenceEndOffset() {
		try {
			return getEndOffset(currentlyViewingSentenceNumber);
		} catch (NoOffsetForSentenceNumberException ex) {
			//Logger.getLogger(BasicCoqEditModelImpl.class.getName()).log(Level.SEVERE, null, ex);
			return -1; //should probably raise an unchecked exception here, actually
		}
	}

	@Override
	public synchronized int getEvaluatedSectionEndOffset() {
		try {
//			Macros.message(jEdit.getActiveView(),"getting evaluated section end offset for"
//					+ " lastEvaluatedSentenceNumber: "+ lastEvaluatedSentenceNumber);
			return getEndOffset(lastEvaluatedSentenceNumber);
		} catch (NoOffsetForSentenceNumberException ex) {
			//Logger.getLogger(BasicCoqEditModelImpl.class.getName()).log(Level.SEVERE, null, ex);
			return -1;
		}
	}

	@Override
	public synchronized int getQueuedSectionEndOffset() {
		try {
			return getEndOffset(getLastQueuedSentenceNumber());
		} catch (NoOffsetForSentenceNumberException ex) {
			//Logger.getLogger(BasicCoqEditModelImpl.class.getName()).log(Level.SEVERE, null, ex);
			return -1;
		}
	}
	
	@Override 
	public synchronized int getErrorSentenceStartOffset () {
		try {
			return getEndOffset(errorSentenceNumber - 1);
		} catch (NoOffsetForSentenceNumberException ex) {
			//Logger.getLogger(BasicCoqEditModelImpl.class.getName()).log(Level.SEVERE, null, ex);
			return -1;
		}
	}

	@Override
	public int getErrorSentenceEndOffset() {
		try {
			return getEndOffset(errorSentenceNumber);
		} catch (NoOffsetForSentenceNumberException ex) {
			//Logger.getLogger(BasicCoqEditModelImpl.class.getName()).log(Level.SEVERE, null, ex);
			return -1;
		}
	}
	
	public synchronized void  enqueueSentence(String sentence) {
		sentenceQueue.add(sentenceQueue.size(), sentence);
		notifyAll();
	}

	/**Blocks*/
	@Override
	public synchronized String dequeueSentence() {
		return sentenceQueue.remove(0);
		
	}
	
	@Override
	public synchronized void removeSentenceQueueTailInclusive(int i) {
		sentenceQueue = sentenceQueue.subList(0, i);
	}

//	@Override
//	public void enqueueSentence(String sentence) {
//		sentenceQueue.enqueueSentence(sentence);
//	}
//
//	/**Blocks*/
//	@Override
//	public String dequeueSentence() throws InterruptedException {
//			return sentenceQueue.dequeueSentence();
//		
//	}
//	
//	@Override
//	public void removeSentenceQueueTailInclusive(int i) {
//		sentenceQueue.removeSentenceQueueTailInclusive(i);
//	}
//
//	//it might be better to get rid of this inner class and 
//	//just synchronize the methods of the model
//	
//	private class SentenceQueue {
//
//		private List<String> sentenceQueueList;
//
//		public SentenceQueue() {
//			this.sentenceQueueList = new ArrayList<String>();
//		}
//
//		public synchronized void enqueueSentence(String sentence) {
//			sentenceQueueList.add(sentenceQueueList.size(), sentence);
//			notifyAll();
//		}
//		
//		public synchronized String dequeueSentence() throws InterruptedException {
//			while (sentenceQueueList.isEmpty()) {
//				wait();
//			}
//			return sentenceQueueList.remove(0);
//		}
//
//		public synchronized void removeSentenceQueueTailInclusive(int i) {
//			sentenceQueueList = sentenceQueueList.subList(0, i + 1);
//		}
//	}

	
	
	@Override
	public synchronized void addToResponseCache(CoqtopResponse response) {
		responseCache.add(response);
	}

	@Override
	public synchronized CoqtopResponse getCachedResponseForSentence(int sentenceNumber) throws NoCachedSentenceException {
		try {
			return responseCache.get(sentenceNumber);
		} catch (IndexOutOfBoundsException ex) {
			throw new NoCachedSentenceException();
		}
		
	}
	
	@Override
	public void removeResponsesFromCacheAtIndicesGreaterThanOrEqualTo(int sentenceNumber) {
		responseCache = responseCache.subList(0, sentenceNumber);
	}
	
	@Override
	public synchronized int getResponseCacheSize() {
		return responseCache.size();
	}

	@Override
	public synchronized boolean isProcessingSentence() {
		return isProcessingSentence;
	}

	@Override
	public synchronized void setIsProcessingSentence(boolean isProcessingSentence) {
		this.isProcessingSentence = isProcessingSentence;
	}
	
	

	@Override
	public synchronized int getSection(int offset) {
//		Macros.message(jEdit.getActiveView(), 
//				"In getSection(offset):\n\n"
//				+"offset: "+offset
//				+"\ngetEvaluatedSectionEndOffset(): "
//				+getEvaluatedSectionEndOffset()
//				+"\ngetLastEvaluatedSentenceNumber(): "+getLastEvaluatedSentenceNumber());
		if (offset >= 0 && offset < getEvaluatedSectionEndOffset()) {
			return BasicCoqEditModel.EVALUATED_SECTION;
		} else if (offset >= getEvaluatedSectionEndOffset()
				&& offset < getQueuedSectionEndOffset()) {
			try {
				if (offset < getEndOffset(getLastEvaluatedSentenceNumber()+dequeueCount)) {
				//if (offset < getEndOffset(getLastEvaluatedSentenceNumber()+1)) {
					return BasicCoqEditModel.QUEUED_SECTION_HEAD;
				} else {
					return BasicCoqEditModel.QUEUED_SECTION_TAIL;
				}
			} catch (NoOffsetForSentenceNumberException ex) {
				return -2; //this should never happen
			}
		} else if ( errorSentenceNumber != -1
				&& offset >= getEvaluatedSectionEndOffset()
				&& offset < getErrorSentenceEndOffset()) {
			return BasicCoqEditModel.ERROR_SECTION;
		} else if ( (errorSentenceNumber == -1
				     && offset >= getQueuedSectionEndOffset())
				|| (errorSentenceNumber != -1
				    && offset >= getErrorSentenceEndOffset())) {
			return BasicCoqEditModel.UNHIGHLIGHTED_SECTION;
		} else {
			return -1;
		}
	}

	/**
	 * Returns sentence number for offset; the offsets for a sentence include the endOfSentenceOffset
	 * for the previous sentence, but not the sentence&apos;s own endOfSentenceOffset.
	 * 
	 * Note that text need not be processed to be part of a sentence.
	 * However, text at the end of the buffer that is not ended with a &quot;.&quot; is not considered part of a sentence. 
	 * 
	 * @param offset
	 * @return -1 on error, the appropriate sentence number (>= 0) otherwise
	 */
	@Override
	public int getSentenceNumberForOffset(int offset) { 
		int sn;
		
		if ( offset < 0 ) {
			return -1;
		}
		
		if (endOfSentenceOffsets.isEmpty()) {
			try {
				getNextEOSOffset();
			} catch (NoMoreSentencesException ex) {
				return -1;
			}
		}
		
		//search for appropriate sentence number within endOfSentenceOffsets list indices; should use a binary search here...
		if (!endOfSentenceOffsets.isEmpty()) {
			sn = 0;
			if (offset < endOfSentenceOffsets.get(sn)) {
				return 0;
			}
			for (; sn <= endOfSentenceOffsets.size() - 2; sn++) {
				if (endOfSentenceOffsets.get(sn) <= offset
						&& offset < endOfSentenceOffsets.get(sn + 1)) {
					return sn + 1;
				}
			}
		}
		
		//try extending the endOfSentenceOffsets list
		while (true) {
			try {
				getNextEOSOffset();
			} catch (NoMoreSentencesException ex) {
				return -1;
			}
			sn = endOfSentenceOffsets.size() - 1;
			if (offset < endOfSentenceOffsets.get(sn)) {
				return sn;
			}
		}
		
	}

	@Override
	public int getStateDepthAfterEvaluatingSentenceNumber(int sentenceNumber) {
		return sentenceNumber + 2; //for now, at least
	}
	
	@Override
	public int getSentenceNumberToEvaluateForStateDepth(int stateDepth) {
		return stateDepth - 2; //for now at least
	}
	
	@Override
	public synchronized int getInterruptCount() {
		return this.interruptCount;
	}

	@Override
	public synchronized void setInterruptCount(int newInterruptCount) {
		this.interruptCount = newInterruptCount;
		notifyAll();
	}

	@Override
	public void incrementInterruptCount() {
		interruptCount++;
		notifyAll();
	}

	@Override
	public void decrementInterruptCount() {
		interruptCount--;
	}

	@Override
	public boolean processingBackCommand() {
		return processingBackCommand;
	}

	@Override
	public void setProcessingBackCommand(boolean b) {
		processingBackCommand = b;
	}

	@Override
	public List<String> getSentenceQueue() {
		return sentenceQueue;
	}
	
	@Override
	public synchronized boolean processingSynchronously() {
		return processingSynchronously;
	}
	
	@Override
	public synchronized void setProcessingSynchronously(boolean processingSynchronously) {
		this.processingSynchronously = processingSynchronously;
	}

	@Override
	public synchronized void setCurrentResponse(CoqtopResponse response) {
		this.currentResponse = response;
	}

	@Override
	public synchronized CoqtopResponse getCurrentResponse() {
		return this.currentResponse;
	}

	@Override
	public synchronized boolean isNavigating() {
		return isNavigating;
	}
	
	@Override
	public void setIsNavigating(boolean isNavigating) {
		this.isNavigating = isNavigating;
	}

	@Override
	public synchronized void incrementDequeueCount() {
		dequeueCount++;
	}

	@Override
	public synchronized void decrementDequeueCount() {
		if (dequeueCount > 0)
			dequeueCount--;
	}

	@Override
	public synchronized int getDequeueCount() {
		return dequeueCount;
	}

	@Override
	public void setDequeueCount(int dequeueCount) {
		this.dequeueCount = dequeueCount;
	}
	
	@Override
	public CoqtopResponseResponderFactory getCoqtopResponseResponderFactory() {
		return responseResponderFactory;
	}
	
	@Override
	public void setCoqtopResponseResponderFactory(CoqtopResponseResponderFactory newResponseResponder) {
		responseResponderFactory = newResponseResponder;
	}
	
}

