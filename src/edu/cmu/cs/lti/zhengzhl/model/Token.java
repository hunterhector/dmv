/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.model;

import com.google.common.base.Joiner;

/**
 * Use to store the original token while read in the data
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class Token {

	private int id;
	private String form;
	private String lemma;
	private String cpos;
	private String pos;
	private String[] features;
	private int head;
	private String deprel;
	private Integer phead;
	private String pDeprel;

	private int predictedHead;

	private Joiner featureJoiner = Joiner.on("|");

	/**
	 * The complete form of initialization
	 * 
	 * @param id
	 * @param form
	 * @param lemma
	 * @param cpos
	 * @param pos
	 * @param features
	 * @param head
	 * @param deprel
	 * @param phead
	 * @param pDeprel
	 */
	public Token(int id, String form, String lemma, String cpos, String pos, String[] features, int head, String deprel, Integer phead, String pDeprel) {
		this.id = id;
		this.form = form;
		this.lemma = lemma;
		this.cpos = cpos;
		this.pos = pos;
		this.features = features;
		this.head = head;
		this.deprel = deprel;
		this.phead = phead;
		this.pDeprel = pDeprel;
	}

	/**
	 * A simpler way, other fieds are not really used
	 * 
	 * @param id
	 * @param form
	 * @param lemma
	 * @param cpos
	 * @param pos
	 * @param head
	 * @param deprel
	 */
	public Token(int id, String form, String lemma, String cpos, String pos, int head, String deprel) {
		this(id, form, lemma, cpos, pos, new String[0], head, deprel, null, "-");
	}

	public static Token fromConllString(String conllStr) {
		String[] parts = conllStr.split("\\s");
		return new Token(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4], Integer.parseInt(parts[6]), parts[7]);
	}

	/**
	 * Get the original string in the given input
	 * 
	 * @return
	 */
	public String getOriginal() {

		String origin = String.format("%d\t%s\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s", id, form, lemma, cpos, pos, featureJoiner.join(features), head,
				deprel, phead == null ? "-" : phead.toString(), pDeprel);
		return origin;
	}

	/**
	 * Replace the column with predicted input
	 * 
	 * @return
	 */
	public String getPredicted() {
		String predicted = String.format("%d\t%s\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s", id, form, lemma, cpos, pos, featureJoiner.join(features),
				predictedHead, deprel, phead == null ? "-" : phead.toString(), pDeprel);
		return predicted;
	}

	public int getId() {
		return id;
	}

	public String getForm() {
		return form;
	}

	public String getLemma() {
		return lemma;
	}

	public String getCpos() {
		return cpos;
	}

	public String getPos() {
		return pos;
	}

	public String[] getFeatures() {
		return features;
	}

	public int getHead() {
		return head;
	}

	public String getDeprel() {
		return deprel;
	}

	public Integer getPhead() {
		return phead;
	}

	public String getpDeprel() {
		return pDeprel;
	}

	public void setPredictedHead(int headIndex) {
		this.predictedHead = headIndex;
	}
}
