/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.algorithm.learn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.lti.zhengzhl.model.ChartCell;
import edu.cmu.cs.lti.zhengzhl.model.Grammar;
import edu.cmu.cs.lti.zhengzhl.model.NonTerminal;
import edu.cmu.cs.lti.zhengzhl.model.Token;
import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.Direction;
import edu.cmu.cs.lti.zhengzhl.model.NonTerminal.LifeCycle;

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

	public InsideOutside(Grammar grammar) {
		this.grammar = grammar;
	}

	public void runInsideOutside(List<List<Token>> sentences) {
		for (List<Token> sentence : sentences) {
			int sentLength = sentences.size();
			initializeChart(sentLength, notSealedChartAlpha);
			initializeChart(sentLength, halfSealedChartAlpha);
			initializeChart(sentLength, sealedChartAlpha);
			if (fillLexicalCells(sentence)) {
				firstLayerUnaries(sentLength);
				calInsideProbability(sentLength);
				calOutsideProbability(sentLength);
			}
		}
	}

	private boolean fillLexicalCells(List<Token> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			NonTerminal nt = NonTerminal.fromToken(token.getPos());
			notSealedChartAlpha[i][i + 1].put(nt, new ChartCell(i, nt));
		}
		return true; // no oov cases, although it is garunteed here.
	}

	private void firstLayerUnaries(int sentLength) {
		for (int i = 0; i < sentLength; i++) {
			int j = i + 1;
			// do unary for span (i,j): not seal (right) to half seal (left)
			for (NonTerminal notSealedNt : notSealedChartAlpha[i][j].keySet()) {
				NonTerminal halfSealdNt = new NonTerminal(notSealedNt.getSymbol(), false, LifeCycle.HALF_SEALED, Direction.LEFT);
				halfSealedChartAlpha[i][j].put(halfSealdNt, new ChartCell(i, j, halfSealdNt, grammar.getStopProb(notSealedNt)));
			}

			// do unary for span (i,j) : half seal to seal
			for (NonTerminal halfSealedNt : halfSealedChartAlpha[i][j].keySet()) {
				// Direction for a sealed one is arbitrary. Lefting them all
				NonTerminal sealedNt = new NonTerminal(halfSealedNt.getSymbol(), false, LifeCycle.SEALED, Direction.LEFT);
				sealedChartAlpha[i][j].put(sealedNt, new ChartCell(i, j, sealedNt, grammar.getStopProb(halfSealedNt)));
			}
		}
	}

	private void calInsideProbability(int sentLength) {
		for (int width = 2; width <= sentLength; width++) {
			for (int i = 0; i <= sentLength - width; i++) {
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
								semiringPlus(notSealedChartAlpha[i][j], attachProb + nonStopProb + alphaLeft + alphaRight, head, i, j);
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
								double nonStopProb = 1 - grammar.getStopProb(head);
								double alphaRight = headAlpha.get(head).getMarginalLogProb();
								double alphaLeft = childAlpha.get(child).getMarginalLogProb();

								NonTerminal parent;
								if (!head.hasChild()) {
									parent = NonTerminal.getHasChildVersion(head);
								} else {
									parent = head;
								}

								semiringPlus(halfSealedChartAlpha[i][j], attachProb + nonStopProb + alphaRight + alphaLeft, parent, i, j);
							}
						}
					}
				}

				// do unary for span (i,j): not seal (right) to half seal (left)
				for (NonTerminal notSealedNt : notSealedChartAlpha[i][j].keySet()) {
					NonTerminal halfSealdNt = new NonTerminal(notSealedNt.getSymbol(), false, LifeCycle.HALF_SEALED, Direction.LEFT);
					if (halfSealedChartAlpha[i][j].containsKey(halfSealdNt)) {
						halfSealedChartAlpha[i][j].get(halfSealdNt).aggregate(grammar.getStopProb(notSealedNt));
					} else {
						halfSealedChartAlpha[i][j].put(halfSealdNt, new ChartCell(i, j, halfSealdNt, grammar.getStopProb(notSealedNt)));
					}
				}

				// do unary for span (i,j) : half seal to seal
				for (NonTerminal halfSealedNt : halfSealedChartAlpha[i][j].keySet()) {
					NonTerminal sealedNt = new NonTerminal(halfSealedNt.getSymbol(), false, LifeCycle.SEALED, Direction.LEFT);
					if (sealedChartAlpha[i][j].containsKey(sealedNt)) {
						sealedChartAlpha[i][j].get(sealedNt).aggregate(grammar.getStopProb(halfSealedNt));
					} else {
						sealedChartAlpha[i][j].put(sealedNt, new ChartCell(i, j, sealedNt, grammar.getStopProb(halfSealedNt)));
					}
				}
			}
		}
	}

	private void calOutsideProbability(int sentLength) {
		// longest downto shortest
		for (int width = sentLength; width >= 2; width--) {
			// for each start
			for (int i = 0; i <= sentLength - width; i++) {
				int j = i + width;
				// for each split point
				for (int k = i + 1; k < j; k++) {

				}
			}
		}
	}

	private void semiringPlus(Map<NonTerminal, ChartCell> chartCells, double logProb, NonTerminal nt, int i, int j) {
		if (chartCells.containsKey(nt)) {
			chartCells.get(nt).aggregate(logProb);
		} else {
			chartCells.put(nt, new ChartCell(i, j, nt, logProb));
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeChart(int sentLength, Map<NonTerminal, ChartCell>[][] chart) {
		chart = new Map[sentLength][sentLength + 1];
		for (int i = 0; i < sentLength; i++) {
			for (int j = i + 1; j <= sentLength; j++) {
				chart[i][j] = new HashMap<NonTerminal, ChartCell>();
			}
		}
	}

	public Grammar getGrammar() {
		return grammar;
	}

}
