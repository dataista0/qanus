package sg.edu.nus.wing.qanus.framework.qp;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;
import sg.edu.nus.wing.qanus.framework.commons.IXMLDataReceipient;
import sg.edu.nus.wing.qanus.framework.commons.StageEngine;
import sg.edu.nus.wing.qanus.framework.util.DirectoryAndFileManipulation;


/**
 * StageEngine for the question processor stage.
 * Takes in a set of questions as input. Registered modules are invoked to process
 * these input questions, and the resulting output annotations are saved to a file.
 * 
 * @author Ng, Jun Ping -- junping@comp.nus.edu.sg
 * @version 16Jan2010
 */
public class QuestionProcessor extends StageEngine implements IXMLDataReceipient  {

	
		
	// Used to point to the file we write our annotated XML to. 
	protected BufferedWriter m_CurrentOutputFile = null;
	
	
	/**
	 * Constructor
	 */
	public QuestionProcessor(File a_QuestionSource, File a_Target) {

		super();

		SetSourceFile(a_QuestionSource);
		SetTargetFile(a_Target);

	} // end constructor

	
	/**
	 * Starts the question analysis process.
	 * 
	 * 
	 */
	@Override
	public boolean Go() {


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
      
      
		} catch (ParserConfigurationException e) {	Logger.getLogger("QANUS").logp(Level.SEVERE, QuestionProcessor.class.getName(), "Go", "Error setting up parser.", e);} catch (SAXException e) {									Logger.getLogger("QANUS").logp(Level.SEVERE, QuestionProcessor.class.getName(), "Go", "Error setting up parser.", e);}
		

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
		for (String l_FileNameToParse : l_ListOfFileNames) 
    {
			
			String l_RelativeFileName = l_FileNameToParse.substring(l_FileNameToParse.lastIndexOf(File.separatorChar)) ;
			
									
			// Build a temporary file to store the processed entries from this file	-----------------
			// TODO output a proper DTD file and add <DOCSTREAM> tag to this xml file?
			// GetTargetFile() could either be a directory or file
			String l_TempFileName = null;			
			l_TempFileName = GetTargetFile().getAbsolutePath() + File.separator +  l_RelativeFileName + ".processed";			
			try {				
				m_CurrentOutputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(l_TempFileName), "UTF8"));
				m_CurrentOutputFile.write("<DOCSTREAM>");
			} catch (Exception e) {
				m_CurrentOutputFile = null;								
				Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "Go", "Error preparing temp file : [" + l_TempFileName + "]", e);
			}
			
			
			
			// Start parsing -------------------------------------------------------
			File l_FileToParse = new File(l_FileNameToParse);			
			try {
				// TODO sloppy work to just cast as DefaultHandler
				// can we marry the inheritance from DefaultHandler and the interface
				// IXMLParser better?
				l_DocBuilder.parse(l_FileToParse, (DefaultHandler) GetXMLHandler());
			} catch (SAXException e) {											
				Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "Go", "Error processing [" + l_FileNameToParse + "]", e);
			} catch (IOException e) {								
				Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "Go", "Error processing [" + l_FileNameToParse + "]", e);
			}
			
			
			// Save the temporary file -------------------------------------------------------
			if (m_CurrentOutputFile != null) {
				try {
					m_CurrentOutputFile.write("</DOCSTREAM>");
					m_CurrentOutputFile.close();
					m_CurrentOutputFile = null;
				} catch (IOException e) {														
					Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "Go", "Error saving output file.", e);
				}
			} // end if
			
			
		} // end for
		
		
		
		// Stop receiving notifications about new data items
		GetXMLHandler().DelistFromNotification(this);
		
		
		return true;
		
	} // end Go()


	/**
	 * Given a data item, we write its XML equivalent into a target file.
	 * The output file m_CurrentOutputFile should be initialized already before calling this method
	 *
	 * // TODO weird way to structure this method, place it somewhere else?
	 *
	 * @param a_Item [in] the item to write to file
	 */
	protected void OutputDataItemToFile(DataItem a_Item) {

		try {
			m_CurrentOutputFile.write(a_Item.toXMLString());
			m_CurrentOutputFile.flush();
		} catch (Exception e) {
			Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "OutputDataItemToFile", "Error writing to temp XML file.", e);
		}

	} // end OutputDataItemToFile()


	public String GetIdentifier() {
		return "QuestionProcessor";
	}


	public void Notify(DataItem a_Item) {

		// Error check
		if (a_Item == null) {
			Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "Notify", "Unexpected null data item.");
			return;
		}


		// Notify each registered text module
		for (IRegisterableModule l_Module : m_ModuleList) {


      //Ejecuta en orden los modulos cargados  en )
			ITextProcessingModule l_TPModule = null;
			if (l_Module instanceof ITextProcessingModule) {
				l_TPModule = (ITextProcessingModule) l_Module;
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, QuestionProcessor.class.getName(), "Notify", "Wrong text processing module.");
				continue;
			}

			// Retrieve the <Q> data item
			DataItem[] l_DataItem_Qs = a_Item.GetFieldValues("q");
			if (l_DataItem_Qs == null) continue;

			for (DataItem l_DataItem_Q : l_DataItem_Qs) {

				// Get the question from the <Q> item and pass it to the module
				String[] l_Annotated = l_TPModule.ProcessText(l_DataItem_Q.GetValue());
				if (l_Annotated == null || l_Annotated.length == 0) continue;

				// Output the annotated result back into the data item marked with a XML tag
				DataItem l_NewAnnotatedField = new DataItem("Q-" + l_Module.GetModuleID());
				l_NewAnnotatedField.AddAttribute("id", l_DataItem_Q.GetAttribute("id"));
				for (String l_AnnotatedSentence : l_Annotated) {
					l_NewAnnotatedField.AddValue(l_AnnotatedSentence);
				}
				a_Item.AddField("Q-" + l_Module.GetModuleID(), l_NewAnnotatedField);
			}
		} // end for


		// Save to output file
		OutputDataItemToFile(a_Item);


	} // end Notify()
	
	
	
} // end class QuestionProcessor
