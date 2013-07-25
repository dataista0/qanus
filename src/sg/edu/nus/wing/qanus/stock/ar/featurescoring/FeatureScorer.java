package sg.edu.nus.wing.qanus.stock.ar.featurescoring;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.stock.ar.DocumentScore;


/**
 * Responsible for tabulating the scores of candidate passages.
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version v04Jan2010
 */
public class FeatureScorer {

	// Used to hold all the added documents
	private ArrayList<String> m_DocumentStore;


	/**
	 * Constructor
	 */
	public FeatureScorer() {
		m_DocumentStore = new ArrayList<String>();
	} // end constructor


	/**
	 * Adds a document to the class for subsequent consideration.
	 * @param a_DocumentString [in] the actual string that make up the document.
	 */
	public void AddDocument(String a_DocumentString) {
		m_DocumentStore.add(a_DocumentString);
	} // end AddDocument()


	/**
	 * Retrieve the top N documents based on the scores assigned to the documents.
	 *
	 * @param a_Query [in] search query used to score the documents against
	 * @param a_NumTopDocs [in] the number of documents to retrieve
	 * @return array of strings of the top N documents
	 */
	public String[] RetrieveTopDocuments(String a_Query, int a_NumTopDocs) {


		// We use a priority queue to quickly retrive top scoring documents
		PriorityQueue<DocumentScore> l_TopDocuments = new PriorityQueue<DocumentScore>();	
		Logger.getLogger("QANUS").log(Level.FINER, "Retrieving [" + a_NumTopDocs + "] documents for query [" + a_Query + "]");


		// Init the features to use - new features can be added here
		FeatureSearchTermFrequency l_Feature_Frequency = new FeatureSearchTermFrequency();
		FeatureSearchTermSpan l_Feature_Proximity = new FeatureSearchTermSpan();
		FeatureSearchTermCoverage l_Feature_Coverage = new FeatureSearchTermCoverage();
		

		// Calculate score for each document
		int l_DocIndex = 0;
		for (String l_Document : m_DocumentStore) {

			double l_DocScore = 0;

			String[] l_QueryArray = { a_Query };

			// Invoke the various features
			double l_FreqScore = l_Feature_Frequency.GetScore(l_QueryArray, l_Document);
			l_DocScore += (0.05)*l_FreqScore;

			double l_ProxScore = l_Feature_Proximity.GetScore(l_QueryArray, l_Document);
			l_DocScore += (0.05)*l_ProxScore;

			double l_CoverageScore = l_Feature_Coverage.GetScore(l_QueryArray, l_Document);
			l_DocScore += (0.9)*l_CoverageScore;

			Logger.getLogger("QANUS").log(Level.FINER, "F:" + l_FreqScore + "-P:" + l_ProxScore + "-C:" + l_CoverageScore + "-[" + l_Document + "]");
			
			l_TopDocuments.add(new DocumentScore(l_DocIndex, l_DocScore, l_Document));
			l_DocIndex++;

		} // end String l_Document..
			
		

		// Create a result array to return the top X documents in		
		String[] l_ResultArray = new String[(a_NumTopDocs<l_TopDocuments.size())?a_NumTopDocs:l_TopDocuments.size()];
		for (int i = 0; i < l_ResultArray.length; ++i) {
			l_ResultArray[i] = l_TopDocuments.remove().GetDocText();
		} // end for i

		return l_ResultArray;

		
	} // end RetrieveTopDocuments()
	

} // end class FeatureScorer
