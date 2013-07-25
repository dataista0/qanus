package sg.edu.nus.wing.qanus.stock.qp;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;
import sg.edu.nus.wing.qanus.framework.qp.FrameworkController;
import sg.edu.nus.wing.qanus.textprocessing.QuestionClassifierWithStanfordClassifier;
import sg.edu.nus.wing.qanus.textprocessing.StanfordNER;
import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;

/**
 * Stock implementation of the Controller ofg the question processing stage.
 * 
 * Takes in a set of TREC 2007 questions, and annotates them with a variety of
 * text processors, such as named entity recognisers and POS taggers.
 *
 * This Controller is the entry point for the question processing stage.
 *
 * This can be a reference implementation for your own customised evaluation
 * components.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Jan 16, 2010
 */
public class Controller extends FrameworkController {


	// For the qp.FrameworkController
	// Notes:
	/*
	 * When creating your XML handlers and modules to customise this portion, you
	 * may need this info read in from the command line:
	 * GetSourceFile() gives the folder/file where questions to be processed are found
	 * GetTargetFile() gives the folder to save the processed questions in
	 */


	/**
	 * First customisable part of the controller.
	 * Prepares and returns the XML handler to be used by the StageEngine for this Controller.
	 * For this StageEngine -- question processing -- the xml parser for the input questions
	 * file is needed	 
	 *
	 * @return array of XML handlers to use for the StageEngine
	 */
	@Override
	public IXMLParser[] GetXMLHandlersForStageEngine() {

		IXMLParser[] l_Array = new IXMLParser[1];

		l_Array[0] = new TRECQuestionXMLHandler();

		return l_Array;

	} // end GetXMLHandlerForStageEngine()



	/**
	 * Seconde customisable part of the controller.
	 * Prepares and returns the modules to be invoked by the StageEngine.
	 * For this StageEngine -- question processing -- the modules would be the evaluation
	 * metrics to score the generated answers with
	 *
	 * @return array of modules to be invoked, or null on errors
	 */
	@Override
	public IRegisterableModule[] GetModulesForStageEngine() {

		IRegisterableModule[] l_Array = new IRegisterableModule[3];
		try {

			IRegisterableModule l_ModQuestionClassifier = null;
			if (File.separator.compareTo("\\") == 0) {
				// Special case for Windows as the Stanford classifier will treat the single \ as an escape character
				l_ModQuestionClassifier = new QuestionClassifierWithStanfordClassifier("lib\\\\trec_classifier.stanford-classifier", "choppingboard" + File.separator + "temp");
			} else {
				l_ModQuestionClassifier = new QuestionClassifierWithStanfordClassifier("lib" + File.separator + "trec_classifier.stanford-classifier", "choppingboard" + File.separator + "temp");
			}
			l_Array[0] = l_ModQuestionClassifier;


			StanfordNER l_ModNER = new StanfordNER("lib" + File.separator + "ner-eng-ie.crf-4-conll-distsim.ser.gz");
			//IRegisterableModule l_ModNER = new StanfordNERWebService();
			l_Array[1] = l_ModNER;

			IRegisterableModule l_ModPOSTagger = new StanfordPOSTagger("lib" + File.separator + "bidirectional-wsj-0-18.tagger");
			l_Array[2] = l_ModPOSTagger;

		} catch (Exception ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, Controller.class.getName(), "GetModulesForStageEngine", "Unable to initialise text processing module: [" + ex + "]");
			return null;
		}
				
		return l_Array;

	} // end GetModulesForStageEngine()


	/**
	 * Entry point function.
	 * You don't have to change this usually, so you can just copy this to make your own
	 * Controller.
	 *
	 * @param args
	 */
	public static void main(String args[]) {

		// --------------------------------------------------------
		// Start up an instance of the controller, and invoke it to get the machinery going
		// This illustrates the standard way this can be done. When creating your own
		// Controller, or customising it for your needs, you can take this as a reference
		// implementation.
		Controller l_Ctr = new Controller();

		// If a log is desired, uncomment this. When this is commented, log messages
		// are not saved to any file
		//l_Ctr.SetUpLog();


		// This call will jump-start the machinery. Must be called, else nothing will happen!
		if (!l_Ctr.Entry(args)) {
			// An error happened
			// Fix it if you want
		} else {
			// Successfull invoked engine.
			// Any post-processing?
		}


		// --------------------


	} // end main()


} // end class Controller

