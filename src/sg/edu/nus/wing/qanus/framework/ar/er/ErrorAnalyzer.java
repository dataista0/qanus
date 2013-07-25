package sg.edu.nus.wing.qanus.framework.ar.er;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public abstract class ErrorAnalyzer {


	// Holds the source and target folders/files
	private File m_InfoSource;
	private File m_TargetFile;

	// Parses input information that will be useful for error analysis
	private ErrorAnalysisInfoSourceReader m_InfoSourceReader;



	/**
	 * Constructor
	 *
	 * @param a_InfoSource [in] folder containing information that will be useful for error analysis
	 * @param a_TargetFile [in] file where results from error analysis is written to
	 */
	public ErrorAnalyzer(File a_InfoSource, ErrorAnalysisInfoSourceReader a_SourceReader, File a_TargetFile) {

		m_InfoSource = a_InfoSource;
		m_InfoSourceReader = a_SourceReader;

		m_TargetFile = a_TargetFile;
		try {
			// Flush output file if it exists
			FileWriter l_FOS = new FileWriter(m_TargetFile, false);
			l_FOS.close();
		} catch (FileNotFoundException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, ErrorAnalyzer.class.getName(), "ErrorAnalyzer", "Unable to access output file.", ex);
		} catch (IOException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, ErrorAnalyzer.class.getName(), "ErrorAnalyzer", "Problems accessing output file.", ex);
		}

	} // end constructor


	/**
	 * Retreives handle to the information source parser
	 * @return handle to info source parser
	 */
	protected ErrorAnalysisInfoSourceReader GetErrorAnalysisInfoSourceReader() {
		return m_InfoSourceReader;
	} // end GetErrorAnalysisInfoSourceReader()


	/**
	 * Performs analysis for a particular question.
	 * The type of analysis can be customised depending on your requirements
	 *
	 * @param a_Info [in] information needed for the analysis
	 * @return results of analysis
	 */
	public abstract DataItem PerformAnalysisOnQuestionAndCandidates(DataItem a_Info);


	/**
	 * Infoms the analyzer that analysis is completed.
	 * Clean up can be done here.
	 */
	public abstract void FinishedAnalysis();



	/**
	 * Saves a set of analysis results to file.
	 *
	 * @param a_Info [in] analysis results
	 * @return true on success, false on any errors
	 */
	protected boolean WriteDataItemToFile(DataItem a_Info) {

		try {

			// Prepare to write to the file
			FileWriter l_FOS = new FileWriter(m_TargetFile, true);
			BufferedWriter l_BW = new BufferedWriter(l_FOS);

			// Write the results to the file
			l_BW.write(a_Info.toXMLString());

			// Save and close file
			l_FOS.flush();
			l_FOS.close();
			l_BW.flush();
			l_BW.close();
			

		} catch (FileNotFoundException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, ErrorAnalyzer.class.getName(), "AddAnalysisResults", "Unable to access output file.", ex);
			return false;
		} catch (IOException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, ErrorAnalyzer.class.getName(), "AddAnalysisResults", "Problems accessing output file.", ex);
			return false;
		}

		return true;

	} // end WriteDataItemToFile



} // end class ErrorAnalyzer

