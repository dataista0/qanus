package sg.edu.nus.wing.qanus.stock.ar.featurescoring;

import java.util.HashSet;
import java.util.StringTokenizer;


/**
 * Class implements a feature used to score retrieved passages.
 *
 * We count the number of times words in the search string appear in the given passage.
 * The score is the ratio of this score over the length of the passage.
 * The score will be between 0 and 1 inclusive.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 24Dec2009
 */
public class FeatureSearchTermFrequency extends Feature {

	
	/**
	 * Retrieves the score for the given search string and passage.
	 * @param a_SearchStr [in] the string with the search terms to look for
	 * @param a_Passage [in] the passage to search through
	 * @return a score from 0 to 1 inclusive. A score closer to 1 means that more search terms appear in the passage.
	 */
	public double GetScore(String[] a_SearchStr, String a_Passage) {

		if (a_SearchStr == null) return 0;

		// Tracks all processed query terms
		HashSet<String> l_ProcessedQueryTerms = new HashSet<String>();

		// 1. Count occurrences of all search terms in target string
		// Search terms delineated in seach string by a space
		StringTokenizer l_STMatch = new StringTokenizer(a_SearchStr[0]);
		int l_Count = 0;
		while (l_STMatch.hasMoreTokens()) {

			// We make sure we only process each search term once by making use of the HashSet
			String l_SearchTerm = l_STMatch.nextToken();
			if (l_ProcessedQueryTerms.contains(l_SearchTerm.toLowerCase())) {
				continue;
			}
			l_ProcessedQueryTerms.add(l_SearchTerm.toLowerCase());

			// Try to locate occurences of the search term within the passage
			int l_FoundIndex = -1;
			int l_SearchIndex = 0;
			while ((l_FoundIndex = a_Passage.toLowerCase().indexOf(l_SearchTerm.toLowerCase(), l_SearchIndex)) != -1) {
				// Match found
				l_SearchIndex = l_FoundIndex + 1;
				l_Count++;
			}

		} // end while


		// Determine first length of original passage
		StringTokenizer l_ST_Passage = new StringTokenizer(a_Passage);
		int l_Length = l_ST_Passage.countTokens();


		// Compute the score
		double l_ComputedScore = 0;		
		if (l_Length != 0) {
			l_ComputedScore = l_Count / (double) l_Length;
		} else {
			// If passage has no words, then it is logical that we return a 0 for the score
			l_ComputedScore = 0;
		}
		return l_ComputedScore;

	} // end GetScore()



} // end class FeatureSearchTermFrequency
