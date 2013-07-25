package sg.edu.nus.wing.qanus.mitic.ar.er;



import java.io.File;
import java.util.HashMap;

import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalyzer;
import sg.edu.nus.wing.qanus.framework.commons.DataItem;


/**
 * Integrates with the answer retriever components and performs error analysis.
 *
 * Helps to find out how much errors are introduced at various steps within answer retrieval.
 *
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Feb 1, 2010
 */
public class FactoidPipelineErrorAnalyzer extends ErrorAnalyzer {


	private HashMap<String,StageStatistics> m_Statistics;


	/**
	 * Constructor
	 *
	 * @param a_InfoSource [in] folder containing information that will be useful for error analysis
	 * @param a_TargetFile [in] file where results from error analysis is written to
	 */
	public FactoidPipelineErrorAnalyzer(File a_InfoSource, File a_TargetFile) {

		super(a_InfoSource, new AnalysisInfoSourceReader(a_InfoSource), a_TargetFile);

		m_Statistics = new HashMap<String, StageStatistics>();
		

	} // end constructor
	


	/**
	 * Determines how many questions can potentially be answered correctly with the given set of current candidates
	 * for a particular question
	 *
	 * This is done by doing a substring match within the candidates. If the correct answer can be found as a substring
	 * of these candidates, we note that the question can potentially be answered.
	 *
	 * This way of performing the analysis does not regardthe context of the answer, and thus may not be the best.
	 * But it required substantially less implementation effort.
	 *
	 * @a_Info a_Info [in] identified candidates for question.
	 * @return results of analysis
	 */
	public DataItem PerformAnalysisOnQuestionAndCandidates(DataItem a_Info) {


		// Get question attributes and ID
		String l_QID = a_Info.GetAttribute("QID");

		// Get correct answers
		String[] l_CorrectAnswers = GetErrorAnalysisInfoSourceReader().GetCorrectAnswersForQID(l_QID);
		if (l_CorrectAnswers == null) return null;
		
		DataItem l_Result = new DataItem("AnalysisResult");
		l_Result.AddAttribute("QID", l_QID);

		// List out various stages and check their answers
		String[] l_FieldNames = a_Info.GetAllFieldNames();
		for (String l_FieldName : l_FieldNames) {

			boolean l_Match = false;

			// Get candidates and check if any of them match the list of correct answers
			DataItem[] l_CandidateItems = a_Info.GetFieldValues(l_FieldName);
			if (l_CandidateItems == null || l_CandidateItems.length == 0) continue;
			for (DataItem l_CandidateItem : l_CandidateItems) {
				String[] l_Candidates = l_CandidateItem.GetValue();
				if (l_Candidates == null || l_Candidates.length == 0) continue;
				for (String l_Candidate : l_Candidates) {
					for (String l_CorrectAnswer : l_CorrectAnswers) {

						if (l_Candidate.contains(l_CorrectAnswer)) {
							l_Match = true;
						}

					} // end for
					if (l_Match) break;
				} // end for
				if (l_Match) break;
			} // end for
			
			
			StageStatistics l_StageStats = null;
			if (m_Statistics.containsKey(l_FieldName)) {
				l_StageStats = m_Statistics.get(l_FieldName);
			} else {
				l_StageStats = new StageStatistics();
				m_Statistics.put(l_FieldName, l_StageStats);
			}

			if (l_Match) {
				l_Result.AddField(l_FieldName, "Pass");
				l_StageStats.AddPotentiallyCorrect();
			} else {
				l_Result.AddField(l_FieldName, "Fail");
				l_StageStats.AddWrong();
			}

		} // end for

		WriteDataItemToFile (l_Result);
		return l_Result;

	} // end PerformAnalysis



	/**
	 * Housekeeping --- after analysis, we tabulate overall results and save the tabulated scores
	 * to the output file.
	 */
	public void FinishedAnalysis() {

		DataItem l_Item = new DataItem("Overall");
		for (String l_FieldName : m_Statistics.keySet()) {
			StageStatistics l_Stats = m_Statistics.get(l_FieldName);
			if (l_Stats == null) continue;

			l_Item.AddField(l_FieldName, Double.toString(l_Stats.GetPercentagePotentiallyCorrect()));
		}

		WriteDataItemToFile(l_Item);
				
	} // end FinishedAnalysis



} // end class FactoidPipelineErrorAnalyzer

