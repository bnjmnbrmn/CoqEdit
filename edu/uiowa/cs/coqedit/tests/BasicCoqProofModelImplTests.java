/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.cs.coqedit.tests;

import edu.uiowa.cs.coqedit.BasicCoqEditModel;
import edu.uiowa.cs.coqedit.BasicCoqEditModelImpl;
import java.util.List;
import java.util.Random;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author bnjmnbrmn
 */
public class BasicCoqProofModelImplTests {
	
	static BasicCoqEditModel m;
	public static void main(String[] args) {
		m = new BasicCoqEditModelImpl(null);
		test2();
	}
	
	static void test1() {
		pm();
		for (int i = 0; i < 40; i++) {
			m.addEndOfSentenceOffset(5+i*5);
			pm();
		}
	}
	
	static void test2() {
		int j = 0;
		
		Random gen = new Random(System.currentTimeMillis());
		for (int i = 0; i < 40; i++) {
			j = j + gen.nextInt(500) + 2;
			m.addEndOfSentenceOffset(j);
			if (j % 7 == 0) {
				pm();
				List<Integer> offsets = m.getEndOfSentenceOffsets();
				int top = offsets.get(offsets.size() - 1);
				int offset = gen.nextInt(top * 3/2);
				System.out.println("Removing offsets greater than " + offset);
				m.removeEndOfSentenceOffsetsGreaterThan(offset);
				pm();
				System.out.println();
			}
		}
		pm();
	}
	
	static void pm() {
		for (Integer eoso : m.getEndOfSentenceOffsets()) {
			System.out.print(eoso + " ");
			
		}
		System.out.println();
	}
}
