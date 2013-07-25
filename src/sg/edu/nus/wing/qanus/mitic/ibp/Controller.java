package sg.edu.nus.wing.qanus.mitic.ibp;

import java.io.File;

import sg.edu.nus.wing.qanus.framework.commons.IInformationBaseBuilder;
import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;
import sg.edu.nus.wing.qanus.framework.ibp.FrameworkController;
import sg.edu.nus.wing.qanus.textprocessing.StanfordNER;
import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;


/**
 * mitic implementation of the Controller of the  information base processing stage.
 * 
 * Basically we take in text from a corpus, and index it with an engine like Lucene.
 *
 * This Controller is the entry point for the information base preparation stage.
 *
 * This can be a reference implementation for your own customised evaluation
 * components.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Jan 16, 2010
 */
public class Controller extends FrameworkController {

	/**
	 * First customisable part of the controller.
	 * Prepares and returns the XML handler to be used by the StageEngine for this Controller.
	 * For this StageEngine -- knowledge base building --- a XML parser is needed to process
	 * the source corpus documents
	 *
	 * @return array of XML handlers to use for the StageEngine
	 */
	@Override
	public IXMLParser[] GetXMLHandlersForStageEngine() {

		IXMLParser[] l_Array = new IXMLParser[1];
		l_Array[0] = new AQUAINT2XMLHandler();		

		return l_Array;

	} // end GetXMLHandlerForStageEngine()



	/**
	 * Second customisable part of the controller.
	 * Prepares and returns the modules to be invoked by the StageEngine.
	 * For this StageEngine -- knowledge base building -- the modules would be
	 * the text processors such as named entity recognisers which can be
	 * used to pre-process the corpus text
	 *
	 * @return array of modules to be invoked.
	 */
	@Override
	public IRegisterableModule[] GetModulesForStageEngine() {

		IRegisterableModule[] l_Array = new IRegisterableModule[2];
		
		l_Array[0] = new StanfordPOSTagger("lib" + File.separator + "bidirectional-wsj-0-18.tagger");		  
    l_Array[1] = new StanfordNER("lib" + File.separator + "ner-eng-ie.crf-4-conll-distsim.ser.gz");
		//l_Array[1] = new StanfordNERWebService(); // "lib" + File.separator + "ner-eng-ie.crf-4-conll-distsim.ser.gz"

		return l_Array;

	} // end GetModulesForStageEngine()


	/**
	 * Customisable for knowledge base preparation stage.
	 * Prepares and sets up a Lucene index to be created.
	 *
	 * @return a knowledge base builder using Lucene
	 */
	@Override
	public IInformationBaseBuilder GetInformationBaseBuilder() {
		
		InformationBaseWithLucene l_KBB = new InformationBaseWithLucene(GetTargetFile());
		return l_KBB;

	} // end GetInformationBaseBuilder()


	/**
	 * Entry point function.
	 * You don't have to change this usually, so you can just copy this to make your own
	 * Controller.
	 *
	 * @param args
	 */
	public static void main(String args[]) {

		//System.out.println("Ehehehe viejaaa eeeh1hh");
    // --------------------------------------------------------
		// Start up an instance of the controller, and invoke it to get the machinery going
		// This illustrates the standard way this can be done. When creating your own
		// Controller, or customising it for your needs, you can take this as a reference
		// implementation.
		Controller l_Ctr = new Controller();
    
		// If a log is desired, uncomment this. When this is commented, log messages
		// are not saved to any file
		l_Ctr.SetUpLog();
  

		// This call will jump-start the machinery. Must be called, else nothing will happen!
		if (!l_Ctr.Entry(args)) {
			// An error happened
			// Fix it if you want
		} else {
			// Successfull invoked engine.
			// Any post-processing?
        System.out.println("FIN IBP-Mitic");
		}


		// --------------------


	} // end main()


	
	
} // end class
