package sg.edu.nus.wing.qanus.framework.commons;



/**
 * An interface every answer retrieval strategy module should implement.
 * The strategy module will be invoked and used in the answer retrieval stage.
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public interface IStrategyModule extends IRegisterableModule {


	/**
	 * Given a question, retrieve the answer for it.
	 *
	 * @param a_QuestionItem [in] the input question
	 * @return DataItem structure with string containing retrieved answer for specified question
	 */
	public DataItem GetAnswerForQuestion(DataItem a_QuestionItem);


} // end interface IStrategyModule
