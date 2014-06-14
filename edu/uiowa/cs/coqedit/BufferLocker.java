package edu.uiowa.cs.coqedit;

import org.gjt.sp.jedit.buffer.BufferAdapter;

import org.gjt.sp.jedit.buffer.JEditBuffer;

public class BufferLocker extends BufferAdapter {
	private BasicCoqEditControllerImpl cep;
	private int offsetToLockThrough = 0;
	private boolean thisIsInsertingText = false;
	private boolean thisIsRemovingText = false;
	private String savedText;
	
	public BufferLocker(BasicCoqEditControllerImpl cep) {
		this.cep = cep;	
	}
	
	public void lockUpThroughOffset(int offset) {
		offsetToLockThrough = offset;
	}
	
	@Override
	public void contentInserted(JEditBuffer buffer, int startLine,
		int offset, int numLines, int length) {
		if (!thisIsInsertingText && offset < offsetToLockThrough) {
			thisIsRemovingText = true;
			buffer.remove(offset,length);
			thisIsRemovingText = false;
		}
	}
	
	@Override
	public void preContentRemoved(JEditBuffer buffer, int startLine, 
		int offset, int numLines, int length) {
		if (!thisIsRemovingText && offset < offsetToLockThrough) {
			this.savedText = buffer.getText(offset,length);
		}
	}
	
	@Override
	public void contentRemoved(JEditBuffer buffer, int startLine, 
		int offset, int numLines, int length) {
		if (!thisIsRemovingText && offset < offsetToLockThrough) {
			thisIsInsertingText = true;
			buffer.insert(offset,savedText);
			thisIsInsertingText = false;
		}
	}
}
