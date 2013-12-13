/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.Direction;
import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.LifeCycle;
import edu.cmu.cs.lti.zhengzhl.utils.Utils;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class Grammar {
	private boolean isNaiveInitialized = false;

	// P(stop|x,direction ,has_children) store here
	protected Table<String, Direction, Double> hasChildStopProbs;

	protected Table<String, Direction, Double> noChildStopProbs;

	// P(Y|X,left) store here
	protected Table<String, String, Double> leftAttachments;

	protected Table<String, String, Double> rightAttachments;

	// use to reestimate stop
	private Table<String, Direction, Double> hasChildStopNominator;

	private Table<String, Direction, Double> hasChildStopDenominator;

	private Table<String, Direction, Double> noChildStopNominator;

	private Table<String, Direction, Double> noChildStopDenominator;

	// use to reestimate left attach
	private Table<String, String, Double> leftAttachNominator;
	private Table<String, String, Double> leftAttachDenominator;

	// use to reestimate right attach
	private Table<String, String, Double> rightAttachNominator;
	private Table<String, String, Double> rightAttachDenominator;

	public Grammar(int initializationMethod, List<List<Token>> sentences) {
		hasChildStopProbs = HashBasedTable.create();
		noChildStopProbs = HashBasedTable.create();

		leftAttachments = HashBasedTable.create();
		rightAttachments = HashBasedTable.create();

		if (initializationMethod == 0) {
			naiveLazyInitailizaton();
		} else if (initializationMethod == 1) {
			harmonicInitialization(sentences);
		}
	}

	/**
	 * Make sure each time start with a fresh statistics
	 */
	public void cleanStatistics() {
		hasChildStopNominator = HashBasedTable.create();
		hasChildStopDenominator = HashBasedTable.create();
		noChildStopNominator = HashBasedTable.create();
		noChildStopDenominator = HashBasedTable.create();
		leftAttachNominator = HashBasedTable.create();
		leftAttachDenominator = HashBasedTable.create();
		rightAttachNominator = HashBasedTable.create();
		rightAttachDenominator = HashBasedTable.create();
	}

	public void reestimate() {
		for (Cell<String, Direction, Double> cell : hasChildStopNominator.cellSet()) {
			double nominator = cell.getValue();
			double denominator = hasChildStopDenominator.get(cell.getRowKey(), cell.getColumnKey());

			if (nominator > denominator) {
				hasChildStopProbs.put(cell.getRowKey(), cell.getColumnKey(), -0.0001);
			} else
				hasChildStopProbs.put(cell.getRowKey(), cell.getColumnKey(), nominator - denominator);
		}

		for (Cell<String, Direction, Double> cell : noChildStopNominator.cellSet()) {
			double nominator = cell.getValue();
			double denominator = noChildStopDenominator.get(cell.getRowKey(), cell.getColumnKey());

			if (nominator > denominator) {
				noChildStopProbs.put(cell.getRowKey(), cell.getColumnKey(), -0.0001);
			} else
				noChildStopProbs.put(cell.getRowKey(), cell.getColumnKey(), nominator - denominator);
		}

		for (Cell<String, String, Double> cell : leftAttachNominator.cellSet()) {
			double nominator = cell.getValue();
			double denominator = leftAttachDenominator.get(cell.getRowKey(), cell.getColumnKey());
			System.out.println("Update " + cell.getRowKey() + " " + cell.getColumnKey());
			leftAttachments.put(cell.getRowKey(), cell.getColumnKey(), nominator - denominator);
		}

		for (Cell<String, String, Double> cell : rightAttachNominator.cellSet()) {
			double nominator = cell.getValue();
			double denominator = rightAttachDenominator.get(cell.getRowKey(), cell.getColumnKey());
			System.out.println("Update right " + cell.getRowKey() + " " + cell.getColumnKey());
			rightAttachments.put(cell.getRowKey(), cell.getColumnKey(), nominator - denominator);
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
		if (nt.getIsRoot()) {
			return 0;
		}

		Table<String, Direction, Double> stopProbs;
		if (nt.hasChild()) {
			stopProbs = hasChildStopProbs;
		} else {
			stopProbs = noChildStopProbs;
		}

		if (stopProbs.contains(nt.getSymbol(), nt.getDirection())) {
			return stopProbs.get(nt.getSymbol(), nt.getDirection());
		} else {
			if (isNaiveInitialized) {
				double samplelogProb;
				if (nt.hasChild()) {
					samplelogProb = Math.log(0.25);
				} else {
					samplelogProb = Math.log(0.5);
				}
				stopProbs.put(nt.getSymbol(), nt.getDirection(), samplelogProb);
				return samplelogProb;
			} else {
				throw new IllegalArgumentException("Seems we are having OOV in unsupervised setting, how is that possible?");
			}
		}
	}

	public double getNonStopProb(NonTerminal nt) {
		return Utils.logMinus(0, getStopProb(nt));
	}

	public double getAttachmentProb(NonTerminal head, NonTerminal child) {
		if (head.getLifeCyle().equals(LifeCycle.SEALED) || !child.getLifeCyle().equals(LifeCycle.SEALED)) {
			throw new IllegalArgumentException("Head must not be sealed and child must be sealed");
		}

		// System.out.println(head + " request " + child);

		return head.getDirection().equals(Direction.LEFT) ? getAttachmentProb(head.getSymbol(), child.getSymbol(), leftAttachments)
				: getAttachmentProb(head.getSymbol(), child.getSymbol(), rightAttachments);
	}

	private double getAttachmentProb(String headSymbol, String childSymbol, Table<String, String, Double> attachments) {
		if (attachments.contains(headSymbol, childSymbol)) {
			return attachments.get(headSymbol, childSymbol);
		} else {
			if (isNaiveInitialized) {
				double sampleLogProb = Math.log(0.5);
				attachments.put(headSymbol, childSymbol, sampleLogProb);
				return sampleLogProb;
			} else {
				throw new IllegalArgumentException("Seems we are having OOV in unsupervised setting, how is that possible?");
			}
		}
	}

	public void updateRightStatistics(List<ChartCell> sentence, Map<NonTerminal, ChartCell>[][] sealedChartAlpha,
			Map<NonTerminal, ChartCell>[][] notSealedChartAlpha, Map<NonTerminal, ChartCell>[][] notSealedChartBeta,
			Map<NonTerminal, Double>[][] unSealedExpectedCounts, double sentll) {
		int sentLength = sentence.size();
		for (ChartCell cell : sentence) {
			int loc_l = cell.getFrom();
			int loc_r = cell.getTo();

			// object creating might be slow, could refactor to pass in table
			NonTerminal unSealedHead = cell.getNonTerminal();

			String symbol = unSealedHead.getSymbol();

			for (int i = 0; i <= loc_l; i++) {
				for (int j = loc_r + 1; j < sentLength; j++) {
					for (String rightAttachCanddidateSymbol : rightAttachments.row(symbol).keySet()) {
						// cal w_s
						if (notSealedChartBeta[i][j].containsKey(unSealedHead)) {
							double outsideProb = notSealedChartBeta[i][j].get(unSealedHead).getMarginalLogProb();
							double attachProb = rightAttachments.get(symbol, rightAttachCanddidateSymbol);

							for (int k = i + 1; k < j; k++) {

								double nonStopProb;
								if (unSealedHead.getId() - 1 != k) {
									nonStopProb = getNonStopProb(unSealedHead.getHasChildVersion());
								} else {
									nonStopProb = getNonStopProb(unSealedHead);
								}

								if (notSealedChartAlpha[i][k].containsKey(unSealedHead)) {
									double insideHeadProb = notSealedChartAlpha[i][k].get(unSealedHead).getMarginalLogProb();
									for (int rightWordIndex = k; rightWordIndex < j; rightWordIndex++) {
										ChartCell rightWord = sentence.get(rightWordIndex);
										NonTerminal rightWordSealed = rightWord.getNonTerminal().makeHalfSealed().makeSealed();

										if (sealedChartAlpha[k][j].containsKey(rightWordSealed)) {
											double rightWordInsideProb = sealedChartAlpha[k][j].get(rightWordSealed).getMarginalLogProb();
											aggregateTable(rightAttachNominator, symbol, rightAttachCanddidateSymbol, nonStopProb + attachProb
													+ rightWordInsideProb + insideHeadProb + outsideProb - sentll);
										}
									}
								}
							}
						}
					}
				}
			}

		}
	}

	public void updateLeftAttachStatistics(List<ChartCell> sentence, Map<NonTerminal, ChartCell>[][] halfSealedChartAlpha,
			Map<NonTerminal, ChartCell>[][] sealedChartAlpha, Map<NonTerminal, ChartCell>[][] halfSealedChartBeta,
			Map<NonTerminal, Double>[][] halfSealedExpectedCounts, double sentll) {
		int sentLength = sentence.size();
		for (ChartCell cell : sentence) {
			int loc_l = cell.getFrom();
			int loc_r = cell.getTo();

			// object creating might be slow, could refactor to pass in table
			NonTerminal unSealedHead = cell.getNonTerminal();
			NonTerminal halfSealedHead = unSealedHead.makeHalfSealed();

			String symbol = unSealedHead.getSymbol();

			for (int i = 0; i < loc_l; i++) {
				for (int j = loc_r; j < sentLength; j++) {
					for (String leftAttachCandidiateSymbol : leftAttachments.row(symbol).keySet()) {
						// cal w_s
						if (halfSealedChartBeta[i][j].containsKey(halfSealedHead)) {
							double outsideProb = halfSealedChartBeta[i][j].get(halfSealedHead).getMarginalLogProb();
							double attachProb = leftAttachments.get(symbol, leftAttachCandidiateSymbol);

							for (int k = i + 1; k < j; k++) {
								if (halfSealedChartAlpha[k][j].containsKey(halfSealedHead)) {
									double insideHeadProb = halfSealedChartAlpha[k][j].get(halfSealedHead).getMarginalLogProb();

									double nonStopProb;
									if (halfSealedHead.getId() != k) {
										nonStopProb = getNonStopProb(halfSealedHead.getHasChildVersion());
									} else {
										nonStopProb = getNonStopProb(halfSealedHead);
									}

									for (int leftWordIndex = i; leftWordIndex < k; leftWordIndex++) {
										ChartCell leftWord = sentence.get(leftWordIndex);
										NonTerminal leftWordSealed = leftWord.getNonTerminal().makeHalfSealed().makeSealed();
										if (sealedChartAlpha[i][k].containsKey(leftWordSealed)) {
											double leftWordInside = sealedChartAlpha[i][k].get(leftWordSealed).getMarginalLogProb();
											aggregateTable(leftAttachNominator, symbol, leftAttachCandidiateSymbol, nonStopProb + attachProb
													+ leftWordInside + insideHeadProb + outsideProb - sentll);
										}
									}
								}
							}
						}

						// denominator
						aggregateTable(leftAttachDenominator, symbol, leftAttachCandidiateSymbol, halfSealedExpectedCounts[i][j].get(halfSealedHead));
					}
				}
			}
		}
	}

	public void updateStopStatistics(List<ChartCell> sentence, Map<NonTerminal, Double>[][] sealedExpectedCounts,
			Map<NonTerminal, Double>[][] halfSealedExpectedCounts, Map<NonTerminal, Double>[][] unSealedExpectedCounts) {
		int sentLength = sentence.size();
		for (ChartCell cell : sentence) {
			int loc_l = cell.getFrom();
			int loc_r = cell.getTo();

			// object creating might be slow, could refactor to pass in table
			NonTerminal unSealedHead = cell.getNonTerminal();
			NonTerminal halfSealedHead = unSealedHead.makeHalfSealed();
			NonTerminal sealedHead = halfSealedHead.makeSealed();

			// symbol is shared
			String symbol = unSealedHead.getSymbol();

			for (int j = loc_r; j < sentLength; j++) {
				for (int i = 0; i < loc_l; i++) {
					if (sealedExpectedCounts[i][j].containsKey(sealedHead)) {
						aggregateTable(hasChildStopNominator, symbol, Direction.LEFT, sealedExpectedCounts[i][j].get(sealedHead));
					}

					if (halfSealedExpectedCounts[i][j].containsKey(halfSealedHead)) {
						aggregateTable(hasChildStopDenominator, symbol, Direction.LEFT, halfSealedExpectedCounts[i][j].get(halfSealedHead));
					}
				}

				int i = loc_l;
				if (sealedExpectedCounts[i][j].containsKey(sealedHead)) {
					aggregateTable(noChildStopNominator, symbol, Direction.LEFT, sealedExpectedCounts[i][j].get(sealedHead));
				}

				if (halfSealedExpectedCounts[i][j].containsKey(halfSealedHead)) {
					aggregateTable(noChildStopDenominator, symbol, Direction.LEFT, halfSealedExpectedCounts[i][j].get(halfSealedHead));
				}

				// the rest apply to j == loc_r
				if (j == loc_r) {
					if (halfSealedExpectedCounts[i][j].containsKey(halfSealedHead)) {
						aggregateTable(noChildStopNominator, symbol, Direction.RIGHT, halfSealedExpectedCounts[i][j].get(halfSealedHead));
					}

					if (unSealedExpectedCounts[i][j].containsKey(unSealedHead)) {
						aggregateTable(noChildStopDenominator, symbol, Direction.RIGHT, unSealedExpectedCounts[i][j].get(unSealedHead));
					}
					continue;
				}

				// the rest apply to j > loc_r
				if (halfSealedExpectedCounts[i][j].containsKey(halfSealedHead)) {
					aggregateTable(hasChildStopNominator, symbol, Direction.RIGHT, halfSealedExpectedCounts[i][j].get(halfSealedHead));
				}

				if (unSealedExpectedCounts[i][j].containsKey(unSealedHead)) {
					aggregateTable(hasChildStopDenominator, symbol, Direction.RIGHT, unSealedExpectedCounts[i][j].get(unSealedHead));
				}
			}
		}
	}

	private <A extends Object, B extends Object> void aggregateTable(Table<A, B, Double> table, A key1, B key2, double logProb) {
		if (table.contains(key1, key2)) {
			table.put(key1, key2, Utils.logAdd(table.get(key1, key2), logProb));
		} else {
			table.put(key1, key2, logProb);
		}
	}
}