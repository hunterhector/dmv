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

	private boolean isRoot = false;

	private NonTerminal previousLifeCycleNt = null;

	public enum Direction {
		LEFT, RIGHT, NONE
	}

	public enum LifeCycle {
		NOT_SEALED, HALF_SEALED, SEALED
	}

	public NonTerminal(String symbol, boolean hasChild, LifeCycle lc, Direction d, int id, boolean isRoot) {
		this.symbol = symbol;
		this.hasChild = hasChild;
		this.lifeCyle = lc;
		this.direction = d;
		this.id = id;
		this.isRoot = isRoot;
	}

	public NonTerminal(String symbol, boolean hasChild, LifeCycle lc, Direction d, int id) {
		this(symbol, hasChild, lc, d, id, false);
	}

	public static NonTerminal fromToken(Token token) {
		if (token.getIsRoot()) {
			return new NonTerminal(token.getPos(), false, LifeCycle.NOT_SEALED, Direction.RIGHT, token.getId(), true);
		}

		return new NonTerminal(token.getPos(), false, LifeCycle.NOT_SEALED, Direction.RIGHT, token.getId());
	}

	/**
	 * HasChild version will share the same hash code and equals(), so that they
	 * will be matched if put into a map
	 * 
	 * @return
	 */
	public NonTerminal getHasChildVersion() {
		NonTerminal after = new NonTerminal(this.getSymbol(), true, this.getLifeCyle(), this.getDirection(), this.getId());
		after.setPrevoiusLifeCycle(this.fromPreviousLifeCycle());
		return after;
	}

	public NonTerminal makeHalfSealed() {
		if (this.getLifeCyle().equals(LifeCycle.NOT_SEALED)) {
			NonTerminal after = new NonTerminal(this.getSymbol(), false, LifeCycle.HALF_SEALED, Direction.LEFT, this.getId());
			after.setPrevoiusLifeCycle(this);
			return after;
		} else {
			throw new IllegalArgumentException("Cannot only make not_sealed to half_sealed");
		}
	}

	public NonTerminal makeSealed() {
		if (this.getLifeCyle().equals(LifeCycle.HALF_SEALED)) {
			NonTerminal after = new NonTerminal(this.getSymbol(), false, LifeCycle.SEALED, Direction.NONE, this.getId());
			after.setPrevoiusLifeCycle(this);
			return after;
		} else {
			System.err.println(this);
			throw new IllegalArgumentException("Cannot only make half_sealed to sealed");
		}
	}

	private void setPrevoiusLifeCycle(NonTerminal nt) {
		previousLifeCycleNt = nt;
	}

	public NonTerminal fromPreviousLifeCycle() {
		return previousLifeCycleNt;
	}

	public boolean getIsRoot() {
		return isRoot;
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

	@Override
	public String toString() {
		return String.format("%d:%s-(%s,%s)", id, symbol, direction, lifeCyle);
	}

}
