/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.runner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.cmu.cs.lti.zhengzhl.io.InputReader;
import edu.cmu.cs.lti.zhengzhl.model.Token;

/**
 * Run the inside outside
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class TrainRunner {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		List<List<Token>> sentences = InputReader.getSentences(new File("data/train.conll.txt"));
		System.out.println(sentences.size());
		for (List<Token> sentence : sentences) {

		}
	}

}
