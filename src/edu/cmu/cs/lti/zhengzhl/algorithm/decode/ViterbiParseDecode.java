/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.algorithm.decode;

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
public class ViterbiParseDecode {
	Grammar grammar;

	Map<NonTerminal, ChartCell>[][] notSealedChartAlpha;
	Map<NonTerminal, ChartCell>[][] halfSealedChartAlpha;
	Map<NonTerminal, ChartCell>[][] sealedChartAlpha;

	Joiner dollarJoiner = Joiner.on("$");

	public ViterbiParseDecode(Grammar grammar) {
		this.grammar = grammar;
		System.out.println("Viterbi parser initialized");
	}

	public int[] depParse(List<Token> sentence) {
		int sentLength = sentence.size();
		notSealedChartAlpha = initializeChart(sentLength, notSealedChartAlpha);
		halfSealedChartAlpha = initializeChart(sentLength, halfSealedChartAlpha);
		sealedChartAlpha = initializeChart(sentLength, sealedChartAlpha);

		fillLexicalCells(sentence);
		firstLayerUnariesAlpha(sentLength);

		runViterbi(sentLength);

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

		return recover(sentLength);
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

	public void runViterbi(int sentLength) {
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
										i, j, childAlpha.get(child), headAlpha.get(head));
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
										i, j, childAlpha.get(child), headAlpha.get(head));
							}
						}
					}
				}

				// do unary for span (i,j): not seal (right) to half seal (left)
				for (NonTerminal notSealedNt : notSealedChartAlpha[i][j].keySet()) {
					double marginalProb = notSealedChartAlpha[i][j].get(notSealedNt).getMarginalLogProb() + grammar.getStopProb(notSealedNt);
					NonTerminal halfSealdNt = notSealedNt.makeHalfSealed();
					semiringPlus(halfSealedChartAlpha[i][j], marginalProb, halfSealdNt, i, j, notSealedChartAlpha[i][j].get(notSealedNt));
				}

				// do unary for span (i,j) : half seal to seal
				for (NonTerminal halfSealedNt : halfSealedChartAlpha[i][j].keySet()) {
					double marginalProb = halfSealedChartAlpha[i][j].get(halfSealedNt).getMarginalLogProb() + grammar.getStopProb(halfSealedNt);
					NonTerminal sealedNt = halfSealedNt.makeSealed();
					semiringPlus(sealedChartAlpha[i][j], marginalProb, sealedNt, i, j, halfSealedChartAlpha[i][j].get(halfSealedNt));
				}
			}
		}
		// the diamond! combine with sentence end symbol
		for (NonTerminal sealedNt : sealedChartAlpha[0][sentLength - 1].keySet()) {
			for (NonTerminal halfSealedStop : halfSealedChartAlpha[sentLength - 1][sentLength].keySet()) {
				double attachProb = grammar.getAttachmentProb(halfSealedStop, sealedNt);
				double alphaLeft = sealedChartAlpha[0][sentLength - 1].get(sealedNt).getMarginalLogProb();
				semiringPlus(halfSealedChartAlpha[0][sentLength], attachProb + alphaLeft, halfSealedStop, 0, sentLength,
						sealedChartAlpha[0][sentLength - 1].get(sealedNt));
			}
		}

		// seal the diamond
		for (NonTerminal halfSealedStop : halfSealedChartAlpha[0][sentLength].keySet()) {
			NonTerminal sealedStop = halfSealedStop.makeSealed();
			double marginalProb = halfSealedChartAlpha[0][sentLength].get(halfSealedStop).getMarginalLogProb();
			sealedChartAlpha[0][sentLength].put(sealedStop, new ChartCell(0, sentLength, sealedStop, marginalProb));
		}
	}

	public int[] recover(int sentLength) {
		int[] headPosition = new int[sentLength];

		for (Entry<NonTerminal, ChartCell> entry : halfSealedChartAlpha[0][sentLength].entrySet()) {
			ChartCell stopCell = entry.getValue();

			ChartCell parent = stopCell;

			while (!parent.hasNoChildren()) {
				if (parent.numberOfChildren() == 1) {
					ChartCell child = parent.getSingleChild();
					if (child.getNonTerminal().getId() != parent.getNonTerminal().getId()) {
						// System.out.println(child + " " + parent);
						// System.out.println(child.getNonTerminal().getId() +
						// " " + parent.getNonTerminal().getId());
						headPosition[child.getNonTerminal().getId()] = parent.getNonTerminal().getId();
					}
					parent = child;
				} else if (parent.numberOfChildren() == 2) {
					ChartCell child = parent.getChildren()[0];
					// System.out.println(child + " " + parent);
					// System.out.println(child.getNonTerminal().getId() + " " +
					// parent.getNonTerminal().getId());
					headPosition[child.getNonTerminal().getId()] = parent.getNonTerminal().getId();
					parent = parent.getChildren()[1];
				}
			}
		}
		return headPosition;
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

	private void semiringPlus(Map<NonTerminal, ChartCell> chartCellsAtIJ, double logProb, NonTerminal nt, int i, int j, ChartCell... children) {
		if (chartCellsAtIJ.containsKey(nt)) {
			if (logProb > chartCellsAtIJ.get(nt).getMarginalLogProb()) {
				chartCellsAtIJ.put(nt, new ChartCell(i, j, nt, logProb, children));
			}
		} else {
			chartCellsAtIJ.put(nt, new ChartCell(i, j, nt, logProb, children));
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
