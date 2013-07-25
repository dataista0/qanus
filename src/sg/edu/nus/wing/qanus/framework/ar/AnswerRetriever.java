package sg.edu.nus.wing.qanus.framework.ar;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalyzer;
import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IAnalyzable;
import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.framework.commons.IStrategyModule;
import sg.edu.nus.wing.qanus.framework.commons.IXMLDataReceipient;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;
import sg.edu.nus.wing.qanus.framework.commons.StageEngine;
import sg.edu.nus.wing.qanus.framework.util.DirectoryAndFileManipulation;


/**
 * Looks at given questions and find the answers for the questions from the
 * knowledge base. This is done by making use of registered retrieval
 * strategy modules.
 *
 * Currently only 1 strategy module is supported, in the sense that we do not
 * have any module to choose between the answers of different strategy modules.
 *
 * TODO Ranker
 * The results of these strategy modules (if more than 1) can be optionally
 * combined with a ranker. This is planned for subsequent releases
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class AnswerRetriever extends StageEngine implements IXMLDataReceipient {


	// Name of file where output is placed
	protected String m_OutputFileName;

	// Hold all the retrieved answer strings until they are ready to be written to file.
	protected DataItem m_DataItem_Results;

	// Keeps track if the callbacks of the XML handler has all been completed
	// We need to be sure of this as we only write all results back to file
	// after the last callback has completed -- this is to make sure
	// we do not miss any result returned by the callback if we write
	// ahead of the callback.
	protected int l_CallbacksOutstanding = 0;

	// Error analysis module
	protected ErrorAnalyzer m_ErrorAnalyzer = null;


	/**
	 * Constructor.
	 * @param a_QuestionsSource [in] folder containing annotated questions in XML files
	 * @param a_ResultFolder [in] folder to write answers to, in the form of XML files
	 */
	public AnswerRetriever(File a_QuestionsSource, File a_ResultFolder) {

		super();

		SetSourceFile(a_QuestionsSource);
		SetTargetFile(a_ResultFolder);


		m_DataItem_Results = new DataItem("DOCSTREAM");


		// If the questions are given in a folder, we will create a answer file with a name including the current date string
		// If the questions are given in a file, we will create a answer file with the same name, appended with the word "answers.xml"

		if (a_QuestionsSource.isFile()) {

			m_OutputFileName = GetTargetFile().getAbsolutePath() + File.separator + a_QuestionsSource.getName() + "-answers.xml";

		} else {

			// Get the current date and time to append to the output file name
			java.util.Date l_Date = new java.util.Date();
			java.text.SimpleDateFormat l_DateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			String l_DateString = l_DateFormat.format(l_Date);

			// Set up name of file where we store our output
			m_OutputFileName = GetTargetFile().getAbsolutePath() + File.separator + "answers" + l_DateString + ".xml";

		}

		
		
	} // end constructor


	/**
	 * Constructor
	 * @param a_QuestionsSource [in] folder containing annotated questions in XML files
	 * @param a_ResultFolder [in] folder to write answers to, in the form of XML files
	 * @param a_Parser [in] the parser to use to process the input questions XML files
	 */
	public AnswerRetriever(File a_QuestionsFolder, File a_ResultFolder, IXMLParser a_Parser) {
		this(a_QuestionsFolder, a_ResultFolder);
		SetXMLHandler(a_Parser);
	} // end constructor


	/**
	 * Sets the error analysis engine to use with this stage engine.
	 * If not set, no error analysis will be carried out
	 *
	 * @param a_ErrorAnalyzer [in] error analysis engine to use
	 */
	public void SetErrorAnalysisEngine(ErrorAnalyzer a_ErrorAnalyzer) {

		m_ErrorAnalyzer = a_ErrorAnalyzer;
		
	} // end SetErrorAnalysisEngine()


	/**
	 * Start to retrieve answers for all of the questions found in the question folder
	 * and output the answers in the result folder.
	 * @return true if all questions processed correctly, false on any errors
	 */
	@Override
	public boolean Go() {

		// Since the questions are read in callback fashion
		// we will start the questions parsing, then on each call back,
		// activate the various strategies and retrieve their answers.
		// Subsequently we rank the answers and post-process

		
		// Register ourself to receive notifications when parsing XML file.
		// This call works because of dynamic binding in Java, where "this"
		// is associated to the derived subclass of TextProcessor.
		GetXMLHandler().RegisterForNotification(this);


		// Set up the SAX parser ------------------------------------------
		SAXParserFactory l_Factory = SAXParserFactory.newInstance();
		l_Factory.setValidating(true);
		SAXParser l_DocBuilder = null;
		try {
			l_DocBuilder = l_Factory.newSAXParser();
		} catch (ParserConfigurationException e) {			
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error setting up parser", e);
		} catch (SAXException e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error setting up parser", e);
		}


		// The input source file could be a folder or a file
		// We try to make the code generic by expanding the list of file names to be processed into
		// a linked list.
		LinkedList<String> l_ListOfFileNames = null;
		if (GetSourceFile().isDirectory()) {

			// Is a directory

			// The source folder could contain main nested directories,
			// we will collect all the file names in these directories first
			// so that it is easier to process them subsequently.
			// We collect the file names and not the files to save on memory requirements.
			// We will instantiate the java.io.File object as and when needed instead.
			l_ListOfFileNames = DirectoryAndFileManipulation.CollectFileNamesInDirectory(GetSourceFile());

		} else {

			// Is a file
			l_ListOfFileNames = new LinkedList<String>();
			l_ListOfFileNames.add(GetSourceFile().getAbsolutePath());

		}			

		// Process each file, sending it into the XML parser. -----------------
		for (String l_FileNameToParse : l_ListOfFileNames) {
						
			// Start parsing -------------------------------------------------------
			File l_FileToParse = new File(l_FileNameToParse);
			try {
				
				// TODO sloppy work to just cast as DefaultHandler
				// can we marry the inheritance from DefaultHandler and the interface
				// IXMLParser better?
				NewCallBack(); // Used later on the ensure that all invokved callbacks have completed before we stop
				l_DocBuilder.parse(l_FileToParse, (DefaultHandler) GetXMLHandler());

			} catch (SAXException e) {
				Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error processing [" + l_FileNameToParse + "]", e);
			} catch (IOException e) {
				Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error processing [" + l_FileNameToParse + "]", e);
			}

		} // end for

		
		// Stop receiving notifications about new data items
		GetXMLHandler().DelistFromNotification(this);


		// Make sure we don't write to file until all callbacks have completed
		while (AnyCallBacksLeft()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				// exception, forget it don't get ourselves into a potentially
				// sticky infinite loop
				break;
			}
		}
		WriteResultsToFile();

		
		// Signal the error analysis engine that everything is over.
		if (m_ErrorAnalyzer != null) {
			m_ErrorAnalyzer.FinishedAnalysis();
		}

		return true;

	} // end Go()



	/**
	* Identifying string.
	* @return a string that uniquely identifies this module
	*/
	public String GetIdentifier() {
		return "GenericAnswerRetriever";
	} // end GetIdentifier()


	public synchronized void Notify(DataItem a_Item) {

		HashMap<String,DataItem> l_Answers = new HashMap<String,DataItem>();
		DataItem l_RankedAnswerTODO = null;

		// a_Item contains the question and its annotations
		for (IRegisterableModule l_Module : m_ModuleList) {

			// TODO questions do not follow an order now

			//System.out.println(a_Item.toXMLString()); // Display received question for debugging if needed

			IStrategyModule l_StrategyModule = null;
			if (l_Module instanceof IStrategyModule) {
				l_StrategyModule = (IStrategyModule) l_Module;			
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, AnswerRetriever.class.getName(), "Notify", "Wrong module type.");
				continue;
			}

			IAnalyzable l_AnalyzableModule = null;
			if (l_Module instanceof IAnalyzable && m_ErrorAnalyzer != null) {
				l_AnalyzableModule = (IAnalyzable) l_Module;

				// Retrieve useful analysis info
				DataItem l_AnalysisInfo = l_AnalyzableModule.GetAnalysisInfoForQuestion(a_Item);
				// Perform analysis
				if (l_AnalysisInfo != null) {
					m_ErrorAnalyzer.PerformAnalysisOnQuestionAndCandidates(l_AnalysisInfo);
				}
			} else {
				
				DataItem l_RetrievedAnswer = l_StrategyModule.GetAnswerForQuestion(a_Item);
				l_RankedAnswerTODO = l_RetrievedAnswer;

				l_Answers.put(l_Module.GetModuleID(), l_RetrievedAnswer);				
				
			} // end if
			
		} // end for


		// TODO Answer ranker
		if (l_RankedAnswerTODO != null) {
			SaveAnswer(l_RankedAnswerTODO);
		}

		OneLessCallBack();

	} // end Notify()


	/**
	 * Stores the provided answer for later write to file
	 * @param a_AnswerStructure [in] the structure containing the answer string 
	 */
	private void SaveAnswer(DataItem a_AnswerStructure) {
		m_DataItem_Results.AddField("Answer", a_AnswerStructure);		
	} // end WriteAnswerToOutputFile()


	/**
	 * Write the stored answers all to file.
	 */
	private void WriteResultsToFile() {
		
		BufferedWriter l_OutputFile = null;
		try {
			l_OutputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m_OutputFileName), "UTF8"));			
		} catch (Exception e) {
			l_OutputFile = null;			
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error preparing temp file : [" + m_OutputFileName + "] ", e);
		}


		try {
			l_OutputFile.write(m_DataItem_Results.toXMLString());
		} catch (Exception e) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error writing to temp XML file : [" + m_OutputFileName + "] ", e);
		}

		// Save the temporary file -------------------------------------------------------
		if (l_OutputFile != null) {
			try {				
				l_OutputFile.flush();
				l_OutputFile.close();
				l_OutputFile = null;
			} catch (IOException e) {
				Logger.getLogger("QANUS").logp(Level.SEVERE, AnswerRetriever.class.getName(), "Go", "Error closing file : [" + m_OutputFileName + "] ", e);
			}
		} // end if
		
	} // end WriteResultsToFile()

	
	// --- Used to track the number of outstanding callbacks atomically
	private synchronized void NewCallBack() {
		l_CallbacksOutstanding++;
	} // end NewCallBack()

	private synchronized void OneLessCallBack() {
		l_CallbacksOutstanding--;
	} // end OneLessCallBack()

	private synchronized boolean AnyCallBacksLeft() {
		return (l_CallbacksOutstanding > 0);
	} // end AnyCallBacksLeft()


	
} // end class AnswerRetriever
