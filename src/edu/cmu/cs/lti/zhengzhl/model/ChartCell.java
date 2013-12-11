/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.Direction;
import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.LifeCycle;

/**
 * Represent a general CKY cell, which does not need to conform CNF
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class ChartCell implements Comparable<ChartCell> {

	private NonTerminal nonTerminal;

	private Span span;

	private ChartCell[] children;

	private double marginalLogProb;

	/**
	 * Creating the bottom layer from a terminal symbol, marginal probability is
	 * 1, with no children
	 * 
	 * @param i
	 * @param terminal
	 */
	public ChartCell(int i, String terminal) {
		this.span = new Span(i, i + 1);
		this.nonTerminal = new NonTerminal(terminal, false, LifeCycle.NOT_SEALED, Direction.RIGHT);
		this.children = new ChartCell[0];
		this.marginalLogProb = 0.0;
	}

	/**
	 * Create a cell by aggregate the probability from children and from the
	 * rule
	 * 
	 * @param i
	 * @param j
	 * @param lhs
	 * @param children
	 */
	public ChartCell(int i, int j, RuleLhs lhs, ChartCell... children) {
		this.span = new Span(i, j);
		this.nonTerminal = lhs.getNonTerminal();
		this.children = children;

		this.marginalLogProb = lhs.getLogProb();
		for (ChartCell c : children) {
			this.marginalLogProb += c.getMarginalLogProb();
		}
	}

	public boolean hasNoChildren() {
		if (children.length == 0)
			return true;
		return false;
	}

	public int numberOfChildren() {
		return children.length;
	}

	public ChartCell getSingleChild() {
		if (numberOfChildren() == 1)
			return children[0];
		else
			throw new IllegalAccessError("Can call this iff you have one children only");
	}

	public NonTerminal getNonTerminal() {
		return nonTerminal;
	}

	public Span getSpan() {
		return span;
	}

	public int getFrom() {
		return span.getFrom();
	}

	public int getTo() {
		return span.getTo();
	}

	public ChartCell[] getChildren() {
		return children;
	}

	public double getMarginalLogProb() {
		return marginalLogProb;
	}

	public int getSplit(int index) {
		if (index >= children.length - 1)
			throw new IllegalArgumentException(String.format("Acessing split point %d failed because there are only %d children", index,
					numberOfChildren()));
		else
			return children[index].getTo();
	}

	@Override
	public int compareTo(ChartCell c) {
		int spanCompare = span.compareTo(c.getSpan());
		if (spanCompare != 0) {
			return spanCompare;
		} else {
			return nonTerminal.compareTo(c.getNonTerminal());
		}
	}
}
