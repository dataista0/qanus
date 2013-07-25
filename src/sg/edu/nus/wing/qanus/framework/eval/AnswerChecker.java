package sg.edu.nus.wing.qanus.framework.eval;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IEvaluationMetric;
import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.framework.commons.IXMLDataReceipient;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;
import sg.edu.nus.wing.qanus.framework.commons.StageEngine;




/**
 * Compares a set of generated answers with a gold-standard and present desired scores
 * according to prescribed evaluation metrics.
 *
 * This is supposed to be a generic module. To compute different scores, we just need to
 * register more evaluation modules.
 *
 * However currently one limitation of the design is that a specific input files are expected
 * for the generated and correct answers. These files do not contain the type (i.e. factoid,
 * list, etc) of the questions.
 *
 * The current implementation takes in the gold-standard factoid answers, and list questions
 * are not checked. Just adding a module is not sufficient unless
 * 1. More types of input files are accepted, take in a folder maybe
 * 2. More information is included in the XML of the answers
 *
 * For now, we are satisifed with just ensuring the accuracy of factoid answers. But definitely
 * this has to change as QANUS develops to handle other types of questions
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version v16Jan2010
 */
public class AnswerChecker extends StageEngine implements IXMLDataReceipient {


	// Name of file where output is placed
	protected String m_OutputFileName;

	// Hold all the retrieved answer strings until they are ready to be written to file.
	protected DataItem m_DataItem_Results;

	// We require two XML handlers, but the parent class only has 1
	// So we add 1 more here
	private IXMLParser m_CorrectAnswerXMLHandler;

	// Pointer to the files which hold the "correct" and "generated" answers
	private File m_CorrectFile, m_GeneratedFile;

	// Pointer to file where output is written to
	private BufferedWriter m_CurrentOutputFile;


	// There could be race conditions with these variables, but it is not fatal
	// and is not expected to give us problems.
	// But if the code is to be changed and modified, please look through uses
	// of these two variables, and introduce synchronisation if necessary!
	private boolean m_GeneratedAnswersReceived, m_CorrectAnswersReceived;
	
	



	/**
	 * Constructor.
	 * @param a_CorrectFile [in] file name of file storing correct answers
	 * @param a_GeneratedFile [in] file name of file storing generated answers
	 * @param a_ResultsFolder [in] the folder name where the result file will be stored
	 */
	public AnswerChecker(File a_CorrectFile, File a_GeneratedFile, File a_ResultsFolder) {

		super();
		
		SetTargetFile(a_ResultsFolder);
		m_CorrectFile = a_CorrectFile;
		m_GeneratedFile = a_GeneratedFile;


		m_CorrectAnswerXMLHandler = null;
		

		m_DataItem_Results = new DataItem("DOCSTREAM");

		// Get the current date and time to append to the output file name
		java.util.Date l_Date = new java.util.Date();
		java.text.SimpleDateFormat l_DateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String l_DateString = l_DateFormat.format(l_Date);


		// Set up name of file where we store our output
		m_OutputFileName = GetTargetFile().getAbsolutePath() + "\\eval" + l_DateString + ".xml";

		m_GeneratedAnswersReceived = false;
		m_CorrectAnswersReceived = false;

	} // end constructor
	

	public String GetIdentifier() {
		return "AnswerChecker";
	}


	/**
	 * Determines the XML handler to use for the XML file of generated answers.
	 * @param a_Parser [in] the XML handler to use
	 */
	public void SetGeneratedAnswersXMLHandler(IXMLParser a_Parser) {
		this.SetXMLHandler(a_Parser);
	} // end SetGeneratedAnswersXMLHandler()


	/**
	 * Determines the XML handler to use for the XML file of correct answers.
	 * @param a_Parser [in] the XML handler to use
	 */
	public void SetCorrectAnswersXMLHandler(IXMLParser a_Parser) {
		m_CorrectAnswerXMLHandler = a_Parser;
	} // end SetCorrectAnswersXMLHandler()


	/**
	 * This class will get notified when the XML parsers have finished parsing the input files
	 * containing the answers.
	 * @param a_Item [in] the parsed answer structure.
	 */
	public void Notify(DataItem a_Item) {

		// Error check
		if (a_Item == null) {
			Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Notify", "Unexpected null data item.");
			return;
		}


		// Notify each registered text module
		for (IRegisterableModule l_Module : m_ModuleList) {

			IEvaluationMetric l_EvalModule = null;
			if (l_Module instanceof IEvaluationMetric) {
				l_EvalModule = (IEvaluationMetric) l_Module;
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Notify", "Wrong module type.");
				continue;
			}

			// Pass data items to the evaluation module
			// Note that we can get notified by two different XML handlers
			// One for the generated answers, the other for correct answers.
			if (a_Item.GetXMLTag().compareToIgnoreCase("GeneratedAnswers") == 0) {
				l_EvalModule.SetGeneratedAnswers(a_Item);
				m_GeneratedAnswersReceived = true;
			} else if (a_Item.GetXMLTag().compareToIgnoreCase("CorrectAnswers") == 0) {
				l_EvalModule.SetCorrectAnswers(a_Item);
				m_CorrectAnswersReceived = true;
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Notify", "Unknown data item.");
			}
		} // end for


	} // end Notify()



	/**
	 * Start to verify all the provided answers and output the results into the
	 * output folder.
	 * 
	 * @return true if all questions processed correctly, false on any errors
	 */
	@Override
	public boolean Go() {


		// Error check? null targetfolder?



		// Register ourself to receive notifications when parsing XML files.
		GetXMLHandler().RegisterForNotification(this); // Generated answers
		m_CorrectAnswerXMLHandler.RegisterForNotification(this); // Correct answers


		// Set up the SAX parser ------------------------------------------
		SAXParserFactory l_Factory = SAXParserFactory.newInstance();
		l_Factory.setValidating(true);
		SAXParser l_DocBuilder = null;
		try {
			l_DocBuilder = l_Factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerChecker.class.getName(), "Go", "Error setting up parser", e);
		} catch (SAXException e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerChecker.class.getName(), "Go", "Error setting up parser", e);
		}

	
	
		// Start parsing generated answers file
		try {
			// TODO sloppy work to just cast as DefaultHandler
			// can we marry the inheritance from DefaultHandler and the interface
			// IXMLParser better?
			l_DocBuilder.parse(m_GeneratedFile, (DefaultHandler) GetXMLHandler());
		} catch (SAXException e) {			
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerChecker.class.getName(), "Go", "Error processing [" + m_GeneratedFile + "]", e);
		} catch (IOException e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerChecker.class.getName(), "Go", "Error processing [" + m_GeneratedFile + "]", e);
		}
		// And the correct answers file
		try {
			l_DocBuilder.parse(m_CorrectFile, (DefaultHandler) m_CorrectAnswerXMLHandler);
		} catch (SAXException e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerChecker.class.getName(), "Go", "Error processing [" + m_CorrectFile + "]", e);
		} catch (IOException e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerChecker.class.getName(), "Go", "Error processing [" + m_CorrectFile + "]", e);
		}


		// Wait for documents to be parsed
		while (!m_CorrectAnswersReceived || !m_GeneratedAnswersReceived) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				// exception, forget it don't get ourselves into a potentially
				// sticky infinite loop
				break;
			}
		}


		// Stop receiving notifications about new data items
		GetXMLHandler().DelistFromNotification(this); // Generated answers
		m_CorrectAnswerXMLHandler.DelistFromNotification(this); // Correct answers


		// Prepare output file
		try {
				m_CurrentOutputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m_OutputFileName), "UTF8"));
				m_CurrentOutputFile.write("<DOCSTREAM>");
			} catch (Exception e) {
				m_CurrentOutputFile = null;
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Go", "Error preparing temp file : [" + m_OutputFileName + "]", e);
			}

		// Now we can get the results
		for (IRegisterableModule l_Module : m_ModuleList) {

			IEvaluationMetric l_EvalModule = null;
			if (l_Module instanceof IEvaluationMetric) {
				l_EvalModule = (IEvaluationMetric) l_Module;
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Go", "Wrong module type.");
				continue;
			}

			DataItem l_Results = l_EvalModule.GetEvaluationSummary();
			try {
				m_CurrentOutputFile.write(l_Results.toXMLString());
				m_CurrentOutputFile.flush();
			} catch (IOException ex) {
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Go", "Error writing answer to [" + m_OutputFileName + "]", ex);
			}


		} // end for

		// Save output file -------------------------------------------------------
		if (m_CurrentOutputFile != null) {
			try {
				m_CurrentOutputFile.write("</DOCSTREAM>");
				m_CurrentOutputFile.close();
				m_CurrentOutputFile = null;
			} catch (IOException e) {				
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerChecker.class.getName(), "Go", "Error saving [" + m_OutputFileName + "]", e);
			}
		} // end if


		return true;


	} // end Go()

	
	
} // end class AnswerChecker
