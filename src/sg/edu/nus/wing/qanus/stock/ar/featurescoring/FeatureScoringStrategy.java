package sg.edu.nus.wing.qanus.stock.ar.featurescoring;


import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IAnalyzable;
import sg.edu.nus.wing.qanus.framework.commons.IStrategyModule;
import sg.edu.nus.wing.qanus.stock.ar.AnswerCandidate;
import sg.edu.nus.wing.qanus.stock.ar.FreebaseQuerier;
import sg.edu.nus.wing.qanus.stock.ar.LuceneInformationBaseQuerier;
import sg.edu.nus.wing.qanus.textprocessing.StanfordNER;
import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;
import sg.edu.nus.wing.qanus.textprocessing.StopWordsFilter;
import sg.yeefan.searchenginewrapper.SearchEngineClient;
import sg.yeefan.searchenginewrapper.SearchEngineException;
import sg.yeefan.searchenginewrapper.SearchEngineResults;
import sg.yeefan.searchenginewrapper.clients.YahooClient;

/**
 * This is an attempt to improve on the BasicIRBasedStrategy that was the first one to
 * be shipped with QANUS.
 *
 * In this strategy, we will attempt to identify several features, and score
 * candidate passages based on these features. The scores of individual features
 * are summed linearly to identify the best passage.
 * This best passage will then be scoured for the answer string.
 *
 * We also put in improvements to the heuristics used for answer string extraction:
 * 1. Abbreviations (ABBR:exp)
 *	Use of regular expressions to identify expansions
 * 2. Locations (LOC:*)
 *	Use of NER information to identify locations from best ranked sentence
 * 3. Individual people (HUM:ind)
 *	Locate proper nouns from best ranked sentence
 *	Score each candidate based on features such as its proximity with terms in the query
 *  Two special sub types of HUM:ind questions have been identified :
 *    Type 1 - Who is the CEO [of ABC]?
 *    Type 2 - Who supervised the transplant?
 *  They are identified based on how these questions are expected to be answered. For these
 *  types of questions, the query used is a bit different from the rest of the questions.
 *  The "subject" of the question (such as CEO of ABC company) is identified and used as the query
 *  instead.
 * 4. Dates (NUM:date)
 *	For questions that look for years, a regular expression is used to pick out possible years
 *  Otherwise the date of the newspaper article from which the candidate passage is retrieved from is used
 * 5. Counts (NUM:count)
 *	The "subject" of the question (for example "miners" in "How many miners...") is retrieved.
 *  Numerals are retrieved from various candidate passages and scored based on their proximity to the
 *  occurence of the "subject" within the candidate passages. 
 * 6. Groups (HUM:gr)
 *  Proper nouns are identified from the candidate passages.
 *  These candidates are scored based on a few features, including the proximity of the noun
 *  and occurences of the question target within the candidate passages.
 *
 *  The code in this file is long, un-wieldy and not the best example of how code should be written.
 * But keeping the if-elses in the same code body makes it easier to read(?)....
 *
 *  This hopefully can also serve as a (somewhat negative) example of how new answer retrieval
 * strategies can be added to QANUS.
 *  A new class implementing IStrategyModule is all that is needed.
 *
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version v04Jan2010
 */
public class FeatureScoringStrategy implements IStrategyModule, IAnalyzable {

	// Retrieve 100 top documents from search engine for each search
	private final int RESULTS_TO_RETRIEVE = 50; // was 100

	// The Lucene engine
	private LuceneInformationBaseQuerier m_InformationBase;

	// Temporary annotation modules
	// While we are still building this into our Lucene index
	private StanfordPOSTagger m_ModulePOS;
	private StanfordNER m_ModuleNER; // TODO remove after the information is included into Lucene index
	//private StanfordNERWebService m_ModuleNER; // TODO remove after the information is included into Lucene index

	// Used to query Freebase to make sure some answers are "sane".
	// I mean, we want to be sure we return a country when asked for a country.
	private FreebaseQuerier m_FBQ;


	// Specially identified types of question sub types, detracting from the taxonomy of
	// (Li and Roth 2002)
	private enum QuestionSubType {
		HUM_IND_TYPE1, HUM_IND_TYPE1_IC, HUM_IND_TYPE2, OTHERS
	};



	public FeatureScoringStrategy(File a_IBFolder) {

		// Initialise required components
		m_InformationBase = new LuceneInformationBaseQuerier(a_IBFolder, RESULTS_TO_RETRIEVE);

		// Processing modules
		m_ModulePOS = new StanfordPOSTagger("lib" + File.separator + "bidirectional-wsj-0-18.tagger");
		m_ModuleNER = new StanfordNER("lib" + File.separator + "ner-eng-ie.crf-4-conll-distsim.ser.gz");
		//m_ModuleNER = new StanfordNERWebService();

		// Validation modules
		m_FBQ = new FreebaseQuerier("choppingboard" + File.separator + "temp" + File.separator + "freebase-cache");

	}

	public String GetModuleID() {
		return "FeatureScoringStrategy";
	}

	
	public DataItem GetAnalysisInfoForQuestion(DataItem a_QuestionItem) {
		return GetAnswerForQuestion(a_QuestionItem, true);
	} // end GetAnalysisInfoForQuestion()


	/**
	 * Retrieves an answer for the provided question.
	 * @param a_QuestionItem [in] structure containing question and annotations
	 * @return Answer to the question
	 */
	public DataItem GetAnswerForQuestion(DataItem a_QuestionItem) {
		return GetAnswerForQuestion(a_QuestionItem, false);
	} // end GetAnswerForQuestion()


	/**
	 * Retrieves an answer for the provided question.
	 * @param a_QuestionItem [in] structure containing question and annotations
	 * @return Answer to the question
	 */
	public DataItem GetAnswerForQuestion(DataItem a_QuestionItem, boolean a_Analysis) {

		
		// Retrieve question and annotations
		String l_QuestionType = a_QuestionItem.GetAttribute("type");
		if (l_QuestionType.compareToIgnoreCase("FACTOID") != 0) {
			return null; // TODO list questions?
			// Plans are to support list questions soon
		}

		String l_QuestionID = a_QuestionItem.GetAttribute("id");
		String l_QuestionTarget = a_QuestionItem.GetAttribute("Target");

		String l_ExpectedAnswerType = null;
		DataItem[] l_QCItems = a_QuestionItem.GetFieldValues("Q-QC");
		if (l_QCItems != null) {
			l_ExpectedAnswerType = (l_QCItems[0].GetValue())[0];
		}

		// Retreive actual question string
		String l_QuestionText = "";
		DataItem[] l_QItems = a_QuestionItem.GetFieldValues("q");
		if (l_QItems != null) {
			l_QuestionText = (l_QItems[0].GetValue())[0];
		}

		// Retrieve POS annotation
		String l_QuestionPOS = "";
		DataItem[] l_QuestionPOSItems = a_QuestionItem.GetFieldValues("Q-POS");
		if (l_QuestionPOSItems != null) {
			l_QuestionPOS = (l_QuestionPOSItems[0].GetValue())[0];
		}



		// If analysis is required, prepare the return items
		DataItem l_AnalysisResults = new DataItem("Analysis");
		l_AnalysisResults.AddAttribute("QID", l_QuestionID);



		// Query to use depends on question type
		// First identify question type and subtype
		String l_Query = null;
		QuestionSubType l_SubType = GetQuestionSubType(l_QuestionText, l_QuestionPOS, l_ExpectedAnswerType);
		switch (l_SubType) {
			case HUM_IND_TYPE1:
				//System.out.println(l_QuestionText + " - TYPE1");
				l_Query = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);
				break;
			case HUM_IND_TYPE1_IC:
				//System.out.println(l_QuestionText + " - TYPE1-IC");
				l_Query = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);
				l_Query += " " + l_QuestionTarget;
				break;
			case HUM_IND_TYPE2:				
				l_Query = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);				
				break;
			default:				
				// Build a query string from the question information
				l_Query = FormQuery(l_QuestionTarget, l_QuestionPOS);				
				break;
		} // end switch

				

		// Perform search and process results for answers
		try {

			// Retrieve documents based on the search string from the search engine
			ScoreDoc[] l_RetrievedDocs = (ScoreDoc[]) m_InformationBase.SearchQuery(l_Query);

			if (l_RetrievedDocs == null) throw new Exception("No search queries returned.");

			// Iterate over the Documents in the Hits object
			FeatureScorer l_FScorer = new FeatureScorer();
			for (int i = 0; i < l_RetrievedDocs.length; i++) {

				ScoreDoc l_ScoreDoc = l_RetrievedDocs[i];
				Document l_Doc = m_InformationBase.GetDoc(l_ScoreDoc.doc);
				String[] l_ArrText = l_Doc.getValues("Text");
				String l_Headline = l_Doc.get("Headline");


				// Treat each sentence within every document as a 'passage'
				// We will rank each passage based on a set of features, and use the top scoring
				// passage for answer string extraction
				for (String l_Sentence : l_ArrText) {
					// Add the passage to our scorer for scoring
					l_FScorer.AddDocument(l_Sentence);

					// If analysis is to be performed, we track the sentences that are retrieved
					if (a_Analysis) {
						l_AnalysisResults.AddField("Stage1", l_Sentence);
					}
				} // end for

			} // end for i

			
			// Retrieve the N-best passages from all the retrieved documents
			String[] l_BestSentence = l_FScorer.RetrieveTopDocuments(l_Query, 40);

			// If analysis is to be performed, we track the sentences that are retrieved
			if (a_Analysis) {
				for (String l_Sentence : l_BestSentence) {
					l_AnalysisResults.AddField("Stage2", l_Sentence);
				}
			}


			// Pattern-based answer extraction
			// Use expected question answer type, try to see if we can find a similar type in best sentence
			

			// Get POS annotations for ranked sentences
			// This is currently being built into the Lucene index. It will take some time. Once
			// this is done we can just retrieve the annotations from Lucene - cutting down on
			// run-time computation requirements.
			String[] l_POSTaggedBestSentence = m_ModulePOS.ProcessText(l_BestSentence);


			// Variable used to hold the extracted answer (eventually) and the passage from which
			// it is extracted
			String l_Answer = "";
			String l_OriginalAnswerString = "";
			// -----


			// Start of pattern based answer extraction - based on the identified expected
			// answer types of the questions we are handling
			if (l_ExpectedAnswerType.compareToIgnoreCase("ABBR:exp") == 0) {

				// Abbreviation - Expansions

				// Identify the abbreviation we want to expand.
				// Typically this should have been labeled as an "ORGANISATION" by the NER.
				LinkedList<String> l_Candidates = new LinkedList<String>();
				String l_NERLabel = null;
				DataItem[] l_NERItems = a_QuestionItem.GetFieldValues("Q-NER");
				if (l_NERItems != null) {
					l_NERLabel = (l_NERItems[0].GetValue())[0];

					// Look for organisations
					Pattern l_Pattern = Pattern.compile("([A-Za-z]+)/ORGANIZATION");
					Matcher l_Matcher = l_Pattern.matcher(l_NERLabel);
					while (l_Matcher.find()) {
						// Grab the matching results
						l_Candidates.add(l_NERLabel.substring(l_Matcher.start(), l_Matcher.end()));
					}
				} // end if

				// l_Candidates now contain the identified possible abbreviations within the question


				// Look for Capital letters denoting the abbreviation by constructing
				// a new reg ex string
				LinkedList<String> l_Answers = new LinkedList<String>();
				LinkedList<String> l_OriginalAnswerStrings = new LinkedList<String>();
				for (String l_Candidate : l_Candidates) {
					String l_Regex = "";
					for (int i = 0; i < l_Candidate.length(); ++i) {
						if (Character.isLetter(l_Candidate.charAt(i))) {
							// We expect the answer to be something like
							// D... A... R.... etc for the abbreviation DAR...
							l_Regex += "[" + Character.toUpperCase(l_Candidate.charAt(i)) + "]+[A-Za-z0-9]+ ";
						} else if (l_Candidate.charAt(i) == '/') {
							break;
						}
					}
					l_Regex = l_Regex.trim();
					Pattern l_Pattern = Pattern.compile(l_Regex);
					for (int i = 0; i < l_BestSentence.length; ++i) {
						// Find possible matches within top N passages
						Matcher l_Matcher = l_Pattern.matcher(l_BestSentence[i]);
						if (l_Matcher.find()) {
							l_Answers.add(l_BestSentence[i].substring(l_Matcher.start(), l_Matcher.end()));
							l_OriginalAnswerStrings.add(l_BestSentence[i]);
						}
					}
				} // end for


				// If analysis is to be performed, we track the sentences that are retrieved
				if (a_Analysis) {
					for (String l_Candidate : l_Answers) {
						l_AnalysisResults.AddField("Stage3", l_Candidate);
					}
				}


				// Return the first answer candidate we have amongst all the candidates
				if (l_Answers.size() > 0) {
					l_Answer = l_Answers.getFirst();
					l_OriginalAnswerString = l_OriginalAnswerStrings.getFirst();
				}

				
			} else if (l_ExpectedAnswerType.compareToIgnoreCase("ABBR:abb") == 0) {

				// Abbreviations : Contractions -> What can we do?
				
			} else if ((l_ExpectedAnswerType.length() >= 6
					&& l_ExpectedAnswerType.substring(0, 6).compareToIgnoreCase("HUM:gr") == 0)
					|| (l_ExpectedAnswerType.length() >= 11
					&& l_ExpectedAnswerType.substring(0, 11).compareToIgnoreCase("ENTY:cremat") == 0)) {


				// HUM:gr - Human groups (companies, organisations)
				// ENTY:cremat - Entities

				// Can try to identify the subject of the question, i.e. company, university, etc
				// Heuristically, this is usually the /NN after a /WDT or /WP.

				// Tried some ideas, but gave up on them as they are not helping. One thing tried:
				// 0. Identified the "subject" of the question. For example in "what company...", the subject is "company".
				// 1. Ran a web search for candidate answer and the subject and score the number of hits matched
				// 2. Combined this with other scores like proximity etc
				// Conclusion: Helped for some queries, but when we try to query "us" and "university" for example, these
				//   are very generic terms, it didn't help.

				

				// Now we try to extract candidate answers --- these would be the proper nouns (NNP)
				// We extract them from all the ranked sentences
				// We assume consecutive NNPs are part of the same NNP.
				// i.e. Tiger/NNP Woods/NNP form one NNP
				LinkedList<String> l_CandidateAnswers = new LinkedList<String>();
				LinkedList<String> l_AnswerSources = new LinkedList<String>();
				for (String l_POSTaggedSentence : l_POSTaggedBestSentence) {

					String l_CandidateAnswer = "";
					StringTokenizer l_ST = new StringTokenizer(l_POSTaggedSentence);
					while (l_ST.hasMoreTokens()) {
						try {
							String l_Token = l_ST.nextToken();
							// Check POS
							int l_DelimIndex = l_Token.indexOf('/');
							if (l_DelimIndex == -1) {
								continue; // POS tag not found
							}
							String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
							if (l_POSTag.length() >= 3 && l_POSTag.substring(0, 3).compareToIgnoreCase("NNP") == 0) {
								if (l_CandidateAnswer.length() > 0) {
									l_CandidateAnswer += " ";
								}
								l_CandidateAnswer += l_Token.substring(0, l_DelimIndex);
							} else {
								if (l_CandidateAnswer.length() > 0) {
									// Break in NN sequence.
									// TODO refactor
									l_CandidateAnswer = l_CandidateAnswer.trim();
									Matcher l_EndingPuncMatcher = Pattern.compile("[\\.,\\?\\!']$").matcher(l_CandidateAnswer);
									if (l_EndingPuncMatcher.find()) {
										l_CandidateAnswer = l_EndingPuncMatcher.replaceAll("");
									}
									if (IsReasonableAnswerForHUMGR(l_CandidateAnswer)) {
										l_CandidateAnswers.add(l_CandidateAnswer);
										l_AnswerSources.add(l_POSTaggedSentence);
									}
									l_CandidateAnswer = "";

								}
							}
						} catch (Exception ex) {
							// Generally somethign went wrong;
							continue;
						} // end try-catch
					} // end while
					if (l_CandidateAnswer.length() > 0) {
						// Break in NN sequence.
						// TODO refactor
						l_CandidateAnswer = l_CandidateAnswer.trim();
						Matcher l_EndingPuncMatcher = Pattern.compile("[\\.,\\?\\!']$").matcher(l_CandidateAnswer);
						if (l_EndingPuncMatcher.find()) {
							l_CandidateAnswer = l_EndingPuncMatcher.replaceAll("");
						}
						if (IsReasonableAnswerForHUMGR(l_CandidateAnswer)) {
							l_CandidateAnswers.add(l_CandidateAnswer);
							l_AnswerSources.add(l_POSTaggedSentence);
						}
						l_CandidateAnswer = "";
					}

				} // end for String l_POSTaggedSentence...


				
				// Score each candidate answer relative to the subject and question target
				// We re-use these modules found in the FeatureScorer to help us compute coverage and proximity scores
				FeatureSearchTermCoverage l_FS_Coverage = new FeatureSearchTermCoverage();   
				FeatureSearchTermProximity l_FS_Proximity = new FeatureSearchTermProximity();

				// Look for highest scoring candidate answer
				double l_BestScore = Double.NEGATIVE_INFINITY;
				int l_CurrIndex = 0;
				int l_NumCandidates = l_CandidateAnswers.size();
				for (String l_Candidate : l_CandidateAnswers) {
				
					// Retrieve the sentence where this answer came from
					String l_SourceString = l_AnswerSources.get(l_CurrIndex);


					// Scoring based on different features
					// ---------------------------

					// Proximity score - between target and candidate
					// How near the target and candidate answer appear to each other within the source passage
					String[] l_TargetProximityStrings = {l_QuestionTarget, l_Candidate};
					double l_TargetProximityScore = l_FS_Proximity.GetScore(l_TargetProximityStrings, l_SourceString);

					// Coverage score of target within passage
					// How many words of the target appear within the passage
					String[] l_CoverageStrings = {l_QuestionTarget};
					double l_CoverageScore = l_FS_Coverage.GetScore(l_CoverageStrings, l_SourceString);

					// Penalise if the answer candidate consists of repeated words compared to the question target
					// So we multiply a -1 into the score
					String[] l_CandidateStrings = {l_Candidate};
					double l_RepeatedTermScore = -1 * l_FS_Coverage.GetScore(l_CandidateStrings, l_QuestionTarget);

					// Score derived from rank of source passage
					// This is not very ideal, because NNPs from the same source passage will get a different score.
					// But this is a simple implementation
					double l_SentenceScore = (double) (l_NumCandidates - l_CurrIndex) / l_NumCandidates;

					// Tally the score
					// Weights manually set. We could use supervised learning and learn these values.
					// But these values seem to work fine.
					double l_TotalScore = 
							(0.55 * l_CoverageScore)
							+ (0.2 * l_SentenceScore)
							+ (0.1 * l_TargetProximityScore)
							+ (0.15 * l_RepeatedTermScore);


					if (l_TotalScore > l_BestScore) {
						l_BestScore = l_TotalScore;
						l_Answer = l_Candidate;
						l_OriginalAnswerString = l_SourceString;
					}

					l_CurrIndex++;

				} // end for


				// If analysis is to be performed, we track the sentences that are retrieved
				if (a_Analysis) {
					for (String l_Candidate : l_CandidateAnswers) {
						l_AnalysisResults.AddField("Stage3", l_Candidate);
					}
				}

				

			} else if (l_ExpectedAnswerType.length() >= 7
					&& l_ExpectedAnswerType.substring(0, 7).compareTo("HUM:ind") == 0) {


				// HUM:ind - Humans - individuals


				// We make use of some of the functions within these classes to process the text
				FeatureSearchTermProximity l_FS_Proximity = new FeatureSearchTermProximity();
				FeatureSearchTermCoverage l_FS_Coverage = new FeatureSearchTermCoverage();

				// Get the subject, could be used subsequently to score sentences if needed
				// The subject is only applicable to identified TYPE1 and TYPE2 sentences.
				String l_Subject = "";
				if (l_SubType == QuestionSubType.HUM_IND_TYPE1 || l_SubType == QuestionSubType.HUM_IND_TYPE1_IC || l_SubType == QuestionSubType.HUM_IND_TYPE2) {
					l_Subject = GetSubjectOfHumIndQuestion(l_QuestionText, l_QuestionPOS, l_SubType);
				}

				// Extract proper nouns from all ranked sentences
				LinkedList<AnswerCandidate> l_CandidateAnswers = GetProperNounsOfPersons(l_BestSentence, l_POSTaggedBestSentence);

				// Choose the highest scoring candidate from all the candidates
				double l_BestScore = Double.NEGATIVE_INFINITY;
				Pattern l_NERPattern = Pattern.compile("/PERSON");
				int l_NumCandidates = l_CandidateAnswers.size();
				int l_CurrIndex = 0;
				LinkedHashMap<String, Double> l_CandidatesAndScores = new LinkedHashMap<String, Double>();
				for (AnswerCandidate l_CandidateAnswer : l_CandidateAnswers) {

					String l_CandidateAnswerStringwNER = l_CandidateAnswer.GetAnswer();
					String l_CandidateAnswerSource = l_CandidateAnswer.GetOrigSource();
					

					// Note that the answer string contains punctuations at the end possibly. We keep the punctuations
					// because we will need to do exact string matches between this answer string and the source passage.
					// removing the punctuations will cause the matches to fail
					// Some matches may need the punctuations to be removed though. Where needed,
					// RemoveTrailingPunctuations() is used.

					// Remove POS tag from candidate answer before storing it
					Matcher l_NERMatcher = l_NERPattern.matcher(l_CandidateAnswerStringwNER);
					String l_CandidateAnswerString = l_NERMatcher.replaceAll("");

					// Clear stop words from the query, we will use this to score the proximity.
					// That is how near the query appears to the candidate answer in the source passage
					String[] l_StopWordsFileNames = new String[1];
					l_StopWordsFileNames[0] = "lib" + File.separator + "common-english-words.txt";
					StopWordsFilter l_StopWords = new StopWordsFilter(l_StopWordsFileNames);
					String[] l_StopWordsProcessArr = { l_Query };
					String l_CleanedQuery = l_StopWords.ProcessText(l_StopWordsProcessArr)[0];
					String[] l_ProximityStrings = {l_CleanedQuery, l_CandidateAnswerString};
					double l_ProximityScore = l_FS_Proximity.GetScore(l_ProximityStrings, l_CandidateAnswerSource);

					// Score based on how many words of the target or subject (if available) appear in the source passage
					String[] l_CoverageStrings_Target = {l_QuestionTarget};
					double l_CoverageScore_Target = l_FS_Coverage.GetScore(l_CoverageStrings_Target, l_CandidateAnswerSource);
					double l_CoverageScore_Subject = 0;
					if (l_Subject.length() > 0) {
						String[] l_CoverageStrings_Subject = {l_Subject};
						l_CoverageScore_Subject = l_FS_Coverage.GetScore(l_CoverageStrings_Subject, l_CandidateAnswerSource);
					}

					// Penalise if the answer candidate consists of repeated words compared to the question target
					// So we multiply a -1 into the score
					String[] l_CandidateStrings = {RemoveTrailingPunctuation(l_CandidateAnswerString)};
					double l_RepeatedTermScore = -1 * l_FS_Coverage.GetScore(l_CandidateStrings, l_QuestionTarget);

					// Score derived from rank of source passage - Not an ideal implementation as
					// candidates within the same source passage will get a different sentence score. But
					// this implementation is simple.
					double l_SentenceScore = (double) (l_NumCandidates - l_CurrIndex) / l_NumCandidates;
					
					// Penalise pronouns
					double l_IsPronoun = IsPronoun(RemoveTrailingPunctuation(l_CandidateAnswerString)) ? -1 : 0;

					
					// Tally the score
					// Sentence score depends on order of results returned by Lucene
					// Coverage considers how many keywords from the question target are within the sentence
					// Weights set manually. 
					double l_TotalScore = (0.5 * l_CoverageScore_Target)
							+ (0.25 * l_CoverageScore_Subject) // Only if subject is extracted 
							+ (0.35 * l_SentenceScore)
							+ (0.25 * l_ProximityScore)							
							+ (0.1 * l_RepeatedTermScore) // Penalties
							+ (0.5 * l_IsPronoun);

					// Store the candidate answer and its score
					l_CandidatesAndScores.put(RemoveTrailingPunctuation(l_CandidateAnswerString), l_TotalScore);

					

					if (l_TotalScore > l_BestScore) {

						l_BestScore = l_TotalScore;

						// Remove trailing punctionation marks
						l_CandidateAnswerString = RemoveTrailingPunctuation(l_CandidateAnswerString);

						l_Answer = l_CandidateAnswerString;
						l_OriginalAnswerString = l_CandidateAnswerSource;

					} // end if


					l_CurrIndex++;


				} // end for


				// If analysis is to be performed, we track the sentences that are retrieved
				if (a_Analysis) {
					for (AnswerCandidate l_Candidate : l_CandidateAnswers) {
						l_AnalysisResults.AddField("Stage3", l_Candidate.GetAnswer());
					}
				}


				// If we have an answer string, we see if we can expand on the name
				// For example given "Bush", we look up all candidate answers, and see
				// if there are longer versions like "President Bush", "George W. Bush" etc...
				// We choose the longer version to return as it is more specific.
				if (l_Answer.length() > 0) {

					double l_CurrBestAlternativeScore = Double.NEGATIVE_INFINITY;
					for (String l_Candidate : l_CandidatesAndScores.keySet()) {
						if (l_Candidate.length() > l_Answer.length() && l_Candidate.contains(l_Answer)) {
							double l_AltScore = l_CandidatesAndScores.get(l_Candidate);
							if (l_AltScore > l_CurrBestAlternativeScore) {
								l_CurrBestAlternativeScore = l_AltScore;
								l_Answer = l_Candidate;
								// Note we do not change the originating answer string as that
								// is evidence of the validity of our answer
							}
						}
					} // end for

				} // end if


			} else if (l_ExpectedAnswerType.length() >= 4
					&& l_ExpectedAnswerType.substring(0, 4).compareTo("HUM:") == 0) {


				// All other types of HUMAN questions
				

				// Take the best ranked sentence, extract the first NNP from it and use that as the answer
				StringTokenizer l_ST = new StringTokenizer(l_POSTaggedBestSentence[0]);
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
							if (l_Answer.length() > 0) {
								l_Answer += " ";
							}
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can return the answer already
								l_OriginalAnswerString = l_POSTaggedBestSentence[0];
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				}
				if (l_Answer.length() > 0) {
					// break in NN, so we can return the answer already
					l_OriginalAnswerString = l_POSTaggedBestSentence[0];


					// If analysis is to be performed, we track the sentences that are retrieved
					if (a_Analysis) {
						l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
					}
				}


			} else if (l_ExpectedAnswerType.length() >= 4
					&& l_ExpectedAnswerType.substring(0, 4).compareTo("LOC:") == 0) {

				// Location questions

				// Attempt to find strings tagged as LOCATION by the NER
				Pattern l_Pattern = Pattern.compile("(([A-Za-z\\.,]+)/LOCATION)( ([A-Za-z\\.,]+)/LOCATION)*");
				String[] l_CandidateSentences = m_ModuleNER.ProcessText(l_BestSentence);
				LinkedList<String> l_Candidates = new LinkedList<String>();
				LinkedList<String> l_OriginalAnswerStrings = new LinkedList<String>();
				for (String l_CandidateSentence : l_CandidateSentences) {

					// Extract NER information -- Look for LOCATION words
					Matcher l_Matcher = l_Pattern.matcher(l_CandidateSentence);
					while (l_Matcher.find()) {
						// Grab the matching results
						String l_RawCandidate = l_CandidateSentence.substring(l_Matcher.start(), l_Matcher.end());
						// Remove NER tags from candidate string
						Pattern l_NERPattern = Pattern.compile("/LOCATION");
						Matcher l_NERMatcher = l_NERPattern.matcher(l_RawCandidate);
						String l_Candidate = l_NERMatcher.replaceAll("");
						// Save candidate answer and string the answer is derived from
						l_Candidates.add(l_Candidate.trim());
						l_OriginalAnswerStrings.add(l_CandidateSentence);
					}


				} // end for (String l_CandidateSentence...


				// With the list of candidates, try to score them with some heuristics

				// Re-use these Featurescorer modules to help compute the scores we use
				FeatureSearchTermProximity l_FS_Proximity = new FeatureSearchTermProximity();
				FeatureSearchTermCoverage l_FS_Coverage = new FeatureSearchTermCoverage();

				// Find the highest scoring candidate
				double l_BestScore = Double.NEGATIVE_INFINITY;
				int l_CurrIndex = 0;
				int l_NumCandidates = l_Candidates.size();
				for (String l_Candidate : l_Candidates) {

					// Passage from which candidate is derived
					String l_SourcePassage = l_OriginalAnswerStrings.get(l_CurrIndex);

					// Proximity score
					String[] l_ProximityStrings = {l_QuestionTarget, l_Candidate};
					double l_ProximityScore = l_FS_Proximity.GetScore(l_ProximityStrings, l_SourcePassage);

					// Coverage score of target within passage
					String[] l_CoverageStrings = {l_QuestionTarget};
					double l_CoverageScore = l_FS_Coverage.GetScore(l_CoverageStrings, l_SourcePassage);

					// Penalise if the answer candidate consists of repeated words compared to the question target
					// So we multiply a -1 into the score
					String[] l_CandidateStrings = {l_Candidate};
					double l_RepeatedTermScore = -1 * l_FS_Coverage.GetScore(l_CandidateStrings, l_QuestionTarget);

					// Score derived from rank of source passage
					double l_SentenceScore = (double) (l_NumCandidates - l_CurrIndex) / l_NumCandidates;

					// Score the sanity of the answer, ie. must correspond to question expected answer type
					int l_SanityScore = 0;
					if (l_ExpectedAnswerType.compareToIgnoreCase("LOC:country") == 0) {
						l_SanityScore = m_FBQ.CorrespondsToType(l_Candidate, FreebaseQuerier.ObjectTypes.COUNTRY);
					} else if (l_ExpectedAnswerType.compareToIgnoreCase("LOC:city") == 0) {
						l_SanityScore = m_FBQ.CorrespondsToType(l_Candidate, FreebaseQuerier.ObjectTypes.CITY);
					} else if (l_ExpectedAnswerType.compareToIgnoreCase("LOC:state") == 0) {
						l_SanityScore = m_FBQ.CorrespondsToType(l_Candidate, FreebaseQuerier.ObjectTypes.STATE);
					}

					// Tally the score
					double l_TotalScore = (0.6 * l_CoverageScore)
							+ (0.1 * l_SentenceScore)
							+ (0.2 * l_ProximityScore)
							+ (0.5 * l_SanityScore)
							+ (0.3 * l_RepeatedTermScore);


					if (l_TotalScore > l_BestScore) {
						l_BestScore = l_TotalScore;
						l_Answer = l_Candidate;
						l_OriginalAnswerString = l_SourcePassage;
					}

					l_CurrIndex++;

				} // end for


				// If analysis is to be performed, we track the sentences that are retrieved
				if (a_Analysis) {
					for (String l_Candidate : l_Candidates) {
						l_AnalysisResults.AddField("Stage3", l_Candidate);
					}
				}


			} else if (l_ExpectedAnswerType.length() >= 8
					&& l_ExpectedAnswerType.substring(0, 8).compareToIgnoreCase("NUM:date") == 0) {

				// Dates (or the number of years also)

				// There are two main types of answer
				// 1. a real date, such as 25-Oct-06
				// 2. a year, such as 1999
				// Best differentiator seems to be to detect the word 'year' in the question
				if (l_QuestionText.contains("year")) {

					Pattern l_YearPattern = Pattern.compile("[12][0-9][0-9][0-9]");

					// Look through ranked sentences
					for (String l_Sentence : l_BestSentence) {

						Matcher l_YearMatcher = l_YearPattern.matcher(l_Sentence);

						if (l_YearMatcher.find()) {
							l_Answer = l_Sentence.substring(l_YearMatcher.start(), l_YearMatcher.end());
							l_OriginalAnswerString = l_Sentence;

							// If analysis is to be performed, we track the sentences that are retrieved
							if (a_Analysis) {
								l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
							}

							break; // Stop when we have an answer
						}

					} // end for (String l_Sentence...


				} else {

					// Output a real date

					// Since the AQUAINT-2 corpus consist largely of news paper reports, can we just
					// use the date of the article as the result?

					ScoreDoc l_ScoreDoc = l_RetrievedDocs[0];
					Document l_Doc = m_InformationBase.GetDoc(l_ScoreDoc.doc);
					String l_DocID = l_Doc.get("DocID");

					Pattern l_Pattern = Pattern.compile("[A-Za-z_]+([0-9]+)\\..");
					Matcher l_Matcher = l_Pattern.matcher(l_DocID);
					String l_Captured = "";
					if (l_Matcher.find()) {
						// Get all groups for this match
						for (int i = 0; i <= l_Matcher.groupCount(); i++) {
							l_Captured = l_Matcher.group(i);
						}
					}

					// Change the YYYYMMDD format into DD-MM-YY
					String l_Month = "";
					if (l_Captured.substring(4, 6).compareTo("01") == 0) {
						l_Month = "Jan";
					} else if (l_Captured.substring(4, 6).compareTo("02") == 0) {
						l_Month = "Feb";
					} else if (l_Captured.substring(4, 6).compareTo("03") == 0) {
						l_Month = "Mar";
					} else if (l_Captured.substring(4, 6).compareTo("04") == 0) {
						l_Month = "Apr";
					} else if (l_Captured.substring(4, 6).compareTo("05") == 0) {
						l_Month = "May";
					} else if (l_Captured.substring(4, 6).compareTo("06") == 0) {
						l_Month = "Jun";
					} else if (l_Captured.substring(4, 6).compareTo("07") == 0) {
						l_Month = "Jul";
					} else if (l_Captured.substring(4, 6).compareTo("08") == 0) {
						l_Month = "Aug";
					} else if (l_Captured.substring(4, 6).compareTo("09") == 0) {
						l_Month = "Sep";
					} else if (l_Captured.substring(4, 6).compareTo("10") == 0) {
						l_Month = "Oct";
					} else if (l_Captured.substring(4, 6).compareTo("11") == 0) {
						l_Month = "Nov";
					} else if (l_Captured.substring(4, 6).compareTo("12") == 0) {
						l_Month = "Dec";
					}
					if (l_Captured.charAt(6) == '0') {
						l_Answer = l_Captured.substring(7, 8);
					} else {
						l_Answer = l_Captured.substring(6, 8);
					}
					l_Answer += "-" + l_Month + "-" + l_Captured.substring(2, 4);

					// Extract first sentence as answer string
					String[] l_ArrText = l_Doc.getValues("Text");
					if (l_ArrText != null) {
						l_OriginalAnswerString = l_ArrText[0];
						// If analysis is to be performed, we track the sentences that are retrieved
						if (a_Analysis) {
							l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
						}
					}


				} // end if (a_QuestionText...

			} else if (l_ExpectedAnswerType.compareToIgnoreCase("NUM:period") == 0) {

				// Handle the special case for questions resembling :
				// "How old is XXX ...?
				Pattern l_SubjPattern = Pattern.compile("[hH]ow/WRB old/JJ (is/VBZ|was/VBD) ([A-Za-z\\?\\.,]+/NNP( [A-Za-z\\?\\.,]+/NNP)*)");
				Matcher l_SubjMatcher = l_SubjPattern.matcher(l_QuestionPOS);
				// group(0) by Java's convenion is the original string
				// group(1) is the 'is' or 'was' part
				// group(2) is the NNP after the "how many" phrase
				String l_Subject = "";
				if (l_SubjMatcher.find()) {
					// groupCount() does not include this whole string
					// so we check to see if group(3) exists by checking >= 2
					for (int i = 0; i < l_SubjMatcher.groupCount() && i < 3; ++i) {
						if (i == 2) {
							l_Subject = l_QuestionPOS.substring(l_SubjMatcher.start(i), l_SubjMatcher.end(i));
							// Remove POS tags from subject
							Pattern l_POSPattern = Pattern.compile("[,\\.\\?\\!]?/[A-Z]+");
							Matcher l_POSMatcher = l_POSPattern.matcher(l_Subject);
							l_Subject = l_POSMatcher.replaceAll("");
						} else {
							l_SubjMatcher.group(i);
						}
					} // end for i
				} // end if


				// Go through candidate passages, identify patterns for "age".
				// such as /NNP /NNP, ??/CD,
				// or ??-year-old
				if (l_Subject.length() == 0) {

					// We are unable to retrieve any "subject" from the question
					// Fall back to our default strategy
					String[] l_ExtractedResult = RetrieveBestCD(l_POSTaggedBestSentence);
					if (l_ExtractedResult[0].length() > 0) {
						l_Answer = l_ExtractedResult[0];
						l_OriginalAnswerString = l_ExtractedResult[1];
						// If analysis is to be performed, we track the sentences that are retrieved
						if (a_Analysis) {
							l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
						}
					}

				} else {


					// With the subject, we try to extract candidate age patterns and score them
					// with their relative proximity to occurences of the subject within
					// the given passage

					// Used to score and choose best candidate answer
					double l_BestScore = Double.NEGATIVE_INFINITY;
					FeatureSearchTermProximity l_FS_P = new FeatureSearchTermProximity();
					FeatureSearchTermCoverage l_FS_C = new FeatureSearchTermCoverage();
					int l_NumCandidates = l_POSTaggedBestSentence.length;
					int l_CurrIndex = 0;

					for (String l_POSSentence : l_POSTaggedBestSentence) {

						// Retrieve occurences of the subuject within the candidate passage

						// If occurences found, proceed to extract all /CD from passage
						// and score the CD
						// The scoring includes the proximity between the /CD and subject
						// as well as the current rank of the sentence


						Pattern l_AgePattern1 = Pattern.compile(", ?([0-9]+)[,\\.]?/CD[,\\.\\?\\!]?");
						Matcher l_AgeMatcher1 = l_AgePattern1.matcher(l_POSSentence);
						while (l_AgeMatcher1.find()) {

							String l_RawCDString = l_POSSentence.substring(l_AgeMatcher1.start(), l_AgeMatcher1.end());

							String[] l_Strings = {l_RawCDString, l_Subject};
							double l_ProximityScore = l_FS_P.GetScore(l_Strings, l_POSSentence);
							double l_SentenceRankScore = (l_NumCandidates - l_CurrIndex) / (double) l_NumCandidates;

							double l_TotalScore = (0.8 * l_ProximityScore)
									+ (0.2 * l_SentenceRankScore);

							if (l_TotalScore > l_BestScore) {
								l_BestScore = l_TotalScore;
								
								l_Answer = l_RawCDString;
								l_OriginalAnswerString = l_POSSentence;
							} // end if (l_TotalScore..


						} // end while

						Pattern l_AgePattern2 = Pattern.compile("([0-9]+)-year-old");
						Matcher l_AgeMatcher2 = l_AgePattern2.matcher(l_POSSentence);
						while (l_AgeMatcher2.find()) {

							if (l_AgeMatcher2.groupCount() < 2) {
								continue;
							}

							l_AgeMatcher2.group(0);
							String l_RawCDString = l_AgeMatcher2.group(1);

							String[] l_Strings = {l_RawCDString, l_Subject};
							double l_ProximityScore = l_FS_P.GetScore(l_Strings, l_POSSentence);
							double l_SentenceRankScore = (l_NumCandidates - l_CurrIndex) / (double) l_NumCandidates;

							double l_TotalScore = (0.8 * l_ProximityScore)
									+ (0.2 * l_SentenceRankScore);

							if (l_TotalScore > l_BestScore) {
								l_BestScore = l_TotalScore;
								
								l_Answer = l_RawCDString;
								l_OriginalAnswerString = l_POSSentence;
							} // end if (l_TotalScore..


						} // end while

						l_CurrIndex++;

					} // end for String...


					// Fall back strategy, if after the FOR loop, no candidate /CDs found
					// We are unable to retrieve any "subject" from the question
					// Fall back to our default strategy
					if (l_Answer.length() == 0) {
						String[] l_ExtractedResult = RetrieveBestCD(l_POSTaggedBestSentence);
						if (l_ExtractedResult[0].length() > 0) {
							l_Answer = l_ExtractedResult[0];
							l_OriginalAnswerString = l_ExtractedResult[1];
						}
					}

					// If analysis is to be performed, we track the sentences that are retrieved
					if (a_Analysis) {
						l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
					}

				} // end if (l_Subject,.lengtt...


			} else if (l_ExpectedAnswerType.compareToIgnoreCase("NUM:count") == 0) {

				
				// Counts

				// Identify the subject of the question
				// For example : "How many miners.."..
				// The subject is "miners"
				Pattern l_SubjPattern = Pattern.compile("[hH]ow/WRB many/JJ ([A-Za-z0-9]+/NN[SP]? ([A-Za-z0-9]+/NN[SP]?)*)");
				Matcher l_SubjMatcher = l_SubjPattern.matcher(l_QuestionPOS);
				// group(0) by Java's convenion is the original string
				// group(2) is the NN after the "how many" phrase
				String l_Subject = "";
				if (l_SubjMatcher.find()) {
					// groupCount() does not include this whole string
					// so we check to see if group(3) exists by checking >= 2
					for (int i = 0; i < l_SubjMatcher.groupCount() && i < 2; ++i) {
						if (i == 1) {
							l_Subject = l_QuestionPOS.substring(l_SubjMatcher.start(i), l_SubjMatcher.end(i));
							// Remove POS tags from subject
							Pattern l_POSPattern = Pattern.compile("/[A-Z]+");
							Matcher l_POSMatcher = l_POSPattern.matcher(l_Subject);
							l_Subject = l_POSMatcher.replaceAll("");
						} else {
							l_SubjMatcher.group(i);
						}
					} // end for i
				} // end if


				if (l_Subject.length() == 0) {

					// We are unable to retrieve any "subject" from the question
					// Fall back to our default strategy
					String[] l_ExtractedResult = RetrieveBestCD(l_POSTaggedBestSentence);
					if (l_ExtractedResult[0].length() > 0) {
						l_Answer = l_ExtractedResult[0];
						l_OriginalAnswerString = l_ExtractedResult[1];

						// If analysis is to be performed, we track the sentences that are retrieved
						if (a_Analysis) {
							l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
						}
					}

				} else {


					// With the subject, we try to extract candidate /CD s are score them
					// with their relative proximity to occurences of the subject within
					// the given passage

					// Used to score and choose best candidate answer
					double l_BestScore = Double.NEGATIVE_INFINITY;
					FeatureSearchTermProximity l_FS_P = new FeatureSearchTermProximity();
					FeatureSearchTermCoverage l_FS_C = new FeatureSearchTermCoverage();
					int l_NumCandidates = l_POSTaggedBestSentence.length;
					int l_CurrIndex = 0;

					for (String l_POSSentence : l_POSTaggedBestSentence) {

						// Retrieve occurences of the subuject within the candidate passage

						// If occurences found, proceed to extract all /CD from passage
						// and score the CD
						// The scoring includes the proximity between the /CD and subject
						// as well as the current rank of the sentence


						Pattern l_RawCDPattern = Pattern.compile("([0-9]+(,[0-9]+)*)[\\.!\\?]?/CD");
						Matcher l_RawCDMatcher = l_RawCDPattern.matcher(l_POSSentence);

						while (l_RawCDMatcher.find()) {

							String l_RawCDString = l_POSSentence.substring(l_RawCDMatcher.start(), l_RawCDMatcher.end());
							String[] l_Strings = {l_RawCDString, l_Subject};

							double l_ProximityScore = l_FS_P.GetScore(l_Strings, l_POSSentence);
							double l_SentenceRankScore = (l_NumCandidates - l_CurrIndex) / (double) l_NumCandidates;

							double l_TotalScore = (0.8 * l_ProximityScore)
									+ (0.2 * l_SentenceRankScore);

							if (l_TotalScore > l_BestScore) {
								l_BestScore = l_TotalScore;

								// Retrieve only the number from the raw candidate CD string								
								Matcher l_NumberMatcher = l_RawCDPattern.matcher(l_RawCDString);
								if (l_NumberMatcher.find()) {
									l_NumberMatcher.group(0);
									l_Answer = l_NumberMatcher.group(1);
									l_OriginalAnswerString = l_POSSentence;
								} // end if (l_NumberMatcher...

							} // end if (l_TotalScore..


						} // end while

						l_CurrIndex++;

					} // end for String...


					// Fall back strategy, if after the FOR loop, no candidate /CDs found
					// We are unable to retrieve any "subject" from the question
					// Fall back to our default strategy
					if (l_Answer.length() == 0) {
						String[] l_ExtractedResult = RetrieveBestCD(l_POSTaggedBestSentence);
						if (l_ExtractedResult[0].length() > 0) {
							l_Answer = l_ExtractedResult[0];
							l_OriginalAnswerString = l_ExtractedResult[1];
						}
					}

					// If analysis is to be performed, we track the sentences that are retrieved
					if (a_Analysis) {
						l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
					}

				} // end if (l_Subject,.lengtt...


			} else if (l_ExpectedAnswerType.length() >= 4
					&& l_ExpectedAnswerType.substring(0, 4).compareTo("NUM:") == 0) {

				// Default strategy, look for the first /CD we come across
				String[] l_ExtractedResult = RetrieveBestCD(l_POSTaggedBestSentence);
				if (l_ExtractedResult[0].length() > 0) {
					l_Answer = l_ExtractedResult[0];
					l_OriginalAnswerString = l_ExtractedResult[1];

					// If analysis is to be performed, we track the sentences that are retrieved
					if (a_Analysis) {
						l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
					}
				}

			} else if (l_ExpectedAnswerType.substring(0, 5).compareTo("ENTY:") == 0) {

				// Entities
				// Default strategy

				// Look for any nouns and return the first one as the answer

				StringTokenizer l_ST = new StringTokenizer(l_POSTaggedBestSentence[0]);
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
							if (l_Answer.length() > 0) {
								l_Answer += " ";
							}
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can retunr the answer already
								l_OriginalAnswerString = l_POSTaggedBestSentence[0];
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				} // end while
				if (l_Answer.length() > 0) {
					// break in NN, so we can retunr the answer already
					l_OriginalAnswerString = l_POSTaggedBestSentence[0];
				}

				// If analysis is to be performed, we track the sentences that are retrieved
				if (a_Analysis) {
					l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
				}

			} else {

				// Default case, return the first noun we come across
				

				StringTokenizer l_ST = new StringTokenizer(l_POSTaggedBestSentence[0]);
				while (l_ST.hasMoreTokens()) {
					try {
						String l_Token = l_ST.nextToken();
						// Check POS
						int l_DelimIndex = l_Token.indexOf('/');
						if (l_DelimIndex == -1) {
							continue; // POS tag not found
						}
						String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
						if (l_POSTag.length() >= 2 && l_POSTag.substring(0, 2).compareToIgnoreCase("NN") == 0) {
							if (l_Answer.length() > 0) {
								l_Answer += " ";
							}
							l_Answer += l_Token.substring(0, l_DelimIndex);
						} else {
							if (l_Answer.length() > 0) {
								// break in NN, so we can return the answer already
								break;
							}
						}
					} catch (Exception ex) {
						// Generally somethign went wrong;
						continue;
					} // end try-catch
				} // end while
				if (l_Answer.length() > 0) {
					// Save the supporting answer string
					l_OriginalAnswerString = l_BestSentence[0];
				}

				// If analysis is to be performed, we track the sentences that are retrieved
				if (a_Analysis) {
					l_AnalysisResults.AddField("Stage3", l_OriginalAnswerString);
				}
			} // end if


			

			// Final post-processing
			// 1. Remove punctuation from the end of answers which invariably get extracted too
			if (l_Answer.length() > 0) {
				if (l_Answer.charAt(l_Answer.length() - 1) == '.'
						|| l_Answer.charAt(l_Answer.length() - 1) == ','
						|| l_Answer.charAt(l_Answer.length() - 1) == ':'
						|| l_Answer.charAt(l_Answer.length() - 1) == '"'
						|| l_Answer.charAt(l_Answer.length() - 1) == '?'
						|| l_Answer.charAt(l_Answer.length() - 1) == '\'') {
					l_Answer = l_Answer.substring(0, l_Answer.length() - 1);
				}
			}
			// more punctionation cleansing
			if (l_Answer.length() > 1) {
				if (l_Answer.substring(l_Answer.length() - 2, l_Answer.length()).compareToIgnoreCase("'s") == 0) {
					l_Answer = l_Answer.substring(0, l_Answer.length() - 2);
				}
			}
			if (l_Answer.startsWith("``")) {
				l_Answer = l_Answer.substring(2);
			}


			// No answer found =(
			if (l_Answer.length() == 0) {
				l_Answer = "NA";
			}


			// Build the data item to return as result of this function
			DataItem l_Result = new DataItem("Result");
			l_Result.AddAttribute("QID", l_QuestionID);
			l_Result.AddField("Answer", l_Answer);
			l_Result.AddField("QuestionType", l_ExpectedAnswerType);
			l_Result.AddField("AnswerString", l_OriginalAnswerString);


			// Before returning, save the results we have queries from freebase for this question
			m_FBQ.SaveCache();


			// Return either analysis results of answer to question
			if (a_Analysis) {
				return l_AnalysisResults;
			} else {
				return l_Result;
			}


		} catch (Exception ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, FeatureScoringStrategy.class.getName(), "GetAnswerForQuestion", "General exception", ex);
		}


		// Before returning, save the results we have queries from freebase for this question
		m_FBQ.SaveCache();

		// Exception encountered, no answer
		return null;

	} // end GetAnswerForQuestion()

	/**
	 * Given a array of POS tagged sentences, retrieve the best choice of a "NUMBER" /CD
	 * from the sentences.
	 * This can be used as a default strategy for extracting answer strings from
	 * sentences.
	 * Currently what we do is to extract the first /CD that is encountered.
	 *
	 * @return an array, 1st element is extracted number, 2nd element is the sentence from which it is extracted.
	 *				or an empty array of 2 elements if no answer found.
	 */
	private String[] RetrieveBestCD(String[] a_POSTaggedSentences) {

		String[] l_Result = new String[2];
		l_Result[0] = ""; // Answer string
		l_Result[1] = ""; // Answer source

		// Sanity check
		if (a_POSTaggedSentences == null) {
			return l_Result;
		}

		// Get first/ CD we come across
		for (String l_Sentence : a_POSTaggedSentences) {

			StringTokenizer l_ST = new StringTokenizer(l_Sentence);
			while (l_ST.hasMoreTokens()) {
				try {
					String l_Token = l_ST.nextToken();
					// Check POS
					int l_DelimIndex = l_Token.indexOf('/');
					if (l_DelimIndex == -1) {
						continue; // POS tag not found
					}
					String l_POSTag = l_Token.substring(l_DelimIndex + 1, l_Token.length());
					if (l_POSTag.compareToIgnoreCase("CD") == 0) {
						if (l_Result[0].length() > 0) {
							l_Result[0] += " ";
						}
						l_Result[0] += l_Token.substring(0, l_DelimIndex);
						l_Result[1] = l_Sentence;
					} else {
						if (l_Result[0].length() > 0) {
							break;
						}
					}
				} catch (Exception ex) {
					// Generally something went wrong;
					continue;
				} // end try-catch
			} // end while

			// If we already have an answer we can stop
			if (l_Result[0].length() > 0) {
				break;
			}

		} // end for


		return l_Result;

	} // end RetrieveBestCD()


	/**
	 * Builds a query string with question information.
	 * If POS information is provided with the question text, NN and VB are extracted to
	 * form the query.
	 * If POS information is not available, all individual terms within the question text
	 * are used.
	 *
	 * The formed query is post-processed to ensure that every term is unique,
	 * and stop words are removed.
	 * Stemming is not done because porter's algo does not seem to do a very good job.
	 *
	 * @param a_Target [in] string containing information about the target of the question
	 * @param a_Question [in] string containing the question, can include POS annotations.
	 * @return string containing query formed from the question.
	 */
	private String FormQuery(String a_Target, String a_Question) {

		// Tracks used word to ensure no repteition of terms in resulting query
		LinkedList<String> l_UsedTerms = new LinkedList<String>();

		// Used to remove stop words and stem the query
		String[] l_StopWordsFileNames = new String[1];
		l_StopWordsFileNames[0] = "lib" + File.separator + "common-english-words.txt";
		StopWordsFilter l_StopWords = new StopWordsFilter(l_StopWordsFileNames);

		// Build seach terms dynamically, incorporating relevant information where possible
		// We ensure that the query does not has repeated words by using a LinkedList to collect the hash terms first
		// The linked list helps ensure the terms are ordered unlike if we use a hashset. This could be important
		// as multi-word terms are kept in their correct order.
		String l_Query = "";

		// -- Include the name of the target as part of the query
		StringTokenizer l_ST_Target = new StringTokenizer(a_Target);
		while (l_ST_Target.hasMoreTokens()) {
			String l_Term = l_ST_Target.nextToken();
			if (!l_StopWords.IsStopWord(l_Term)) {
				l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
			}
		}

		// Use POS if available
		if (a_Question.length() > 0) {

			// Used to look up synonyms and related words
			// Commented out because it doesn't seem useful
			// But keeping the code around just in case it turns out to be useful
			// subsequently
			//Thesaurus l_Thesaurus = new Thesaurus();

			// Process every token in question
			StringTokenizer l_POSST = new StringTokenizer(a_Question);
			while (l_POSST.hasMoreTokens()) {
				String l_POSEntity = l_POSST.nextToken();
				int l_DelimPos = l_POSEntity.indexOf('/');
				if (l_DelimPos != -1) {
					try {

						String l_POSTag = l_POSEntity.substring(l_DelimPos + 1);
						if (l_POSTag.substring(0, 2).compareToIgnoreCase("NN") == 0) {
							// Match all NN tags like NNS NNP NN NNPS
							String l_Term = StripXMLChar(l_POSEntity.substring(0, l_DelimPos));
							if (!l_UsedTerms.contains(l_Term)) {

								/* Unused, thesaurus look up
								String[] l_QueryWords = { l_Term };
								String[] l_RelatedWords = l_Thesaurus.ProcessText(l_QueryWords);
								String l_Expanded = "";
								for (String l_RelatedWord : l_RelatedWords) {
								if (l_RelatedWord == null) continue;
								if (l_StopWords.IsStopWord(l_RelatedWord)) continue;
								if (l_Expanded.length() > 0) l_Expanded += " ";
								l_Expanded += l_RelatedWord;
								}
								l_Query = UpdateQuery(l_UsedTerms, l_Expanded, l_Query);
								 */
								l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);

							}

						}
						if (l_POSTag.substring(0, 2).compareToIgnoreCase("VB") == 0) {
							// Match all VB tags like VB VBZ VBG VBD
							String l_Term = StripXMLChar(l_POSEntity.substring(0, l_DelimPos));
							if (!l_UsedTerms.contains(l_Term)) {

								/* Unused thesaurus look up
								String[] l_QueryWords = { l_Term };
								String[] l_RelatedWords = l_Thesaurus.ProcessText(l_QueryWords);
								String l_Expanded = "";
								for (String l_RelatedWord : l_RelatedWords) {
								if (l_RelatedWord == null) continue;
								if (l_StopWords.IsStopWord(l_RelatedWord)) continue;
								if (l_Expanded.length() > 0) l_Expanded += " ";
								l_Expanded += l_RelatedWord;
								}
								l_Query = UpdateQuery(l_UsedTerms, l_Expanded, l_Query);
								 */
								l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);

							}
						}
						if (l_POSTag.substring(0, 2).compareToIgnoreCase("JJ") == 0) {
							// Match all NN tags like NNS NNP NN NNPS
							String l_Term = StripXMLChar(l_POSEntity.substring(0, l_DelimPos));
							if (!l_UsedTerms.contains(l_Term)) {

								/* Unused, thesaurus look up
								String[] l_QueryWords = { l_Term };
								String[] l_RelatedWords = l_Thesaurus.ProcessText(l_QueryWords);
								String l_Expanded = "";
								for (String l_RelatedWord : l_RelatedWords) {
								if (l_RelatedWord == null) continue;
								if (l_StopWords.IsStopWord(l_RelatedWord)) continue;
								if (l_Expanded.length() > 0) l_Expanded += " ";
								l_Expanded += l_RelatedWord;
								}
								l_Query = UpdateQuery(l_UsedTerms, l_Expanded, l_Query);
								 */
								l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);

							}

						}

					} catch (Exception ex) {
						// In case we mishandle indices and run into problems
						// just skip this one, for robustness
						continue;
					}
				}
			} // end while
		} else {

			// No POS information, just use individual tokens from question
			StringTokenizer l_QnST = new StringTokenizer(a_Question);
			while (l_QnST.hasMoreTokens()) {
				String l_Qn = l_QnST.nextToken();
				String l_Term = StripXMLChar(l_Qn);
				if (l_StopWords.IsStopWord(l_Term)) {
					continue;
				}
				l_Query = UpdateQuery(l_UsedTerms, l_Term, l_Query);
			} // end while

		} // end if (l_QuestionPOS.length() ...



		// Remove puntuation marks ',', '.', '?', '!' from the end of search terms
		l_Query = l_Query.replaceAll("[,\\.\\?!]", "");


		//
		return l_Query;

	} // end FormQuery()



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
	 * Retrieve the humber of hits when we send the query to a search engine.
	 *
	 * Un used because making use of the count of hits from the web doesn't seem useful.
	 * Keeping this around for reference, in case later this proves useful.
	 *
	 * @param a_Query [in] the query to use
	 * @return the number of hits as reported by the search engine, or 0 on errors
	 */
	private long GetNumberOfHits(String a_Query) {

		SearchEngineClient l_SearchEngine = new YahooClient();
		l_SearchEngine.setLabel("GetNumberOfHits-" + Math.random());
		l_SearchEngine.setQuery(a_Query);
		l_SearchEngine.setStartIndex(1);
		l_SearchEngine.setNumResults(1);
		SearchEngineResults l_SearchResults = null;
		try {
			l_SearchResults = l_SearchEngine.getResults();
		} catch (SearchEngineException e) {
			Logger.getLogger("QANUS").logp(Level.WARNING, FeatureScoringStrategy.class.getName(), "GetNumberOfHits", "Error doing search [" + a_Query + "]");
			return 0;
		}

		return l_SearchResults.getTotalResults();

	} // end GetNumberOfHits();


	/**
	 * Check whether a given candidate answer is a reasonable choice for HUM:gr.
	 * Invalid answers could be days of the week (i.e. Monday, etc)....
	 *
	 * We need this because tons of non-proper nouns get tagged as NNPs by our POS tagger.
	 *
	 * @param a_CandidateAnswer [in] candidate answer to consider
	 * @return true if the answer is reasonable, false otherwise
	 */
	private boolean IsReasonableAnswerForHUMGR(String a_CandidateAnswer) {

		String[] l_DaysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
		String[] l_MonthsOfYear = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
		String[] l_Countries = {"us", "uk", "france", "england", "cuba", "japan", "u.s", "america"};

		for (int i = 0; i < l_DaysOfWeek.length; ++i) {
			if (l_DaysOfWeek[i].compareToIgnoreCase(a_CandidateAnswer) == 0) {
				return false;
			}
		}
		for (int i = 0; i < l_MonthsOfYear.length; ++i) {
			if (l_MonthsOfYear[i].compareToIgnoreCase(a_CandidateAnswer) == 0) {
				return false;
			}
		}
		for (int i = 0; i < l_Countries.length; ++i) {
			if (l_Countries[i].compareToIgnoreCase(a_CandidateAnswer) == 0) {
				return false;
			}
		}

		return true;

	} // end IsReasonableAnswerForHUMGR()


	/**
	 * Processes a new search term and get a new query string with the term.
	 * Terms that are already used will not be added to the query however.
	 *
	 * @param a_UsedTerms [in] linkied list tracking terms that are already used.
	 * @param a_Term [in] the search term to add
	 * @param a_Query [in] the query to add to
	 * @return a new query string
	 */
	private String UpdateQuery(LinkedList<String> a_UsedTerms, String a_Term, String a_Query) {

		if (!a_UsedTerms.contains(a_Term)) {


			a_UsedTerms.add(a_Term);
			if (a_Query.length() > 0) {
				a_Query += " ";
			}
			a_Query += a_Term;
		}
		return a_Query;

	} // end UpdateQuery()


	/**
	 * Retrieve a list of the proper nouns found within the provided sentences tagged with POS info.
	 *
	 * We will check with a NER to ensure that we have picked up candidates marked as PERSONS only.
	 *
	 * We will go through the sentences sequentially.
	 * 
	 * The candidate answers will have their POS tags kept intact.
	 *
	 * @param a_Sentences [in] provided sentences to extract nouns from
	 * @param a_POSSentences [in] sentences with POS annotations. Correspond to a_Sentences.
	 * @return linked list of proper nouns within sentences, arranged in the order they are found, or null on errors
	 */
	private LinkedList<AnswerCandidate> GetProperNounsOfPersons(String[] a_Sentences, String[] a_POSSentences) {

		if (a_Sentences == null) {
			return null;
		}


		// Invoke NER
		String[] l_CandidateSentences = m_ModuleNER.ProcessText(a_Sentences);


		// Extract candidates
		Pattern l_NERPattern = Pattern.compile("(([A-Za-z-\\.,\\?!']+)/PERSON)([ ]+([A-Za-z-\\.,\\?!']+)/PERSON)*");
		int l_CurrIndex = 0;
		LinkedList<AnswerCandidate> l_Candidates = new LinkedList<AnswerCandidate>();
		for (String l_Sentence : l_CandidateSentences) {


			Matcher l_NERMatcher = l_NERPattern.matcher(l_Sentence);
			while (l_NERMatcher.find()) {
				String l_RawCandidate = l_Sentence.substring(l_NERMatcher.start(), l_NERMatcher.end());
				l_Candidates.add(new AnswerCandidate(l_RawCandidate, a_Sentences[l_CurrIndex]));
			} // end while

			l_CurrIndex++;

		} // end for

		return l_Candidates;

	} // end GetProperNounsOfPersons()


	/**
	 * Strips off the punctuation marks behind a string.
	 * These include
	 * , comma
	 * . fullstop
	 * ! exclaimation
	 * ? question
	 * ' apostrophe
	 *
	 * @param a_String [in] string to check for punctuations
	 * @return the string without the training punctuations.
	 */
	private String RemoveTrailingPunctuation(String a_String) {

		// Using regular expressiosn would be cleaner, but Java's support
		// for the '$' special regex symbol doesn't seem correct.
		// The regex pattern [ ,\\.\\?!']+$ did not work.

		while (a_String.endsWith(".")
				|| a_String.endsWith(",")
				|| a_String.endsWith("?")
				|| a_String.endsWith("!")
				|| a_String.endsWith("'")
				|| a_String.endsWith(" ")) {

			a_String = a_String.substring(0, a_String.length() - 1);

		} // end while

		return a_String;

	} // end a_String()

	/**
	 * Checks whether given string is a pronoun
	 * @param a_String [in] string to be checked
	 * @return true if the string is a pronoun, false otherwise.
	 */
	private boolean IsPronoun(String a_String) {

		String[] l_Pronouns = {"it", "we", "he", "she", "they", "our", "their"};
		for (String l_Pronoun : l_Pronouns) {
			if (a_String.compareToIgnoreCase(l_Pronoun) == 0) {
				return true;
			}
		}
		return false;

	} // end IsPronoun()

	/**
	 * We can identify the "subject" of a Type 1 HUM:ind question
	 * A Type 1 question include questions like
	 * Who is the president?
	 * Who is the chief executive of Merill Lynch?
	 * 
	 * The subject in these cases are president and chief execute of Merill Lynch respectively.
	 * 
	 * @param a_QuestionText [in] the question, must be of type HUM:ind TYPE1 or TYPE1_IC
	 * @param a_QuestionTextPOS [in] the question with POS annotations
	 * @param a_SubType [in] subtype of the question (either HUM_IND_TYPE1 or HUM_IND_TYPE1_IC
	 *
	 * @return a string expressing the subject of the question, or null on errors
	 */
	private String GetSubjectOfHumIndQuestion(String a_QuestionText, String a_QuestionTextPOS, QuestionSubType a_Type) {

		String l_Subject = "";

		switch (a_Type) {
			case HUM_IND_TYPE1_IC:
				//Pattern l_Pattern1 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)");
				Pattern l_Pattern1 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+)");
				//Pattern l_Pattern2 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)");
				Pattern l_Pattern2 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+)");

				Matcher l_Matcher1 = l_Pattern1.matcher(a_QuestionTextPOS);
				Matcher l_Matcher2 = l_Pattern2.matcher(a_QuestionTextPOS);
				if (l_Matcher1.matches()) {
					for (int i = 0; i < l_Matcher1.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher1.group(i);
						} else {
							l_Matcher1.group(i);
						}
					}
				} else if (l_Matcher2.matches()) {
					for (int i = 0; i < l_Matcher2.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher2.group(i);
						} else {
							l_Matcher2.group(i);
						}
					}
				} else {
					l_Subject = "";
				}
				break;
			case HUM_IND_TYPE1:
				Pattern l_Pattern3 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))");
				Pattern l_Pattern4 = Pattern.compile("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))");
				Matcher l_Matcher3 = l_Pattern3.matcher(a_QuestionTextPOS);
				Matcher l_Matcher4 = l_Pattern4.matcher(a_QuestionTextPOS);
				if (l_Matcher3.matches()) {
					for (int i = 0; i < l_Matcher3.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher3.group(i);
						} else {
							l_Matcher3.group(i);
						}
					}
				} else if (l_Matcher4.matches()) {
					for (int i = 0; i < l_Matcher4.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher4.group(i);
						} else {
							l_Matcher4.group(i);
						}
					}
				} else {
					l_Subject = "";
				}
				break;
			case HUM_IND_TYPE2:
				Pattern l_Pattern5 = Pattern.compile("[wW]ho/WP( )([^w]+/VBD(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))");
				Matcher l_Matcher5 = l_Pattern5.matcher(a_QuestionTextPOS);
				if (l_Matcher5.matches()) {
					for (int i = 0; i < l_Matcher5.groupCount(); ++i) {
						if (i == 2) {
							l_Subject = l_Matcher5.group(i);
						} else {
							l_Matcher5.group(i);
						}
					}
				} else {
					l_Subject = "";
				}
				break;
			default:
				// Error
				l_Subject = "";
				break;
		} // end switch


		// Need to strip away POS and trailing question marks
		l_Subject = l_Subject.replaceAll("\\?", "");
		l_Subject = l_Subject.replaceAll("/[A-Z]+", "");


		return l_Subject;


	} // end GetSubjectOfHumIndQuestion()


	/**
	 * Attempt to further classify questions into certain specially identified sub-types.
	 *
	 * Currently two sub-types of HUM:ind questions are identified:
	 *  HUM_IND_TYPE1 and HUM_IND_TYPE1_IC:
	 *    Who is the CEO of Microsoft?
	 *    Who is the CEO? (incomplete type - IC)
	 *  HUM_IND_TYPE2
	 *    Who nominated Miers for the post?
	 *    Who supervised the transplant?
	 *
	 * @param a_QuestionText [in] the question
	 * @param a_QuestionTextPOS [in] the question with POS annotations
	 * @param a_MainType [in] the annotated question classification (according to Li and Roth)
	 * @return The identified sub-type of the question.
	 */
	private QuestionSubType GetQuestionSubType(String a_QuestionText, String a_QuestionTextPOS, String a_MainType) {


		QuestionSubType l_SubType = QuestionSubType.OTHERS;


		if (a_MainType.compareToIgnoreCase("HUM:ind") == 0) {

			// Check for TYPE 1 (hum:ind) questions
			// Of the format "Who is|was < -- Noun Phrase -- >?
			// i.e. Who is director of the mint?
			//      Who is the chief executive of WWE?
			// Incomplete TYPE 1 questions include:
			//      Who is the chairman? (missing the "of...")

			if (a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)")
					|| a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)*([ A-Za-z0-9'\\-\\?]+/NN)+)")) {
				l_SubType = QuestionSubType.HUM_IND_TYPE1_IC;
			} else if (a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD)(([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))")
					|| a_QuestionTextPOS.matches("[wW]ho/WP (is/VBZ|was/VBD) the/DT (([ A-Za-z0-9'\\-]+/NN)+ of/IN(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))")) {
				l_SubType = QuestionSubType.HUM_IND_TYPE1;
			} else if (a_QuestionTextPOS.matches("[wW]ho/WP( )([^w]+/VBD(([ A-Za-z0-9'\\-]+/[A-Z]+)*([ A-Za-z0-9'\\-\\?]+/[A-Z]+)+))")) {
				l_SubType = QuestionSubType.HUM_IND_TYPE2;
			}


		}

		return l_SubType;


	} // end GetQuestionSubType()

	
} // end class FeatureScoringStrategy

