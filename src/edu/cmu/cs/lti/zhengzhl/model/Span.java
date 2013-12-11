/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Defines a range
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class Span implements Comparable<Span> {
	private int from;
	private int to;

	public Span(int from, int to) {
		if (from >= to) {
			throw new IllegalArgumentException("End of span must be larger");
		}

		this.from = from;
		this.to = to;
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(from).append(to).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof NonTerminal))
			return false;

		Span o = (Span) obj;

		return new EqualsBuilder().append(from, o.getFrom()).append(to, o.getTo()).isEquals();
	}

	@Override
	public int compareTo(Span c) {
		if (from < c.from)
			return -1;
		if (from > c.from)
			return 1;
		if (to < c.to)
			return -1;
		if (to > c.to)
			return 1;

		return 0;
	}
}
