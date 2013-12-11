/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class RuleLhs {
	private NonTerminal nonTerminal;
	private double logProb;

	public RuleLhs(NonTerminal nonTerminal, double logProb) {
		this.nonTerminal = nonTerminal;
		this.logProb = logProb;
	}

	public NonTerminal getNonTerminal() {
		return nonTerminal;
	}

	public double getLogProb() {
		return logProb;
	}

	public String toString() {
		return nonTerminal + " :[" + logProb + "]";

	}
}
