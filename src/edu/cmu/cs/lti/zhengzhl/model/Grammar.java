/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.Direction;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class Grammar {
	private boolean isNaiveInitialized = false;

	// P(stop|x,direction ,has_children) store here
	protected Map<NonTerminal, Double> hasChildStopProbs;

	protected Map<NonTerminal, Double> noChildStopProbs;

	// P(Y|X,direction) store here
	protected Table<NonTerminal, NonTerminal, Double> attachments;

	public Grammar(int initializationMethod, List<List<Token>> sentences) {
		hasChildStopProbs = new HashMap<NonTerminal, Double>();
		noChildStopProbs = new HashMap<NonTerminal, Double>();

		attachments = HashBasedTable.create();

		if (initializationMethod == 0) {
			naiveLazyInitailizaton();
		} else if (initializationMethod == 1) {
			harmonicInitialization(sentences);
		}
	}

	/**
	 * Lazy initialization only initialize when see it during processing
	 */
	protected void naiveLazyInitailizaton() {
		isNaiveInitialized = true;
	}

	protected void harmonicInitialization(List<List<Token>> sentences) {
		System.out.println("Harmonic initialization");

	}

	public double getStopProb(NonTerminal nt) {
		Map<NonTerminal, Double> stopProbs;
		if (nt.hasChild()) {
			stopProbs = hasChildStopProbs;
		} else {
			stopProbs = noChildStopProbs;
		}

		if (stopProbs.containsKey(nt)) {
			return stopProbs.get(nt);
		} else {
			if (isNaiveInitialized) {
				double samplelogProb;
				if (nt.hasChild()) {
					samplelogProb = Math.log(0.25);
				} else {
					samplelogProb = Math.log(0.5);
				}
				stopProbs.put(nt, samplelogProb);
				return samplelogProb;
			} else {
				throw new IllegalArgumentException("Seems we are having OOV in unsupervised setting, how is that possible?");
			}
		}
	}

	public double getNonStopProb(NonTerminal nt) {
		return 1 - getStopProb(nt);
	}

	public double getAttachmentProb(NonTerminal head, NonTerminal child) {
		if (attachments.contains(head, child)) {
			return attachments.get(head, child);
		} else {
			if (isNaiveInitialized) {
				double sampleLogProb = Math.log(0.5);
				attachments.put(head, child, sampleLogProb);
				return sampleLogProb;
			} else {
				throw new IllegalArgumentException("Seems we are having OOV in unsupervised setting, how is that possible?");
			}
		}
	}
}