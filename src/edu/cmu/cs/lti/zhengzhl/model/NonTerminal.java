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

	private int id;

	public enum Direction {
		LEFT, RIGHT, NONE
	}

	public enum LifeCycle {
		NOT_SEALED, HALF_SEALED, SEALED
	}

	public NonTerminal(String symbol, boolean hasChild, LifeCycle lc, Direction d, int id) {
		this.symbol = symbol;
		this.hasChild = hasChild;
		this.lifeCyle = lc;
		this.direction = d;
		this.id = id;
	}

	public static NonTerminal fromToken(Token token) {
		return new NonTerminal(token.getPos(), false, LifeCycle.NOT_SEALED, Direction.RIGHT, token.getId());
	}

	public static NonTerminal getHasChildVersion(NonTerminal nt) {
		return new NonTerminal(nt.getSymbol(), true, nt.getLifeCyle(), nt.getDirection(), nt.getId());
	}

	public static NonTerminal makeHalfSealed(NonTerminal nt) {
		if (nt.getLifeCyle().equals(LifeCycle.NOT_SEALED)) {
			return new NonTerminal(nt.getSymbol(), false, LifeCycle.HALF_SEALED, Direction.LEFT, nt.getId());
		} else {
			throw new IllegalArgumentException("Cannot only make not_sealed to half_sealed");
		}
	}

	public static NonTerminal makeSealed(NonTerminal nt) {
		if (nt.getLifeCyle().equals(LifeCycle.HALF_SEALED)) {
			return new NonTerminal(nt.getSymbol(), false, LifeCycle.SEALED, Direction.NONE, nt.getId());
		} else {
			throw new IllegalArgumentException("Cannot only make half_sealed to sealed");
		}
	}

	public int getId() {
		return id;
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
		return new HashCodeBuilder(17, 31).append(symbol).append(direction).append(lifeCyle).append(id).toHashCode();
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

		return new EqualsBuilder().append(symbol, o.getSymbol()).append(direction, o.getDirection()).append(lifeCyle, o.getLifeCyle())
				.append(id, o.getId()).isEquals();
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
