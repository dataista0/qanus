package sg.edu.nus.wing.qanus.stock.eval;


import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IEvaluationMetric;



/**
 * Base class for evaluation modules.
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 */
public abstract class EvaluationMetric implements IEvaluationMetric {

	// Correct and generated answer representations
	protected DataItem m_CorrectAnswers;
	protected DataItem m_GeneratedAnswers;


	/**
	 * Assigns the set of generated answers.
	 * @param a_Item [in] set of generated answers
	 */
	public void SetGeneratedAnswers(DataItem a_Item) {
		m_GeneratedAnswers = a_Item;
	}


	/**
	 * Assigns the set of correct answers.
	 * @param a_Item [in] set of correct answers
	 */
	public void SetCorrectAnswers(DataItem a_Item) {
		m_CorrectAnswers = a_Item;
	}

} // end class EvaluationMetric
