package sg.edu.nus.wing.qanus.stock.ar;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IStrategyModule;
import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;
import sg.yeefan.filedownloader.FileDownloader;
import sg.yeefan.searchenginewrapper.SearchEngineClient;
import sg.yeefan.searchenginewrapper.SearchEngineException;
import sg.yeefan.searchenginewrapper.SearchEngineResult;
import sg.yeefan.searchenginewrapper.SearchEngineResults;
import sg.yeefan.searchenginewrapper.clients.GoogleAJAXClient;


/**
 * Implements a basic IR based strategy for answer retrieval.
 * Terms from the question are used for document selection using a Lucene index
 * of the source documents.
 * We then make use of the expected type of the question coupled with a NER on the
 * document selection to identify answers.
 *
 * General techniques used :
 *
 * 1. Query expansion - using NN and VB of questions, target name
 * 2. Retrieve relevant documents using Lucene
 * 3. Rank individual sentences from retrieved documents with TD-IDF score
 * 4. Basic pattern-based answer extraction from top-ranked sentence
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 07Dec2009
 */
public class BasicIRBasedStrategyUsingWebAsCorpus implements IStrategyModule {

	// Retrieve 100 top documents from search engine for each search
	private final int RESULTS_TO_RETRIEVE = 10;

	// The Lucene engine
	private SearchEngineClient m_SearchEngine;

	private StanfordPOSTagger m_ModulePOS;

	/**
	 * Constructor.
	 * @param a_KBFolder [in] folder where Lucene KB is stored.
	 */
	public BasicIRBasedStrategyUsingWebAsCorpus() {

		m_SearchEngine = new GoogleAJAXClient();

		m_ModulePOS = new StanfordPOSTagger("lib" + File.separator + "bidirectional-wsj-0-18.tagger");


		/* Used for logging
		Logger l_Logger = Logger.getLogger("QANUS");
		FileHandler l_FH;
		try {
			l_FH = new FileHandler("QANUS.log", true);
			l_Logger.addHandler(l_FH);
			l_Logger.setLevel(Level.ALL);
			SimpleFormatter l_Fmter = new SimpleFormatter();
			l_FH.setFormatter(l_Fmter);
		} catch (IOException ex) {
			//Logger.getLogger(BasicIRBasedStrategyUsingWebAsCorpus.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			//Logger.getLogger(BasicIRBasedStrategyUsingWebAsCorpus.class.getName()).log(Level.SEVERE, null, ex);
		}
		 * */


	} // end constructor



	public String GetModuleID() {
		return "BasicIRBasedStrategyUsingWebAsCorpus";
	}


	/**
	 * Retrieve the answer for a given question.
	 * @param a_QuestionItem [in] question represented as DataItem
	 * @return answer represented as DataItem, or null on any errors
	 */
	public DataItem GetAnswerForQuestion(DataItem a_QuestionItem) {

		// 0. Get question text
		// 1. Search terms - made of noun phrases, target name (w/o adj)?
		// 2. Retrieved ranked docs - how to do passage/sentence selection?? TODO
		// -------------------------


		// 0. Get question text and type - do only for FACTOID -- TODO list also?
		String l_QuestionType = a_QuestionItem.GetAttribute("type");
		String l_QuestionID = a_QuestionItem.GetAttribute("id");
		DataItem[] l_QItems = a_QuestionItem.GetFieldValues("q");
		// If we want to retrieve the actual question text for printing out for debugging
		//if (l_QItems != null) {
	    //		l_QuestionText = (l_QItems[0].GetValue())[0];
		//}
		if (l_QuestionType.compareToIgnoreCase("FACTOID") != 0) {
			return null; // TODO list questions?
			// Plans are to support list questions soon
		}


		// 1. Build seach terms dynamically, incorporating relevant information where possible
		String l_Query = "";
		String l_Target = a_QuestionItem.GetAttribute("Target");
		if (l_Target != null) {
			 l_Query = l_Target + " "; // target name
		}
		
		// Use POS if available
		DataItem[] l_POSItems = a_QuestionItem.GetFieldValues("Q-POS");
		if (l_POSItems != null) {
			for (DataItem l_POSItem : l_POSItems) {
				String l_TaggedSentences[] = l_POSItem.GetValue();
				for (String l_TaggedSentence : l_TaggedSentences) {
					StringTokenizer l_POSST = new StringTokenizer(l_TaggedSentence);
					while (l_POSST.hasMoreTokens()) {
						String l_POSEntity = l_POSST.nextToken();
						int l_DelimPos = l_POSEntity.indexOf('/');
						if (l_DelimPos != -1) {
							try {
								String l_POSTag = l_POSEntity.substring(l_DelimPos+1);
								if (l_POSTag.substring(0,2).compareToIgnoreCase("NN") == 0) { // Match all NN tags like NNS NNP NN NNPS
									l_Query += StripXMLChar(l_POSEntity.substring(0, l_DelimPos)) + " ";
								}
								if (l_POSTag.substring(0,2).compareToIgnoreCase("VB") == 0) { // Match all VB tags like VB VBZ VBG VBD
									l_Query += StripXMLChar(l_POSEntity.substring(0, l_DelimPos)) + " ";
								}
							} catch (Exception ex) {
								// In case we mishandle indices and run into problems
								// just skip this one, for robustness
								continue;
							}
						}
					} // end while
				} // end for String
			} // end for DataItem
		} // end if

		
		Logger.getLogger("QANUS").log(Level.FINE, "Search query: " + l_Query);



		/* The following code segment attempt to rank individual sentences based
		 *  on TF_IDF, but doesn't seem to be very effective.
		 * The TFIDF score on individual sentences should be quite crap given
		 * that our search query is actually very long and varied.
		 */
		try {
			// end if
			// 2. Retrieve matched documents
			m_SearchEngine.setLabel(l_QuestionID);
			m_SearchEngine.setQuery(l_Query);
			m_SearchEngine.setStartIndex(1);
			m_SearchEngine.setNumResults(RESULTS_TO_RETRIEVE);
			SearchEngineResults l_SearchResults = null;
			try {
				l_SearchResults = m_SearchEngine.getResults();
			} catch (SearchEngineException e) {
				Logger.getLogger("QANUS").logp(Level.SEVERE, BasicIRBasedStrategyUsingWebAsCorpus.class.getName(), "GetAnswerForQuestion", "Error doing search [" + l_Query + "]");
				return null;
			}

			// Iterate over the Documents in the Hits object
			// Rank each sentence with TF-IDF
			// and use the highest scored sentence to extract our eventual answer from
			TFIDF l_TFIDF = new TFIDF();
			for (SearchEngineResult l_SearchResult : l_SearchResults.getResults()) {
            	
            	//m_SearchEngine.getLabel();
				//String[] l_ArrText = l_SearchResult.getSnippet();
				String l_Headline = l_SearchResult.getTitle();

				Logger.getLogger("QANUS").log(Level.FINE, "Result headline: " + l_Headline);


				FileDownloader l_Downloader = new FileDownloader();
				BufferedInputStream l_InStream = new BufferedInputStream(l_Downloader.getURLInputStream(l_SearchResult.getURL()));

				// Download retrieved file
				byte[] l_InBuffer = new byte[4096];
				String l_Doc = "";
				while (true) {

					int l_BytesRead = l_InStream.read(l_InBuffer);
					if (l_BytesRead == -1) {
						break;
					}

					l_Doc += new String(l_InBuffer);

				} // end while !l_InputStreamEmpty...

				// Treat each sentence within every document as a document, and
				// rank the sentences using TF-IDF
				// To do that we have to break the downloaded file into sentences.
				// We do this primitively by using line breaks
				Logger.getLogger("QANUS").log(Level.FINE, "Result: " + l_Doc);
				StringTokenizer l_ST = new StringTokenizer(l_Doc, "\n");
				while (l_ST.hasMoreTokens()) {
					l_TFIDF.AddDocument(l_ST.nextToken());
				}
				
            }

			Logger.getLogger("QANUS").log(Level.FINE, "Search complete!");
			

			// Convert search term into an array of strings
			String[] l_QueryAsArray = ExtractWordsIntoArray(l_Query);
			String[] l_BestSentence = l_TFIDF.RetrieveTopDocuments(l_QueryAsArray, 1);


			// Pattern-based answer extraction
			// Use expected question answer type, try to see if we can find a similar type in best sentence
			// If multiple matches, we check for POS tag
			//  Question -> What -> we choose NN
			//  Question -> Who -> we choose NNP
			//  Otherwise, random
			// If no match, we check for POS tag as above
			String l_ExpectedAnswerType = null;
			DataItem[] l_QCItems = a_QuestionItem.GetFieldValues("Q-QC");
			if (l_QCItems != null) {
				l_ExpectedAnswerType = (l_QCItems[0].GetValue())[0];
			}
			// Get POS Tag of best sentence
			String[] l_TaggedBestSentence = m_ModulePOS.ProcessText(l_BestSentence);


			String l_Answer = "";
			if (l_ExpectedAnswerType.compareTo("ABBR:exp") == 0) {
				// Look for Capital letters denoting the abbreviation
				Pattern l_Pattern = Pattern.compile("([A-Z][a-z0-9]* )+");
				Matcher l_Matcher = l_Pattern.matcher(l_BestSentence[0]);
				if (l_Matcher.find()) {
					l_Answer = l_BestSentence[0].substring(l_Matcher.start(), l_Matcher.end());
				}
			} else
			if (l_ExpectedAnswerType.substring(0,5).compareTo("ENTY:") == 0) {
				// Look for NN*
				StringTokenizer l_ST = new StringTokenizer(l_TaggedBestSentence[0]);
				while (l_ST.hasMoreTokens()) {
					try {
						String l_Token = l_ST.nextToken();
						// Check POS
						int l_DelimIndex = l_Token.indexOf('/');
						if (l_DelimIndex == -1) {
							continue; // POS tag not found
						}
						String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
						if (l_POSTag.substring(0, 2).compareToIgnoreCase("NN") == 0) {
							if (l_Answer.length() > 0) l_Answer += " ";
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can retunr the answer already
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				}
			} else
			if (l_ExpectedAnswerType.substring(0,4).compareTo("HUM:") == 0) {
				StringTokenizer l_ST = new StringTokenizer(l_TaggedBestSentence[0]);
				while (l_ST.hasMoreTokens()) {
					try {
						String l_Token = l_ST.nextToken();
						// Check POS
						int l_DelimIndex = l_Token.indexOf('/');
						if (l_DelimIndex == -1) {
							continue; // POS tag not found
						}
						String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
						if (l_POSTag.substring(0, 3).compareToIgnoreCase("NNP") == 0) {
							if (l_Answer.length() > 0) l_Answer += " ";
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can retunr the answer already
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				}
			} else
			if (l_ExpectedAnswerType.substring(0,4).compareTo("LOC:") == 0) {
				StringTokenizer l_ST = new StringTokenizer(l_TaggedBestSentence[0]);
				while (l_ST.hasMoreTokens()) {
					try {
						String l_Token = l_ST.nextToken();
						// Check POS
						int l_DelimIndex = l_Token.indexOf('/');
						if (l_DelimIndex == -1) {
							continue; // POS tag not found
						}
						String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
						if (l_POSTag.substring(0, 3).compareToIgnoreCase("NNP") == 0) {
							if (l_Answer.length() > 0) l_Answer += " ";
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can retunr the answer already
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				}
			} else
			if (l_ExpectedAnswerType.substring(0,4).compareTo("NUM:") == 0) {
				StringTokenizer l_ST = new StringTokenizer(l_TaggedBestSentence[0]);
				while (l_ST.hasMoreTokens()) {
					try {
						String l_Token = l_ST.nextToken();
						// Check POS
						int l_DelimIndex = l_Token.indexOf('/');
						if (l_DelimIndex == -1) {
							continue; // POS tag not found
						}
						String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
						if (l_POSTag.substring(0, 2).compareToIgnoreCase("CD") == 0) {
							if (l_Answer.length() > 0) l_Answer += " ";
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can retunr the answer already
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				}
			} else {
				// Default case
				// Look for NN*
				StringTokenizer l_ST = new StringTokenizer(l_TaggedBestSentence[0]);
				while (l_ST.hasMoreTokens()) {
					try {
						String l_Token = l_ST.nextToken();
						// Check POS
						int l_DelimIndex = l_Token.indexOf('/');
						if (l_DelimIndex == -1) {
							continue; // POS tag not found
						}
						String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
						if (l_POSTag.substring(0, 2).compareToIgnoreCase("NN") == 0) {
							if (l_Answer.length() > 0) l_Answer += " ";
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can retunr the answer already
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				}
			}

			//System.out.println("[" + l_ExpectedAnswerType + "] Question : " + l_QuestionText);
			//System.out.println("Answer sentence : " + l_Answer);

			if (l_Answer.length() == 0) {
				l_Answer = "NA";
			}

			// Build the data item to return as result of this function			
			DataItem l_Result = new DataItem("Result");
			l_Result.AddAttribute("QID", l_QuestionID);
			l_Result.AddField("Answer", l_Answer);
			l_Result.AddField("QuestionType", l_ExpectedAnswerType);
			l_Result.AddField("AnswerString", l_TaggedBestSentence[0]);

			return l_Result;
		
		} catch (Exception ex) {			
			Logger.getLogger("QANUS").logp(Level.WARNING, BasicIRBasedStrategyUsingWebAsCorpus.class.getName(), "GetAnswerForQuestion", "General exception", ex);
		}


		// Exception encountered, no answer
		return null;


	} // end GetAnswerForQuestion()


	/**
	 * Removes any XML encoded escape chars in a given string.
	 *
	 * For example Bush&apos;s is actually Bush's
	 * We just strip away the apostrophes.
	 *
	 * &quot;Bush&quot; is actually "Bush"
	 * We also strip away the quotes.
	 *
	 * D&ampG is D&G, we replace accordingly.
	 *
	 * TODO < and > also
	 *
	 * @param a_String
	 * @return string with XML characters replaced
	 */
	private String StripXMLChar(String a_String) {

		Pattern l_Pattern_APOSS = Pattern.compile("&apos;s");
		Matcher l_Matcher_APOSS = l_Pattern_APOSS.matcher(a_String);
		a_String = l_Matcher_APOSS.replaceAll("");

		Pattern l_Pattern_APOS = Pattern.compile("&apos;");
		Matcher l_Matcher_APOS = l_Pattern_APOS.matcher(a_String);
		a_String = l_Matcher_APOS.replaceAll("'");

		Pattern l_Pattern_QUOT = Pattern.compile("&quot;");
		Matcher l_Matcher_QUOT = l_Pattern_QUOT.matcher(a_String);
		a_String = l_Matcher_QUOT.replaceAll("");

		Pattern l_Pattern_AMP = Pattern.compile("&amp;");
		Matcher l_Matcher_AMP = l_Pattern_AMP.matcher(a_String);
		a_String = l_Matcher_AMP.replaceAll("&");

		return a_String;

	} // end StripXMLChar()


	/**
	 * Starting from a string, we pull out sentences within the string into an array.
	 * A sentence is idenfied by parsing for full-stops, exclaimation marks, and question marks.
	 * @param a_Text [in] the string to extract from
	 * @return array of strings containing individual sentneces within string
	 */
	private String[] ExtractSentencesIntoArray(String a_Text) {

		ArrayList<String> l_ArrayList = new ArrayList<String>();

		StringTokenizer l_ST = new StringTokenizer(a_Text, ".?!");
		while (l_ST.hasMoreTokens()) {
			l_ArrayList.add(l_ST.nextToken());
		}

		return l_ArrayList.toArray(new String[0]);

	} // end ExtractSentencesIntoArray


	/**
	 * Break up a string into words, and return the words in an array.
	 * Words identified delimited by spaces
	 * @param a_Text [in] the string to break up
	 * @return array of strings, each individual words within the text.
	 */
	private String[] ExtractWordsIntoArray(String a_Text) {

		ArrayList<String> l_ArrayList = new ArrayList<String>();

		StringTokenizer l_ST = new StringTokenizer(a_Text);
		while (l_ST.hasMoreTokens()) {
			l_ArrayList.add(l_ST.nextToken());
		}

		return l_ArrayList.toArray(new String[0]);

	} // end ExtractWordsIntoArray()



} // end class BasicIRBasedStrategyUsingWebAsCorpus
