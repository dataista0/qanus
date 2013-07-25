package sg.edu.nus.wing.qanus.framework.commons;

/**
 * Interface implemented by front-ends to information bases.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version v18Jan2010
 */
public interface IInformationBaseQuerier {


	/**
	 * Performas a search through the information base for relevant information that can answer
	 * a specified query.
	 *
	 * The interpretation of the returned result is subject to implementation.
	 * 
	 *
	 * @param a_Query [in] the query the information base should respond to
	 * @return a set of relevant info pertinent to the query.
	 */
	public Object SearchQuery(String a_Query);

} // end interface IInformationBaseQuerier
