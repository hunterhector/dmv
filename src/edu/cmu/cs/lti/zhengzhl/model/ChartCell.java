/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import edu.cmu.cs.lti.zhengzhl.utils.Utils;

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
	public ChartCell(int i, NonTerminal nt) {
		this.span = new Span(i, i + 1);
		this.nonTerminal = nt;
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
	public ChartCell(int i, int j, NonTerminal nt, double logProb, ChartCell... children) {
		if (logProb > 0) {
			throw new IllegalArgumentException("Log probability cannot be larger than 0, at position " + i + " " + j + " " + logProb);
		}
		this.span = new Span(i, j);
		this.nonTerminal = nt;
		this.children = children;
		this.marginalLogProb = logProb;
	}

	public void logAggregate(double logProb) {
		if (logProb > 0)
			throw new IllegalArgumentException("Providing positive log probability");
		marginalLogProb = Utils.logAdd(marginalLogProb, logProb);
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

	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(nonTerminal).append(span).toHashCode();
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ChartCell))
			return false;

		ChartCell cell = (ChartCell) obj;

		return new EqualsBuilder().append(nonTerminal, cell.getNonTerminal()).append(span, cell.getSpan()).isEquals();
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

	public boolean onTheRight(ChartCell c) {
		return c.getFrom() > this.getTo();
	}

	public boolean onTheLeft(ChartCell c) {
		return c.getTo() > this.getFrom();
	}

	@Override
	public String toString() {
		return String.format("[%s]:%.2f", nonTerminal, marginalLogProb);
	}

}
