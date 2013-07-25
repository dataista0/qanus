
package sg.edu.nus.wing.qanus.stock.eval;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;


/**
 * This module goes through a set of correct and generated answers.
 * The accuracy of the generated answers is computed. Accuracy is defined
 * as
 * the number of correctly generated answers / the number of correct answers
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 */
public class FactoidPrecisionMetric extends EvaluationMetric  {

	
	public String GetModuleID() {
		return "FactoidPrecision";
	}



	/**
	 * Evaluates the generated answers relative to the "gold standard" answers.
	 * Note that the implementation here ties in with the DataItem structure returned
	 * by CorrectAnswersXMLParser and GeneratedAnswersXMLParser.
	 * Any changes to DataItem in the two parser classes will thus need to be
	 * brought into here too.
	 *
	 * @return DataItem containing answer, or null on errors
	 */
	public DataItem GetEvaluationSummary() {

		// Sanity check
		if (m_CorrectAnswers == null || m_GeneratedAnswers == null) return null;


		// The use of "Answer" here ties in with the implementation inside CorrectAnswersXMLParser
		DataItem[] l_CorrectAnswers = m_CorrectAnswers.GetFieldValues("Answer");
		DataItem[] l_GeneratedAnswers = m_GeneratedAnswers.GetFieldValues("Answer");
				

		// Used for tabulations - only for factoid
		int l_NumGeneratedCorrectly = 0;
		// Go through all the correct answers, and keep track of the distinct occurences
		// of question IDs. The same question ID can have several acceptable answers.
		HashMap<String,String> l_QuestionIDsInGoldStandard = new HashMap<String,String>();
		for (DataItem l_CorrectAnswer : l_CorrectAnswers) {			
			String l_QID = l_CorrectAnswer.GetAttribute("QID");
			l_QuestionIDsInGoldStandard.put(l_QID, l_QID);
		}

		// Used to hold tabulation results
		DataItem l_Result = new DataItem(GetModuleID());

		
		// Walk through the generated answers and mark each of them
		for (DataItem l_GeneratedAnswer : l_GeneratedAnswers) {

			DataItem l_Item = new DataItem("ResultItem");
			
			try {
				
				String l_QID = l_GeneratedAnswer.GetAttribute("QID");								
				DataItem[] l_GeneratedAnswerItem = l_GeneratedAnswer.GetFieldValues("Answer");

				// Additional fields, useful for aggregation
				DataItem[] l_GeneratedAnswerType = l_GeneratedAnswer.GetFieldValues("QuestionType");

				String[] l_GeneratedAnswerString = l_GeneratedAnswerItem[0].GetValue();
				if (l_GeneratedAnswerItem == null || l_GeneratedAnswerString == null || l_GeneratedAnswerString[0].length() == 0) {
					continue;
				}
				
				l_Item.AddAttribute("QID", l_QID);
				l_Item.AddField("GeneratedAnswer", l_GeneratedAnswerItem[0].GetValue()[0]);
				l_Item.AddField("QuestionType", l_GeneratedAnswerType[0].GetValue()[0]);

				// Try to find the corresponding generated answer
				for (DataItem l_CorrectAnswer : l_CorrectAnswers) {
					if (l_CorrectAnswer.GetAttribute("QID").compareToIgnoreCase(l_QID) == 0) {
						String[] l_AnswerString = l_CorrectAnswer.GetValue();
						if (l_AnswerString == null) {

						} else {
							
							if (l_AnswerString[0].length() > 0) {
								l_Item.AddField("CorrectAnswer", l_AnswerString[0]);
								if (l_GeneratedAnswerString[0].compareToIgnoreCase(l_AnswerString[0]) == 0) {
									l_Item.AddAttribute("RESULT", "Correct");
									l_NumGeneratedCorrectly++;
									// If we find that the answer is correct, we can stop
									// Otherwise continue searching if there is an alternative "correct" answer
									break;
								}
							}
						}						
					}
				}

				l_Result.AddField("ResultItem", l_Item);
				
			} catch (Exception ex) {								
				Logger.getLogger("QANUS").logp(Level.WARNING, FactoidPrecisionMetric.class.getName(), "GetEvaluationSummary", "Skipped one answer", ex);				
			}

		} // end for


		double l_Accuracy = (double) l_NumGeneratedCorrectly / l_QuestionIDsInGoldStandard.size();
		l_Result.AddAttribute("Accuracy", new Double(l_Accuracy).toString());
		l_Result.AddAttribute("TotalFactoid", Integer.toString(l_QuestionIDsInGoldStandard.size()));
		l_Result.AddAttribute("TotalCorrect", Integer.toString(l_NumGeneratedCorrectly));


		return l_Result;
		
	} // end GetEvaluationSummary()



} // end class FactoidPrecisionMetric
