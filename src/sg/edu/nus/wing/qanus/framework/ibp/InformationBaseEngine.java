package sg.edu.nus.wing.qanus.framework.ibp;


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
import sg.edu.nus.wing.qanus.framework.commons.IInformationBaseBuilder;
import sg.edu.nus.wing.qanus.framework.commons.IXMLDataReceipient;
import sg.edu.nus.wing.qanus.framework.commons.StageEngine;
import sg.edu.nus.wing.qanus.framework.util.DirectoryAndFileManipulation;

/**
 * StageEngine for information base building.
 * 
 * 1. Reads in text from the provided corpus
 * 2. Process them with the registered text processing modules
 * 3. Sends the annotated/processed text to the inforamtion base builder class
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Jan 17, 2010
 */
public class InformationBaseEngine extends StageEngine implements IXMLDataReceipient {


	// Use to build the required knowledge base
	private IInformationBaseBuilder m_Builder = null;

	// Houses a temporary folder
	private File m_TempFolder;
	

	/**
	 * Constructor
	 * @param a_CorpusSource [in] folder where corpus documents is found
	 * @param a_Target [in] folder used to hold output files from this stage
	 */
	public InformationBaseEngine(File a_CorpusSource, File a_Target)
  {
		
		super();

		SetSourceFile(a_CorpusSource);
		SetTargetFile(a_Target);

		// Set up a temporary directory to contain annotated text when processing the corpus
		m_TempFolder = new File("choppingboard" + File.separator + "temp" + File.separator + "knp");
		
    if (!DirectoryAndFileManipulation.CreateDirectoryIfNonExistent(m_TempFolder)) 
    {
			Logger.getLogger("QANUS").log(Level.WARNING, InformationBaseEngine.class.getName(), "Failed to create temporary folder [" + m_TempFolder + "]");
		}

	} // end constructor
	


	/**
	 * Reads in XML files from the intended corpus and build a KB from them.
	 * The text from the XML is processed by the various registered text processing
	 * modules, and sent to the InformationBaseBuilder
	 *
	 * The annotations done to the XML text by the text processing modules
	 * are stored in a temporary directory in case they are needed in future.
	 *
	 * The XML files in the folder are expected to be stored in a "flat" hierarchy, ie. no
	 * sub-directories. If there are sub-directories within the folder, they are ignored.
	 *	 
	 */
	@Override
	public boolean Go() 
  {
		

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
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseEngine.class.getName(), "Go", "Error setting up parser", e);
		} catch (SAXException e) {			
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseEngine.class.getName(), "Go", "Error setting up parser", e);
		}



		// The source folder could contain main nested directories,
		// we will collect all the file names in these directories first
		// so that it is easier to process them subsequently.
		// We collect the file names and not the files to save on memory requirements.
		// We will instantiate the java.io.File object as and when needed instead.
		LinkedList<String> l_ListOfFileNames = DirectoryAndFileManipulation.CollectFileNamesInDirectory(GetSourceFile());


		// Process each file, sending it into the XML parser. -----------------
		for (String l_FileNameToParse : l_ListOfFileNames) {
      System.out.println("ARCHIVO: "+l_FileNameToParse);
			String l_RelativeFileName = l_FileNameToParse.substring(l_FileNameToParse.lastIndexOf(File.separator)+1) ;
			Logger.getLogger("QANUS").logp(Level.FINE, InformationBaseEngine.class.getName(), "Go", "Processing [" + l_RelativeFileName + "]");

			// Build a temporary file to store the processed entries from this file	-----------------
			// TODO output a proper DTD file and add <DOCSTREAM> tag to this xml file?
			String l_TempFileName = m_TempFolder.getAbsolutePath() + File.separator +  l_RelativeFileName + ".processed";
			BufferedWriter l_IntermediateBW = null;
			try {
				l_IntermediateBW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(l_TempFileName), "UTF8"));
				l_IntermediateBW.write("<DOCSTREAM>");
			} catch (Exception e) {
				l_IntermediateBW = null;
				Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseEngine.class.getName(), "Go", "Error preparing temp file [" + l_TempFileName + "]", e);
			}



			// Start parsing -------------------------------------------------------
			File l_FileToParse = new File(l_FileNameToParse);
			try {
				// Within this call to parse() where the call-backs from
				// AQUAINT2XMLHandler and AQUAINTTextProcessor will be invoked.
				// TODO sloppy work to just cast as DefaultHandler
				// can we marry the inheritance from DefaultHandler and the interface
				// IXMLParser better?
        //JP: Aca outputea el Getting data from....
				l_DocBuilder.parse(l_FileToParse, (DefaultHandler) GetXMLHandler());
        
			} catch (SAXException e) {				
				Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseEngine.class.getName(), "Go", "Error processing [" + l_FileNameToParse + "]", e);
			} catch (IOException e) {				
				Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseEngine.class.getName(), "Go", "Error processing [" + l_FileNameToParse + "]", e);
			}


			// Save the temporary file -------------------------------------------------------
			if (l_IntermediateBW != null) {
				try {
					l_IntermediateBW.write("</DOCSTREAM>");
					l_IntermediateBW.close();
					l_IntermediateBW = null;
				} catch (IOException e) {					
					Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseEngine.class.getName(), "Go", "Error saving file [" + l_IntermediateBW + "]", e);
				}
			} // end if


		} // end for

		
		// Stop receiving notifications about new data items
		GetXMLHandler().DelistFromNotification(this);


		return true;

	} // end Go()


	public String GetIdentifier() {
		return "InformationBaseEngine";
	}


	/**
	 * Callback that is invoked by the XML handler working on parsing the input document.
	 * @param a_Item
	 */
	public void Notify(DataItem a_Item) {

    m_Builder.AddToInfoBase(a_Item);
    /* 		
		// Convert the sentences in the data item to an array of strings for the text modules
		// This is the corpus specific part
		// Each field value is a <TEXT> XML instance
		DataItem[] l_FieldValues = a_Item.GetFieldValues("ART");
    
		if (l_FieldValues == null || l_FieldValues.length == 0) return;  // Nothing to do
		String[] l_Sentences = ExtractSentencesIntoArray(l_FieldValues);
		if (l_Sentences == null || l_Sentences.length == 0) return;


		// Notify each registered text module
		for (IRegisterableModule l_Module : m_ModuleList) {

			ITextProcessingModule l_TPModule = null;
			if (l_Module instanceof ITextProcessingModule) {
				l_TPModule = (ITextProcessingModule) l_Module;
			} else {
				Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseEngine.class.getName(), "Notify", "Wrong text processing module registered.");
				continue;
			}


			String[] l_Annotated = l_TPModule.ProcessText(l_Sentences);
			if (l_Annotated == null || l_Annotated.length == 0) continue;

			// Output the annotated sentences back into the data item marked with a XML tag
			for (String l_AnnotatedSentence : l_Annotated) {
				a_Item.AddSubFieldValue("TEXT-ANNOTATED-"+l_Module.GetModuleID(), "P", l_AnnotatedSentence);
			}

		} // end for
				

		// Check that Lucene is properly initialized
		if (m_Builder == null) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseEngine.class.getName(), "Notify", "Lucene not initialized.");
			return;
		}

		// Add the processed text to the knowledge base
		Logger.getLogger("QANUS").logp(Level.FINE, InformationBaseEngine.class.getName(), "Notify", "Invoking Knowledge Base Creator...");
		m_Builder.AddToInfoBase(a_Item);
		
		*/
	} // end Notify()
	

	/**
	 * Given a data item for the <TEXT> XML instance, extract the different <P> sentences within it
	 * into a string array.
	 *
	 * @param a_FieldValues [in] list of <TEXT> XML instance data items.
	 * @return array of strings of each <P> sentence within the <TEXT> tags.
	 */
	private String[] ExtractSentencesIntoArray(DataItem[] a_FieldValues) {

		LinkedList<String> l_SentencesList = new LinkedList<String>();

		// Each data item is a TEXT XML instance.
		// Normally there should only be one, but we'd loop through anyway, just in case
		for (DataItem l_Item : a_FieldValues) {

			DataItem[] l_SentenceValues =  l_Item.GetFieldValues("P");
			if (l_SentenceValues == null || l_SentenceValues.length == 0) continue;  // Nothing to do

			for (DataItem l_SentenceValue : l_SentenceValues) {

				String[] l_SentenceArray = l_SentenceValue.GetValue();
				if (l_SentenceArray == null || l_SentenceArray.length == 0) continue;

				for (String l_Sentence : l_SentenceArray) {
					l_SentencesList.add(l_Sentence);
				} // end for l_Sentence...

			} // end for l_SentenceValue...

		} // end for l_Item...


		if (l_SentencesList.size() == 0)
			return new String[0];
		else
			return l_SentencesList.toArray(new String[0]);

	} // end ExtractSentencesIntoArray()

	

	/**
	 * This StageEngine requires a knowledge base builder.
	 * This function determines the knowledge base builder to be used.
	 *
	 * @param a_Builder [in] the knowledge base builder to use with this StageEngine
	 */
	public void SetKnowledgeBaseBuilder(IInformationBaseBuilder a_Builder) {
		m_Builder = a_Builder;
	} // end SetKnowledgeBaseBuilder()


} // end class InformationBaseEngine
