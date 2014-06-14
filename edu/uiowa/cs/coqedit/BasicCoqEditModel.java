/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit;

import java.util.List;

/**
 *
 * @author bnjmnbrmn
 */
public interface BasicCoqEditModel {
	public static final int EVALUATED_SECTION = 0;
	public static final int QUEUED_SECTION_HEAD = 1;
	public static final int QUEUED_SECTION_TAIL = 2;
	public static final int ERROR_SECTION = 3;
	public static final int UNHIGHLIGHTED_SECTION = 4;
	
	public List<Integer> getEndOfSentenceOffsets();
	public int getEndOfSentenceOffsetsSize();
	public void addEndOfSentenceOffset(int sentenceOffset);
	public void removeEndOfSentenceOffsetsGreaterThan(int sentenceOffset);
	public int getEndOffset(int sentenceNumber) throws NoOffsetForSentenceNumberException;
	
	public int getCurrentlyViewingSentenceNumber(); // <= lastEvaluatedSentenceNumber
	public int getLastEvaluatedSentenceNumber(); //-1 if there is no highlighting
	public int getLastQueuedSentenceNumber(); //lastQueuedSentenceNumber == lastEvaluatedSentenceNumber iff the queue is empty
	public int getErrorSentenceNumber();
	public void setCurrenlyViewingSentenceNumber(int newSentenceNumber);
	public void setLastEvaluatedSentenceNumber(int newSentenceNumber);
	public void setErrorSentenceNumber(int newSentenceNumber);
	
	public int getCurrentlyViewingSentenceStartOffset();
	public int getCurrentlyViewingSentenceEndOffset();
	public int getEvaluatedSectionEndOffset();
	public int getQueuedSectionEndOffset();
	public int getErrorSentenceStartOffset();
	public int getErrorSentenceEndOffset();

	public void enqueueSentence(String sentence);
	public String dequeueSentence();
	public void removeSentenceQueueTailInclusive(int i);
	
	public boolean isProcessingSentence();
	public void setIsProcessingSentence(boolean processing);
	
	public void incrementDequeueCount();
	public void decrementDequeueCount();
	public int getDequeueCount();
	public void setDequeueCount(int dequeueCount);

	public int getInterruptCount();
	public void setInterruptCount(int newInterruptCount);

	public void addToResponseCache(CoqtopResponse response);
	public int getResponseCacheSize();
	public CoqtopResponse getCachedResponseForSentence(int sentenceNumber) throws NoCachedSentenceException;
	public void removeResponsesFromCacheAtIndicesGreaterThanOrEqualTo(int sentenceNumber);
	
	public int getSection(int offset);

	public int getSentenceNumberForOffset(int offset);

	public int getStateDepthAfterEvaluatingSentenceNumber(int sentenceNumber);
	public int getSentenceNumberToEvaluateForStateDepth(int stateDepth);

	public void incrementInterruptCount();

	public void decrementInterruptCount();

	public boolean processingBackCommand();

	public void setProcessingBackCommand(boolean b);

	public List<String> getSentenceQueue();

	public boolean processingSynchronously();
	public void setProcessingSynchronously(boolean processingSynchronously);

	public void setCurrentResponse(CoqtopResponse response);

	public CoqtopResponse getCurrentResponse();

	public boolean isNavigating();
	public void setIsNavigating(boolean isNavigating);
	
	public CoqtopResponseResponderFactory getCoqtopResponseResponderFactory();
	public void setCoqtopResponseResponderFactory(CoqtopResponseResponderFactory newResponseResponderFactory);

}
