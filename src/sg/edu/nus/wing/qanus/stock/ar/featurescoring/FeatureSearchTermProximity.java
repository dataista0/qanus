package sg.edu.nus.wing.qanus.stock.ar.featurescoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 *	This feature computes the distance between the occurences of two search strings within
 * a passage.
 *  Typically the search string may span multiple word tokens in the passage.
 * 
 *  To compute the distance, we will compute the midpoint of the span of the two
 * search strings, and find the difference between the two midpoints.
 * 
 *  ... X .. X... X .... Y ... Y
 *           |              |
 *            <------------>
 * 
 *  where X are matches to search string 1 and Y are matches to the other search string.
 *
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 24Dec2009
 */
public class FeatureSearchTermProximity extends Feature {



	/**
	 * Scores the proximity between two strings in a given passage.
	 * The proximity is defined as the distance between the middle of the
	 * span of the strings.
	 * See FeatureSearchTermSpan for a definition fo the "span".
	 *
	 * @param a_SearchStrings [in] array of two strings which we want to measure the proximity of
	 * @param a_Passage [in] the passage to search through
	 * @return a score between 0 and 1 inclusive. A score closer to 1 denotes that the two provided
	 *			strings occur close to each other within the passage.
	 */
	public double GetScore(String[] a_SearchStrings, String a_Passage) {

		// Sanity check
		if (a_SearchStrings == null || a_SearchStrings.length != 2) {
			return 0;
		}

		// Count the number of words within the passage
		StringTokenizer l_ST_Passage = new StringTokenizer(a_Passage);
		int l_NumWordsInSourceString = l_ST_Passage.countTokens();

		// Find the span of each search string within the given passage
		int[] l_Span1 = GetSpan(a_SearchStrings[0], a_Passage);
		int[] l_Span2 = GetSpan(a_SearchStrings[1], a_Passage);

		// Calculate proximity score
		double l_ProximityScore = 0;
		if (l_Span1 != null && l_Span2 != null) {

			// This is the distance between the span of the strings
			l_ProximityScore = Math.abs(((l_Span1[0] + l_Span1[1]) / (double) 2) - ((l_Span2[0] + l_Span2[1]) / (double) 2));

			// We want to normalise the score so that it falls within 0 and 1
			if (l_ProximityScore > 0) {
				l_ProximityScore = (l_NumWordsInSourceString - l_ProximityScore) / (double) l_NumWordsInSourceString;
			} else {
				l_ProximityScore = 0;
			}
		}

		return l_ProximityScore;

	} // end GetScore()
	

	/**
	 * Retrieves an index indicating where the search string spans within the given passage.
	 * For example suppose the search string is "Boston Pops".
	 * If the passage is "The Boston Pops orchestra...", we will return [1, 2] to mean
	 * that the search term is found within words 1 and 2.
	 * If only 1 term is found within the search string, for example if the string is
	 * "Philharmonic orchestra", we will return [4, 4].
	 *
	 * null is returned if no matches of the search string are found within the passage.
	 *
	 * @param a_SearchStr [in] the search terms
	 * @param a_Passage [in] passage to look within
	 * @return array of 2 integers representing the span
	 */
	public int[] GetSpan(String a_SearchStr, String a_Passage) {

		ArrayList<Integer> l_MatchLocationsInt = new ArrayList<Integer>();

		// Tracks all matched query terms
		HashSet<String> l_MatchedWords = new HashSet<String>();

		// Visit each term within the search string
		StringTokenizer l_STMatch = new StringTokenizer(a_SearchStr);
		while (l_STMatch.hasMoreTokens()) {

			// Attempt to find a match for the search string within the passage
			String l_SearchTerm = l_STMatch.nextToken();
			int l_FoundIndex = -1;
			int l_SearchIndex = 0;
			while ((l_FoundIndex = a_Passage.toLowerCase().indexOf(l_SearchTerm.toLowerCase(), l_SearchIndex)) != -1) {

				// Match found -- We store the index of the word where the match is found.
				// We only capture word indices if they have not been stored before
				l_SearchIndex = l_FoundIndex + 1;
				int l_WordIndex = NumWordsBeforeIndex(a_Passage, l_FoundIndex);
				if (!l_MatchLocationsInt.contains(l_WordIndex)) {
					l_MatchLocationsInt.add(l_WordIndex);
					l_MatchedWords.add(l_SearchTerm.toLowerCase());
				}

			} // end while

		} // end while (l_STMatch.hasMoreTokens()..


		// See how many matches there are, this will be used to normalise the score
		int l_MatchCount = l_MatchLocationsInt.size();
		if (l_MatchCount == 0) return null; // No matches, return null
		if (l_MatchCount == 1) {
			// Just a single word match
			int [] l_Result = new int[2];
			l_Result[0] = l_MatchLocationsInt.get(0);
			l_Result[1] = l_MatchLocationsInt.get(0);
			return l_Result;
		}
		// Multi-word span
		int[] l_MatchLocations = new int[l_MatchCount];
		int i = 0;
		for (Integer l_i : l_MatchLocationsInt) {
			l_MatchLocations[i++] = l_i.intValue();
		}

		// The span will be the index of the first word match to the index of the last word match
		Arrays.sort(l_MatchLocations);
		int [] l_Result = new int[2];
		l_Result[0] = l_MatchLocations[0];
		l_Result[1] = l_MatchLocations[l_MatchLocations.length-1];
		return l_Result;

	} // end GetSpan()



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
	
} // end class FeatureSearchTermProximity
