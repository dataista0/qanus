package sg.edu.nus.wing.qanus.stock.ar.featurescoring;

import java.util.HashMap;
import java.util.StringTokenizer;



/**
 * Class implements a feature used to score retrieved passages.
 *
 * The count of the occurences of the search terms within a given passage is computed,
 * divided over the length of the search string. This allows us to return a value
 * between 0 and 1.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 24Dec2009
 */
public class FeatureSearchTermCoverage extends Feature {



	/**
	 * Computes the feature score for a given passage and query.
	 * 
	 * @param a_SearchString [in] array of strings of search terms. only first element is accessed.
	 * @param a_Passage [in] passage to search through
	 * @return a score between 0 and 1 inclusive. A score closer to 1 means that more of the search terms appear in the passage.
	 */
	public double GetScore(String[] a_SearchString, String a_Passage) {

		// Sanity check
		if (a_SearchString == null) return 0;

		// Count occurrences of all search terms in target string
		// Search terms delineated in seach string by a space
		StringTokenizer l_STMatch = new StringTokenizer(a_SearchString[0]);
		int l_NumSearchTerms = l_STMatch.countTokens();

		// Walk  through all search terms, and check if they occur within the given passage
		HashMap<String, Integer> l_SearchTermOccurence = new HashMap<String, Integer>();
		while (l_STMatch.hasMoreTokens()) {
			String l_SearchTerm = l_STMatch.nextToken();
			// Don't process repeated search terms
			if (l_SearchTermOccurence.containsKey(l_SearchTerm.toLowerCase())) {
				continue;
			}
			// Check if search term appears in passage
			// If it does, store into hash map
			if (a_Passage.toLowerCase().indexOf(l_SearchTerm.toLowerCase(), 0) != -1) {
				// Match found
				l_SearchTermOccurence.put(l_SearchTerm.toLowerCase(), new Integer(1));
			}
		}

		// Score the passage
		// Perfect score of 1 is when all search terms occur within passage
		// Score of 0 is when no search terms occur within passage
		double l_ComputedScore = 0;
		if (l_NumSearchTerms != 0) {
			l_ComputedScore = l_SearchTermOccurence.size() / (double) l_NumSearchTerms;
		} else {
			// If search query has no words, then it is logical that we return a 0 for the score
			l_ComputedScore = 0;
		}
		return l_ComputedScore;

	} // end GetScore()


} // end class FeatureSearchTermCoverage

