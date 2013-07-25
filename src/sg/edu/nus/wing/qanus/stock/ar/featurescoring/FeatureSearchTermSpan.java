package sg.edu.nus.wing.qanus.stock.ar.featurescoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;


/**
 * Class implements a feature used to score retrieved passages.
 * 
 * This feature makes use of the span of matching search terms within a passage.
 * For example, suppose the passage has the following search term matches (denoted by X)
 * ..... X ..... X ..... X ......
 * ......a ..... b ...... c .....
 * 
 * a, b, c are position of the matching term within the passage (in terms of words).
 * The span is thus |c-a|.
 * 
 * The score of this feature is given as 
 *   # matching terms / span
 * 
 * This score is guaranteed to be between 0 and 1 inclusive.
 *
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 24Dec2009
 */
public class FeatureSearchTermSpan extends Feature {


	/**
	 * Retrieves the score related to the span of the search term within the passage.
	 *
	 * @param a_SearchStr [in] search terms to look for, only first element is accessed
	 * @param a_Passage [in] passage to score
	 * @return a score from 0 to 1 inclusive. A score closer to 1 means the search terms appear close together within the given passage.
	 */
	public double GetScore(String[] a_SearchStr, String a_Passage) {

		// Sanity check
		if (a_SearchStr == null) return 0;

		
		ArrayList<Integer> l_MatchLocationsInt = new ArrayList<Integer>();
		// Tracks all matched query terms
		HashSet<String> l_MatchedWords = new HashSet<String>();
		// 1. Count occurrences of all search terms in target string
		// Search terms delineated in seach string by a space
		StringTokenizer l_STMatch = new StringTokenizer(a_SearchStr[0]);
		while (l_STMatch.hasMoreTokens()) {
			String l_SearchTerm = l_STMatch.nextToken();
			int l_FoundIndex = -1;
			int l_SearchIndex = 0;
			while ((l_FoundIndex = a_Passage.toLowerCase().indexOf(l_SearchTerm.toLowerCase(), l_SearchIndex)) != -1) {
				// Match found
				l_SearchIndex = l_FoundIndex + 1;
				int l_WordIndex = NumWordsBeforeIndex(a_Passage, l_FoundIndex);
				if (!l_MatchLocationsInt.contains(l_WordIndex)) {
					l_MatchLocationsInt.add(l_WordIndex);
					l_MatchedWords.add(l_SearchTerm.toLowerCase());
				}
			}
		}
		// See how many matches there are, this will be used to normalise the score
		int l_MatchCount = l_MatchLocationsInt.size();
		if (l_MatchCount <= 1) {
			// We cannot calculate a distance if there is only 1 or less elements
			return 0;
		}
		int[] l_MatchLocations = new int[l_MatchCount];
		int i = 0;
		for (Integer l_i : l_MatchLocationsInt) {
			l_MatchLocations[i++] = l_i.intValue();
		}
		// Average the pairwise distance between the different match locations
		Arrays.sort(l_MatchLocations);
		int l_Distance = l_MatchLocations[l_MatchLocations.length - 1] - l_MatchLocations[0];

		// Normalise the score
		double l_ComputedScore = (double) (l_MatchedWords.size() - 1) / l_Distance;
		return l_ComputedScore;

		
	} // end GetScore()


	/**
	 * Calculates the number of words from the start of the sentence, to the current
	 * charcter index position
	 * We assume that words are delimited by the space ' ' character.
	 * @param a_String [in] sentence to search through
	 * @param a_Index [in] character index position
	 * @return number of words from start of sentence to current character index.
	 */
	public int NumWordsBeforeIndex(String a_String, int a_Index) {
		// Sanity check
		if (a_Index >= a_String.length()) {
			return 0;
		}
		int l_Count = 1;
		// Start from 1 because we assume the first character won't be a space
		for (int i = 0; i < a_Index; ++i) {
			if (a_String.charAt(i) == ' ') {
				l_Count++;
			}
		}
		// end for i
		return l_Count;
	} // end NumWordsBeforeIndex()

} // end class FeatureSearchTermSpan

