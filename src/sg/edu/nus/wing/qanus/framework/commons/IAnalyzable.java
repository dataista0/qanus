package sg.edu.nus.wing.qanus.framework.commons;




/**
 * This interface is to be implemented by IStrategyModules
 * which are amenable to error analysis.
 *
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Feb 2, 2010
 */
public interface IAnalyzable {

	/**
	 * Retrieves information that will be useful for analysis by the error analysis
	 * engine
	 *
	 * @param a_QuestionItem [in] structure containing question item	 
	 * @return structure containing analysis information
	 */
	public DataItem GetAnalysisInfoForQuestion(DataItem a_QuestionItem);


} // end interface
