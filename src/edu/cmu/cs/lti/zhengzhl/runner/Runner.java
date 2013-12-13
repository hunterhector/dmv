/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.runner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.lti.zhengzhl.algorithm.decode.ViterbiParseDecode;
import edu.cmu.cs.lti.zhengzhl.algorithm.learn.InsideOutside;
import edu.cmu.cs.lti.zhengzhl.io.InputReader;
import edu.cmu.cs.lti.zhengzhl.model.Grammar;
import edu.cmu.cs.lti.zhengzhl.model.Token;

/**
 * Run the inside outside
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class Runner {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		List<List<Token>> sentences = InputReader.getSentences(new File("data/train.conll.txt"));
		System.out.println(sentences.size());

		Grammar naiveGrammar = new Grammar(0, sentences);
		InsideOutside io = new InsideOutside(naiveGrammar);
		io.runInsideOutside(sentences);
		Grammar emGrammar = io.getGrammar();

		ViterbiParseDecode decoder = new ViterbiParseDecode(emGrammar);

		PrintWriter writer = new PrintWriter("data/train_output.txt");

		for (List<Token> sentence : sentences) {
			int[] headPostions = decoder.depParse(sentence);

			for (Token token : sentence) {
				token.setPredictedHead(headPostions[token.getId()]);
				if (!token.getIsRoot()) {
					writer.println(token.getPredicted());
				}
			}

			writer.println();
		}

		writer.close();
	}

}
