/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class NonTerminal implements Comparable<NonTerminal> {

	private String symbol;
	private boolean hasChild;

	private LifeCycle lifeCyle;
	private Direction direction;

	public enum Direction {
		LEFT, RIGHT
	}

	public enum LifeCycle {
		NOT_SEALED, HALF_SEALED, SEALED
	}

	public NonTerminal(String symbol, boolean hasChild, LifeCycle lc, Direction d) {
		this.symbol = symbol;
		this.hasChild = hasChild;
		this.lifeCyle = lc;
		this.direction = d;
	}

	public static NonTerminal fromToken(String token) {
		return new NonTerminal(token, false, LifeCycle.NOT_SEALED, Direction.RIGHT);
	}

	public static NonTerminal getHasChildVersion(NonTerminal nt) {
		return new NonTerminal(nt.getSymbol(), true, nt.lifeCyle, nt.direction);
	}

	public String getSymbol() {
		return symbol;
	}

	public boolean hasChild() {
		return hasChild;
	}

	public LifeCycle getLifeCyle() {
		return lifeCyle;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(symbol).append(direction).append(lifeCyle).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof NonTerminal))
			return false;

		NonTerminal o = (NonTerminal) obj;

		return new EqualsBuilder().append(symbol, o.getSymbol()).append(direction, o.getDirection()).append(lifeCyle, o.getLifeCyle()).isEquals();
	}

	@Override
	public int compareTo(NonTerminal nt) {
		return this.symbol.compareTo(nt.symbol);
	}

	public boolean canAttach(boolean onTheRight) {
		if (direction.equals(Direction.RIGHT) && onTheRight) {
			return true;
		}

		if (direction.equals(Direction.LEFT) && !onTheRight) {
			return true;
		}

		return false;
	}

}
