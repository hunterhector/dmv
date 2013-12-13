/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.algorithm.learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Joiner;

import edu.cmu.cs.lti.zhengzhl.model.ChartCell;
import edu.cmu.cs.lti.zhengzhl.model.Grammar;
import edu.cmu.cs.lti.zhengzhl.model.NonTerminal;
import edu.cmu.cs.lti.zhengzhl.model.Token;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class InsideOutside {

	Grammar grammar;

	Map<NonTerminal, ChartCell>[][] notSealedChartAlpha;
	Map<NonTerminal, ChartCell>[][] halfSealedChartAlpha;
	Map<NonTerminal, ChartCell>[][] sealedChartAlpha;

	Map<NonTerminal, ChartCell>[][] notSealedChartBeta;
	Map<NonTerminal, ChartCell>[][] halfSealedChartBeta;
	Map<NonTerminal, ChartCell>[][] sealedChartBeta;

	Joiner dollarJoiner = Joiner.on("$");

	double sentencell = 0;
	double aggregateSentencell = 0;
	double previousSentll = Double.NEGATIVE_INFINITY;

	public InsideOutside(Grammar grammar) {
		this.grammar = grammar;
		System.out.println("Inside outside initialized");
	}

	public void runInsideOutside(List<List<Token>> sentences) {
		grammar.cleanStatistics();
		int iter = 1;
		int maxIter = 4;
		while (iter <= maxIter) {
			aggregateSentencell = 0;
			System.out.println("Iteration " + iter);
			iter++;

			int counter = 0;
			for (List<Token> sentence : sentences) {
				int sentLength = sentence.size();
				notSealedChartAlpha = initializeChart(sentLength, notSealedChartAlpha);
				halfSealedChartAlpha = initializeChart(sentLength, halfSealedChartAlpha);
				sealedChartAlpha = initializeChart(sentLength, sealedChartAlpha);

				notSealedChartBeta = initializeChart(sentLength, notSealedChartBeta);
				halfSealedChartBeta = initializeChart(sentLength, halfSealedChartBeta);
				sealedChartBeta = initializeChart(sentLength, sealedChartBeta);

				List<ChartCell> ntSent = fillLexicalCells(sentence);

				firstLayerUnariesAlpha(sentLength);
				calInsideProbability(sentLength);

				// System.err.println("Alpha contents");
				// System.err.println("Not sealed");
				// printChartContent(notSealedChartAlpha);
				// System.err.println("Half sealed");
				// printChartContent(halfSealedChartAlpha);
				// System.err.println("sealed");
				// printChartContent(sealedChartAlpha);

				// System.err.println("Alpha sizes");
				// System.err.println("Not sealed");
				// printChartSize(notSealedChartAlpha);
				// System.err.println("Half sealed");
				// printChartSize(halfSealedChartAlpha);
				// System.err.println("sealed");
				// printChartSize(sealedChartAlpha);

				calOutsideProbability(sentLength);
				firstLayerUnariesBeta(sentLength);

				// System.err.println("Beta contents");
				// System.err.println("Not sealed");
				// printChartContent(notSealedChartBeta);
				// System.err.println("sealed");
				// printChartContent(sealedChartBeta);
				// System.err.println("Half sealed");
				// printChartContent(halfSealedChartBeta);

				// System.err.println("Beta sizes");
				// System.err.println("Not sealed");
				// printChartSize(notSealedChartBeta);
				// System.err.println("Half sealed");
				// printChartSize(halfSealedChartBeta);
				// System.err.println("sealed");
				// printChartSize(sealedChartBeta);

				Map<NonTerminal, Double>[][] sealedExpectedCounts = calExpectedCount(sealedChartAlpha, sealedChartBeta, sentLength);
				Map<NonTerminal, Double>[][] halfSealedExpectedCounts = calExpectedCount(halfSealedChartAlpha, halfSealedChartBeta, sentLength);
				Map<NonTerminal, Double>[][] unSealedExpectedCounts = calExpectedCount(notSealedChartAlpha, notSealedChartBeta, sentLength);

				grammar.updateStopStatistics(ntSent, sealedExpectedCounts, halfSealedExpectedCounts, unSealedExpectedCounts);
				// grammar.updateLeftAttachStatistics(ntSent,
				// halfSealedChartAlpha, halfSealedChartAlpha,
				// halfSealedChartBeta, halfSealedExpectedCounts,
				// sentencell);
				// grammar.updateRightStatistics(ntSent, halfSealedChartAlpha,
				// notSealedChartAlpha, notSealedChartBeta,
				// unSealedExpectedCounts,
				// sentencell);
				aggregateSentencell += sentencell;

				counter++;

				if (counter % 500 == 0) {
					System.err.println(counter + " ... ");
				}
				// if (counter >= 2)
				// break;// play with a few sentence first
			}

			// if (sentencell < previousSentll || (sentencell - previousSentll)
			// / sentencell < 0.1) {
			// break;
			// }

			grammar.reestimate();
			System.out.println("Sum of log likelihood over sentences :" + aggregateSentencell);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<NonTerminal, ChartCell>[][] initializeChart(int sentLength, Map<NonTerminal, ChartCell>[][] chart) {
		chart = new Map[sentLength][sentLength + 1];
		for (int i = 0; i < sentLength; i++) {
			for (int j = i + 1; j <= sentLength; j++) {
				chart[i][j] = new HashMap<NonTerminal, ChartCell>();
			}
		}
		return chart;
	}

	private List<ChartCell> fillLexicalCells(List<Token> tokens) {
		List<ChartCell> nonterminals = new ArrayList<ChartCell>();

		for (int i = 0; i < tokens.size() - 1; i++) {
			Token token = tokens.get(i);
			NonTerminal nt = NonTerminal.fromToken(token);
			ChartCell cell = new ChartCell(i, nt);
			notSealedChartAlpha[i][i + 1].put(nt, cell);
			nonterminals.add(cell);
		}
		// directly put root to the top right to avoid being merged
		NonTerminal rootNotSealedNt = NonTerminal.fromToken(tokens.get(tokens.size() - 1));
		NonTerminal rootHalfSealedNt = rootNotSealedNt.makeHalfSealed();
		int rootPos = tokens.size() - 1;

		halfSealedChartAlpha[rootPos][rootPos + 1].put(rootHalfSealedNt, new ChartCell(rootPos, rootHalfSealedNt));

		return nonterminals; // no oov cases, although it is garunteed here.
	}

	private void firstLayerUnariesAlpha(int sentLength) {
		for (int i = 0; i < sentLength; i++) {
			int j = i + 1;
			// do unary for span (i,j): not seal (right) to half seal (left)
			for (NonTerminal notSealedNt : notSealedChartAlpha[i][j].keySet()) {
				NonTerminal halfSealdNt = notSealedNt.makeHalfSealed();
				halfSealedChartAlpha[i][j].put(halfSealdNt, new ChartCell(i, j, halfSealdNt, grammar.getStopProb(notSealedNt)));
			}

			// do unary for span (i,j) : half seal to seal
			for (NonTerminal halfSealedNt : halfSealedChartAlpha[i][j].keySet()) {
				// Direction for a sealed one is arbitrary. Lefting them all
				NonTerminal sealedNt = halfSealedNt.makeSealed();
				sealedChartAlpha[i][j].put(sealedNt, new ChartCell(i, j, sealedNt, grammar.getStopProb(halfSealedNt)));
			}
		}
	}

	private void firstLayerUnariesBeta(int sentLength) {
		for (int i = 0; i < sentLength; i++) {
			int j = i + 1;
			// do unary for span (i,j) : half seal to seal, reversely
			for (NonTerminal sealedNt : sealedChartBeta[i][j].keySet()) {
				NonTerminal halfSealedNt = sealedNt.fromPreviousLifeCycle();
				double marginalProb = sealedChartBeta[i][j].get(sealedNt).getMarginalLogProb() + grammar.getStopProb(halfSealedNt);
				semiringPlus(halfSealedChartBeta[i][j], marginalProb, halfSealedNt, i, j);
			}

			// do unary for span (i,j): not seal (right) to half seal
			// (left), reversely
			for (NonTerminal halfSealedNt : halfSealedChartBeta[i][j].keySet()) {
				NonTerminal notSealedNt = halfSealedNt.fromPreviousLifeCycle();
				double marginalProb = halfSealedChartBeta[i][j].get(halfSealedNt).getMarginalLogProb() + grammar.getStopProb(notSealedNt);
				semiringPlus(notSealedChartBeta[i][j], marginalProb, notSealedNt, i, j);
			}
		}
	}

	private void calInsideProbability(int sentLength) {
		// the portion not including sentence end
		for (int width = 2; width < sentLength; width++) {
			for (int i = 0; i < sentLength - width; i++) {
				int j = i + width;
				for (int k = i + 1; k < j; k++) {
					// do right attachments : -> and -
					if (!(notSealedChartAlpha[i][k].isEmpty() || sealedChartAlpha[k][j].isEmpty())) {
						Map<NonTerminal, ChartCell> headAlpha = notSealedChartAlpha[i][k];
						Map<NonTerminal, ChartCell> childAlpha = sealedChartAlpha[k][j];
						for (NonTerminal head : headAlpha.keySet()) {
							for (NonTerminal child : childAlpha.keySet()) {
								double attachProb = grammar.getAttachmentProb(head, child);
								double nonStopProb = grammar.getNonStopProb(head);
								double alphaLeft = headAlpha.get(head).getMarginalLogProb();
								double alphaRight = childAlpha.get(child).getMarginalLogProb();
								// mark has child here
								semiringPlus(notSealedChartAlpha[i][j], attachProb + nonStopProb + alphaLeft + alphaRight, getHasChildVersion(head),
										i, j);
							}
						}
					}
					// do left attachments : -> <- and -
					if (!(sealedChartAlpha[i][k].isEmpty() || halfSealedChartAlpha[k][j].isEmpty())) {
						Map<NonTerminal, ChartCell> headAlpha = halfSealedChartAlpha[k][j];
						Map<NonTerminal, ChartCell> childAlpha = sealedChartAlpha[i][k];

						for (NonTerminal head : headAlpha.keySet()) {
							for (NonTerminal child : childAlpha.keySet()) {
								double attachProb = grammar.getAttachmentProb(head, child);
								double nonStopProb = grammar.getNonStopProb(head);
								double alphaRight = headAlpha.get(head).getMarginalLogProb();
								double alphaLeft = childAlpha.get(child).getMarginalLogProb();

								// mark has child here
								semiringPlus(halfSealedChartAlpha[i][j], attachProb + nonStopProb + alphaRight + alphaLeft, getHasChildVersion(head),
										i, j);
							}
						}
					}
				}

				// do unary for span (i,j): not seal (right) to half seal (left)
				for (NonTerminal notSealedNt : notSealedChartAlpha[i][j].keySet()) {
					double marginalProb = notSealedChartAlpha[i][j].get(notSealedNt).getMarginalLogProb() + grammar.getStopProb(notSealedNt);
					NonTerminal halfSealdNt = notSealedNt.makeHalfSealed();
					semiringPlus(halfSealedChartAlpha[i][j], marginalProb, halfSealdNt, i, j);
				}

				// do unary for span (i,j) : half seal to seal
				for (NonTerminal halfSealedNt : halfSealedChartAlpha[i][j].keySet()) {
					double marginalProb = halfSealedChartAlpha[i][j].get(halfSealedNt).getMarginalLogProb() + grammar.getStopProb(halfSealedNt);
					NonTerminal sealedNt = halfSealedNt.makeSealed();
					semiringPlus(sealedChartAlpha[i][j], marginalProb, sealedNt, i, j);
				}
			}
		}
		// the diamond! combine with sentence end symbol
		for (NonTerminal sealedNt : sealedChartAlpha[0][sentLength - 1].keySet()) {
			for (NonTerminal halfSealedStop : halfSealedChartAlpha[sentLength - 1][sentLength].keySet()) {
				double attachProb = grammar.getAttachmentProb(halfSealedStop, sealedNt);
				double alphaLeft = sealedChartAlpha[0][sentLength - 1].get(sealedNt).getMarginalLogProb();
				semiringPlus(halfSealedChartAlpha[0][sentLength], attachProb + alphaLeft, halfSealedStop, 0, sentLength);
			}
		}

		// seal the diamond
		for (NonTerminal halfSealedStop : halfSealedChartAlpha[0][sentLength].keySet()) {
			NonTerminal sealedStop = halfSealedStop.makeSealed();
			double marginalProb = halfSealedChartAlpha[0][sentLength].get(halfSealedStop).getMarginalLogProb();
			sealedChartAlpha[0][sentLength].put(sealedStop, new ChartCell(0, sentLength, sealedStop, marginalProb));
			sentencell = marginalProb;
		}
	}

	/**
	 * Beta table currently share keys with alpha table
	 * 
	 * @param sentLength
	 */
	private void calOutsideProbability(int sentLength) {
		// start from the diamond
		for (NonTerminal halfSealedStop : halfSealedChartAlpha[0][sentLength].keySet()) {
			halfSealedChartBeta[0][sentLength].put(halfSealedStop, new ChartCell(0, sentLength, halfSealedStop, 0));

			// split the stop, get our actual starting points
			for (NonTerminal sealedNt : sealedChartAlpha[0][sentLength - 1].keySet()) {
				double sealedBetaMarginal = halfSealedChartAlpha[sentLength - 1][sentLength].get(halfSealedStop).getMarginalLogProb()
						+ grammar.getAttachmentProb(halfSealedStop, sealedNt);
				sealedChartBeta[0][sentLength - 1].put(sealedNt, new ChartCell(0, sentLength - 1, sealedNt, sealedBetaMarginal));
			}
		}

		// longest downto shortest
		for (int width = sentLength - 1; width >= 2; width--) {
			// for each start
			for (int i = 0; i <= sentLength - 1 - width; i++) {
				int j = i + width;
				// before start, do unary rules for this layer, reversely

				// do unary for span (i,j) : half seal to seal, reversely
				for (NonTerminal sealedNt : sealedChartBeta[i][j].keySet()) {
					NonTerminal halfSealedNt = sealedNt.fromPreviousLifeCycle();
					double marginalProb = sealedChartBeta[i][j].get(sealedNt).getMarginalLogProb() + grammar.getStopProb(halfSealedNt);
					semiringPlus(halfSealedChartBeta[i][j], marginalProb, halfSealedNt, i, j);
				}

				// do unary for span (i,j): not seal (right) to half seal
				// (left), reversely
				for (NonTerminal halfSealedNt : halfSealedChartBeta[i][j].keySet()) {
					NonTerminal notSealedNt = halfSealedNt.fromPreviousLifeCycle();
					double marginalProb = halfSealedChartBeta[i][j].get(halfSealedNt).getMarginalLogProb() + grammar.getStopProb(notSealedNt);
					semiringPlus(notSealedChartBeta[i][j], marginalProb, notSealedNt, i, j);
				}

				// for each split point
				for (int k = i + 1; k < j; k++) {
					// do right attachments : -> and -
					// not sealed on the left, sealed on the right

					// loop the left alpha, so that stop prob will be correct
					for (NonTerminal notSealedNt : notSealedChartAlpha[i][k].keySet()) {
						double parentBetaMarginal = notSealedChartBeta[i][j].get(notSealedNt).getMarginalLogProb();
						double leftAlphaMarginal = notSealedChartAlpha[i][k].get(notSealedNt).getMarginalLogProb();

						// non stop
						double nonStopProb = grammar.getNonStopProb(notSealedNt);

						// loop the right alpha
						for (NonTerminal sealedNt : sealedChartAlpha[k][j].keySet()) {
							double rightAlphaMarginal = sealedChartAlpha[k][j].get(sealedNt).getMarginalLogProb();
							double siblingAttachmentProb = grammar.getAttachmentProb(notSealedNt, sealedNt);

							// the left betas, unsealed
							semiringPlus(notSealedChartBeta[i][k], rightAlphaMarginal + parentBetaMarginal + nonStopProb + siblingAttachmentProb,
									notSealedNt, i, k);
							// the right betas, sealed
							semiringPlus(sealedChartBeta[k][j], leftAlphaMarginal + parentBetaMarginal + nonStopProb + siblingAttachmentProb,
									sealedNt, k, j);
						}
					}

					// do left attachments : -> <- and -
					// half sealed on the right, sealed on the left

					// loop the right alpha
					for (NonTerminal halfSealedNt : halfSealedChartAlpha[k][j].keySet()) {
						double parentBetaMarginal = halfSealedChartBeta[i][j].get(halfSealedNt).getMarginalLogProb();
						double rightAlphaMarginal = halfSealedChartAlpha[k][j].get(halfSealedNt).getMarginalLogProb();

						// non stop
						double nonStopProb = grammar.getNonStopProb(halfSealedNt);

						// loop the left alpha
						for (NonTerminal sealedNt : sealedChartAlpha[i][k].keySet()) {
							double leftAlphaMarginal = sealedChartAlpha[i][k].get(sealedNt).getMarginalLogProb();
							double siblingAttachmentProb = grammar.getAttachmentProb(halfSealedNt, sealedNt);

							// the right betas, half sealed
							semiringPlus(halfSealedChartBeta[k][j], leftAlphaMarginal + parentBetaMarginal + nonStopProb + siblingAttachmentProb,
									halfSealedNt, k, j);

							// the left betas, sealed
							semiringPlus(sealedChartBeta[i][k], rightAlphaMarginal + parentBetaMarginal + nonStopProb + siblingAttachmentProb,
									sealedNt, i, k);
						}
					}
				}
			}
		}
	}

	private Map<NonTerminal, Double>[][] calExpectedCount(Map<NonTerminal, ChartCell>[][] alphaChart, Map<NonTerminal, ChartCell>[][] betaChart,
			int sentLength) {
		Map<NonTerminal, Double>[][] expectedCountChart = new Map[sentLength][sentLength + 1];

		// currently ignoring stop
		for (int i = 0; i < sentLength - 1; i++) {
			for (int j = i + 1; j < sentLength; j++) {
				expectedCountChart[i][j] = new HashMap<NonTerminal, Double>();
				for (Entry<NonTerminal, ChartCell> alphaEntry : alphaChart[i][j].entrySet()) {
					NonTerminal nt = alphaEntry.getKey();
					ChartCell betaCell = betaChart[i][j].get(nt);
					ChartCell alphaCell = alphaEntry.getValue();

					expectedCountChart[i][j].put(nt, alphaCell.getMarginalLogProb() + betaCell.getMarginalLogProb() - sentencell);
				}
			}
		}
		return expectedCountChart;
	}

	private NonTerminal getHasChildVersion(NonTerminal nt) {
		// mark has_child status
		NonTerminal parent;
		if (!nt.hasChild()) {
			parent = nt.getHasChildVersion();
		} else {
			parent = nt;
		}

		return parent;
	}

	private void semiringPlus(Map<NonTerminal, ChartCell> chartCellsAtIJ, double logProb, NonTerminal nt, int i, int j) {
		if (chartCellsAtIJ.containsKey(nt)) {
			chartCellsAtIJ.get(nt).logAggregate(logProb);
		} else {
			chartCellsAtIJ.put(nt, new ChartCell(i, j, nt, logProb));
		}
	}

	public Grammar getGrammar() {
		return grammar;
	}

	private void printChartEmptyness(Map<NonTerminal, ChartCell>[][] chart) {
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				String keySetStr = chart[i][j] != null ? "[" + chart[i][j].keySet().isEmpty() + "]" : "[-]";
				System.err.print(String.format("%d,%d : %s\t", i, j, keySetStr));
			}
			System.err.println();
		}
	}

	private void printChartSize(Map<NonTerminal, ChartCell>[][] chart) {
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				String keySetStr = chart[i][j] != null ? "[" + chart[i][j].keySet().size() + "]" : "[-]";
				System.err.print(String.format("%d,%d : %s\t", i, j, keySetStr));
			}
			System.err.println();
		}
	}

	private void printChartContent(Map<NonTerminal, ChartCell>[][] chart) {
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				String keySetStr = chart[i][j] != null ? "[" + formatChatcells(chart[i][j]) + "]" : "[-]";
				System.err.print(String.format("%d,%d : %s\t", i, j, keySetStr));
			}
			System.err.println();
		}
	}

	private String formatChatcells(Map<NonTerminal, ChartCell> chartCells) {
		return dollarJoiner.join(chartCells.values());
	}
}
