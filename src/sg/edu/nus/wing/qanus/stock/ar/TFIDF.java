package sg.edu.nus.wing.qanus.stock.ar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class implements basic TF-IDF weighting as described in
 * http://en.wikipedia.org/wiki/Tf–idf
 *
 * This class is used in the following way :
 * 1. Add all documents to be ranked into the class via AddDocument()
 * 2. Retrieve top ranked documents for a particular term using RetrieveTopDocuments()
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class TFIDF {

	// Used to hold all the added documents
	private ArrayList<String> m_DocumentStore;


	/**
	 * Constructor
	 */
	public TFIDF() {
		m_DocumentStore = new ArrayList<String>();
	} // end constructor


	/**
	 * Adds a document to the class for subsequent computation.
	 * @param a_DocumentString [in] the actual string that make up the document.
	 */
	public void AddDocument(String a_DocumentString) {
		m_DocumentStore.add(a_DocumentString);
	} // end AddDocument()



	/**
	 * Retrieve the top ranked documents for a particular term.
	 * The documents will be from one of all those which have been added
	 * via AddDocument()
	 *
	 * Top ranked document for this array determined
	 *  by linear sum of TF-IDF score of each search term. -- TODO
	 *
	 * For efficiency, the top ranked documents for each individual search term
	 * are retrieved first. Amongst the retrieved documents, they are ranked
	 * accordingly to the total TF-IDF score they have across all terms.
	 * This is only an approximation, but helps in efficiency lots.
	 *
	 * @param a_Term [in] array of search terms
	 * @param a_NumTopDocs [in] number of top documents to retrieve
	 * @return ranked array of strings, each representing one document
	 */
	public String[] RetrieveTopDocuments(String[] a_Terms, int a_NumTopDocs) {

		// Compute first the top X documents for each term
		// In this case we just let X be a_NumTopDocs for efficiency reasons.
		// If X is the number of ALL documents we have, then the documents
		// will be truly ranked by the linear sum of the TF-IDF score for each
		// search term.


		// Priority queue to keep track of top documents for various search terms
		PriorityQueue<DocumentScore>[] l_TopDocumentCandidates = new PriorityQueue[a_Terms.length];		
		for (int i = 0; i < a_Terms.length; ++i) {
			l_TopDocumentCandidates[i] = new PriorityQueue<DocumentScore>();
		}


		// Compute for each individual search term
		int l_TermIndex = 0;
		int l_DocumentsWithTerm = 0;
		for (String l_Term : a_Terms) {
			
			l_DocumentsWithTerm = 0;

			// Calculate score for each document
			int l_DocIndex = 0;
			for (String l_Document : m_DocumentStore) {

				Pattern l_Regex = Pattern.compile(l_Term);
				Matcher l_Matcher = l_Regex.matcher(l_Document);

				// Count number of matches
				int l_NumMatch = 0;
				while (l_Matcher.find()) {
					l_NumMatch++;
				} // end while
				if (l_NumMatch > 0) l_DocumentsWithTerm++;

				// Count number of word tokens in the document
				StringTokenizer l_ST = new StringTokenizer(l_Document);

				// Put into priority queue
				double l_TFIDFij = (double) l_NumMatch / l_ST.countTokens();
				l_TopDocumentCandidates[l_TermIndex].add(new DocumentScore(l_DocIndex, l_TFIDFij, l_Document));

				
				l_DocIndex++;

			} // end String l_Document..

			if (l_DocumentsWithTerm == 0) l_DocumentsWithTerm++; // Prevent possible division-by-zero
			double l_IDF = Math.log((double)m_DocumentStore.size()/l_DocumentsWithTerm) / Math.log(2);

			// Adjust every DocumentScore stored within our priority queue with the l_IDF score
			// to get the TF-IDF score
			for (DocumentScore l_DocScore : l_TopDocumentCandidates[l_TermIndex]) {
				l_DocScore.MultiplyToScore(l_IDF);
			} // end for
			
			l_TermIndex++;
			
		} // end for String l_Term...


		// Now sum up scores of the top X documents as described above		
		// -- There cannot be more than a_NumTopDocs * a_Terms.length documents because of
		// -- the way we choose the TopXDocuments from our loop below
		DocumentScore[] l_TopXDocuments = new DocumentScore[a_NumTopDocs * a_Terms.length];
		for (int i = 0; i < l_TopXDocuments.length; ++i) {
			l_TopXDocuments[i] = null;
		}
		int l_CurrEmptySlot = 0;
		for (int i = 0; i < l_TopDocumentCandidates.length; ++i) {

			// Retrieve top X documents for each term and put them together
			for (int j = 0; j < a_NumTopDocs; ++j) {
				DocumentScore l_DocCandidate = l_TopDocumentCandidates[i].remove();
				if (l_DocCandidate != null) {
					if (CheckInArray(l_TopXDocuments,l_DocCandidate)) {
						int l_IndexInArray = GetIndexInArray(l_TopXDocuments, l_DocCandidate);
						l_TopXDocuments[l_IndexInArray].AddToScore(l_DocCandidate.GetScore());
					} else {
						// Make sure array range not exceeded
						if (l_CurrEmptySlot < l_TopXDocuments.length) {
							l_TopXDocuments[l_CurrEmptySlot++] = l_DocCandidate;
						}
					}
				}
			} // end for j
		} // end for i


		// Sort the various top X documents based on the summed-up TFIDF scores
		// Strip away null elements - first count no. of valid (non null) elements
		int l_NumElements = 0;
		for (int i = 0; i < l_TopXDocuments.length; ++i) {
			if (l_TopXDocuments[i] != null) l_NumElements++;
		} // end for i
		DocumentScore[] l_CleanedTopXDocs = new DocumentScore[l_NumElements];
		int l_CurrIndex = 0;
		for (int i = 0; i < l_CleanedTopXDocs.length; ++i) {
			if (l_TopXDocuments[l_CurrIndex] != null) {
				l_CleanedTopXDocs[i] = l_TopXDocuments[l_CurrIndex];
			}
			l_CurrIndex++;

		}
		Arrays.sort(l_CleanedTopXDocs); // Sorted in ascending order


		// Create a result array to return the top X documents in
		//String[] l_ResultArray = new String[(a_NumTopDocs<l_TopXDocuments.length)?a_NumTopDocs:l_TopXDocuments.length];
		String[] l_ResultArray = new String[l_CleanedTopXDocs.length];
		for (int i = 0; i < l_ResultArray.length; ++i) {
			l_ResultArray[i] = l_CleanedTopXDocs[l_CleanedTopXDocs.length-1-i].GetDocText();
		} // end for i

		return l_ResultArray;
		

	} // end RetrieveTopDocuments()


	/**
	 * Verifies if a given object is found within an array.
	 * Object equality is determined by the object's equal() implementation.
	 * 
	 * @param a_Array [in] the array to be checked
	 * @param a_Object [in] the object we want to check on
	 * @return true if the object is found in the array, false otherwise
	 */
	private boolean CheckInArray(Object[] a_Array, Object a_Object) {

		for (int i = 0; i < a_Array.length; ++i) {
			if (a_Object.equals(a_Array[i])) {
				return true;
			}
		} // end for i

		return false;

	} // end CheckInArray()


	/**
	 * Obtain the index of an object in an array.
	 *
	 * @param a_Array [in] the array to check from
	 * @param a_Object [in] the object whose index we want to locate
	 * @return index of object within array, or -1 if not found
	 */
	private int GetIndexInArray(Object[] a_Array, Object a_Object) {

		for (int i = 0; i < a_Array.length; ++i) {
			if (a_Object.equals(a_Array[i])) {
				return i;
			}
		} // end for i

		return -1;

	} // end GetIndexInArray()

	/**
	 * Used to store documents and their associated TFIDF scores in a priority queue
	 * for efficiency and easy bookkeeping.
	 */
	private class DocumentScore implements Comparable {

		private int m_DocID;
		private double m_Score;
		private String m_DocText;

		public DocumentScore(int a_DocID, double a_Score, String a_DocText) {
			m_DocID = a_DocID;
			m_Score = a_Score;
			m_DocText = a_DocText;
		}

		public double GetScore() {
			return m_Score;
		}

		public int GetDocID() {
			return m_DocID;
		}

		public String GetDocText() {
			return m_DocText;
		}

		public void AddToScore(double a_Part) {
			m_Score += a_Part;
		}
		
		public int compareTo(Object o) {
			if (o == null) return -1;
			DocumentScore l_Object = (DocumentScore) o;
			if (m_Score > l_Object.GetScore()) {
				return 1;
			} else if (m_Score == l_Object.GetScore()) {
				return 0;
			} else {
				return -1;
			}
		} // end compareTo()


		@Override
		public boolean equals(Object o) {

			// Check for self-comparison
			if (this == o) {
				return true;
			}

			//use instanceof instead of getClass here for two reasons
			//1. if need be, it can match any supertype, and not just one class;
			//2. it renders an explict check for "that == null" redundant, since
			//it does the check for null already - "null instanceof [type]" always
			//returns false. (See Effective Java by Joshua Bloch.)
			if (!(o instanceof DocumentScore)) {
				return false;
			}
			//Alternative to the above line :
			//if ( aThat == null || aThat.getClass() != this.getClass() ) return false;

			//cast to native object is now safe
			DocumentScore l_Object = (DocumentScore) o;

			//now a proper field-by-field evaluation can be made
			return (m_DocID == l_Object.GetDocID());
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 41 * hash + this.m_DocID;
			return hash;
		}

		/**
		 * Multiply a factor into existing score
		 * @param a_Factor [in] factor to use
		 */
		private void MultiplyToScore(double a_Factor) {
			m_Score = m_Score / a_Factor;
		} // end MultiplyToScore()

	} // end class DocumentScore


} // end class TFIDF
