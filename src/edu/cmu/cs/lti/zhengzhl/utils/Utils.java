/**
 * 
 */
package edu.cmu.cs.lti.zhengzhl.utils;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public class Utils {
	// https://facwiki.cs.byu.edu/nlp/index.php/Log_Domain_Computations
	public static double logAdd(double logX, double logY) {
		// 1. make X the max
		if (logY > logX) {
			double temp = logX;
			logX = logY;
			logY = temp;
		}
		// 2. now X is bigger
		if (logX == Double.NEGATIVE_INFINITY) {
			return logX;
		}
		// 3. how far "down" (think decibels) is logY from logX?
		// if it's really small (20 orders of magnitude smaller), then ignore
		double negDiff = logY - logX;
		if (negDiff < -20) {
			return logX;
		}
		// 4. otherwise use some nice algebra to stay in the log domain
		// (except for negDiff)
		return logX + Math.log1p(Math.exp(negDiff));
	}

	public static double logMinus(double x, double y) {
		if (x <= y)
			throw new IllegalArgumentException("Cannot compute log for negative number");
		if (y == Double.NEGATIVE_INFINITY)
			return x;
		return x + Math.log1p(-Math.exp(y - x));
	}

}
