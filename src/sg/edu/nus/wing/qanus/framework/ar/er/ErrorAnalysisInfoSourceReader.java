/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sg.edu.nus.wing.qanus.framework.ar.er;

import java.io.File;

/**
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Feb 4, 2010
 */
public abstract class ErrorAnalysisInfoSourceReader {


	private File m_SourceFile;


	/**
	 * Constructor
	 *
	 * @param a_Source [in] file containing source information
	 */
	public ErrorAnalysisInfoSourceReader(File a_Source) {
		m_SourceFile = a_Source;
	} // end constructor


	/**
	 * Retrieve the set of correct answers for a given question from the info source.
	 *
	 * @param a_QID [in] the ID of the desired question
	 * @return array of strings of the correct answers, or null on errors.
	 */
	public abstract String[] GetCorrectAnswersForQID(String a_QID);

	
} // end class ErrorAnalysisInfoSourceReader
