package sg.edu.nus.wing.qanus.framework.commons;

/**
 * Interface to be implemented by modules to the Evaluation stage.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 16Jan2010
 */
public interface IEvaluationMetric extends IRegisterableModule {


	/**
	 * Retrieve a summary of the tabulated results.
	 *
	 * Any desired/required statistics can be returned in the data item.
	 *
	 * @return Data item of evaluation results
	 */
	public DataItem GetEvaluationSummary();

	/**
	 * After parsing the generated answers by the system, input them to this class for tabulation subsequently.
	 * @param a_Item [in] data item containing all generated answers
	 */
	public void SetGeneratedAnswers(DataItem a_Item);

	/**
	 * After parsing the correct answers, input them to this class for tabulation subsequently.
	 * @param a_Item [in] data item containing the gold-standard answers
	 */
	public void SetCorrectAnswers(DataItem a_Item);

} // end interface IEvaluationMetric
