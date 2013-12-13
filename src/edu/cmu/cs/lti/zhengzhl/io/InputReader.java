/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.lti.zhengzhl.model.Token;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class InputReader {

	public static List<List<Token>> getSentences(File inputFile) throws IOException {
		List<List<Token>> sentences = new ArrayList<List<Token>>();

		FileInputStream fstream = new FileInputStream(inputFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		List<Token> sentence = new ArrayList<Token>();

		String strLine;
		while ((strLine = br.readLine()) != null) {
			if (strLine.trim().isEmpty()) {
				sentence.add(Token.stop());
				sentences.add(sentence);
				sentence = new ArrayList<Token>();
			} else {
				sentence.add(Token.fromConllString(strLine));
			}
		}
		br.close();

		return sentences;
	}

	public static List<String> getLines(File inputFile) throws IOException {
		List<String> lines = new ArrayList<String>();

		FileInputStream fstream = new FileInputStream(inputFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		while ((strLine = br.readLine()) != null) {
			lines.add(strLine);
		}
		br.close();

		return lines;
	}
}
