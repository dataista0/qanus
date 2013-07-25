package sg.edu.nus.wing.qanus.mitic.ar;



import sg.edu.nus.wing.qanus.framework.ar.FrameworkController;
import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalyzer;
import sg.edu.nus.wing.qanus.framework.commons.IRegisterableModule;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;
import sg.edu.nus.wing.qanus.mitic.ar.er.FactoidPipelineErrorAnalyzer;
import sg.edu.nus.wing.qanus.mitic.ar.featurescoring.FeatureScoringStrategy;


/**
 * mitic implementation of controller module for answer retrieval stage.
 * Makes use of annotated TREC 2007 questions and a feature-scoring based
 * retrieval technique.
 *
 * This Controller is the entry point for the Answer Retrieval stage.
 * 
 * This can be a reference implementation for your own customised answer retrieval
 * components.
 *
 * @author NG, Jun Ping - junping@comp.nus.edu.sg
 * @version 15Jan2010
 */
public class Controller extends FrameworkController {


	// For the ar.FrameworkController
	// Notes:
	/*
	 * When creating your XML handlers and modules to customise this portion, you
	 * may need this info read in from the command line:
	 * GetSourceFile1() gives the information base source. Could be null.
	 * GetSourceFile2() gives the question source.
	 * GetTargetFile() gives the folder to save the answers in
	 * GetErrorAnalysisSource() gives the folder to load error analysis reference information from
	 * GetErrorAnalysisTarget() gives the folder to store error analysis information to
	 */


	/**
	 * First customisable part of the controller.
	 * Prepares and returns the XML handler to be used by the StageEngine for this Controller.
	 * For this StageEngine -- answer retrieval --- the XML parser should be the one handling
	 * the file of annotated questions
	 * Only 1 handler is required for the engine, so the returned array only contains 1 element.
	 * @return array of XML handlers to use for annotated questions file.
	 */
	@Override
	public IXMLParser[] GetXMLHandlersForStageEngine() {
		IXMLParser[] l_Array = new IXMLParser[1];
		l_Array[0] = new TREC2007AnnotatedQuestionXMLParser();

		return l_Array;
	} // end GetXMLHandlerForStageEngine()



	/**
	 * Seconde customisable part of the controller.
	 * Prepares and returns the modules to be invoked by the StageEngine.
	 * For this StageEngine -- answer retrieval -- the modules would be the answer retrieval
	 * strategies
	 * @return array of modules to be invoked.
	 */
	@Override
	public IRegisterableModule[] GetModulesForStageEngine() {

		//BasicIRBasedStrategyUsingWebAsCorpus l_Module = new BasicIRBasedStrategyUsingWebAsCorpus();
		
		// Use the feature scoring strategy, initialised to the provided knowledge base
		FeatureScoringStrategy l_Module = new FeatureScoringStrategy(GetSourceFile1());
		
		IRegisterableModule[] l_Array = new IRegisterableModule[1];
		l_Array[0] = l_Module;

		return l_Array;

	} // end GetModulesForStageEngine()


	/**
	 * Overrides implementation in parent class to provide an error analyzer so that
	 * error analysis can be carried out
	 * @return error analysis engine
	 */	
	@Override
	public ErrorAnalyzer GetErrorAnalysisEngine() {
		if (GetErrorAnalysisSource() == null) {
			return null;
		}
		return new FactoidPipelineErrorAnalyzer(GetErrorAnalysisSource(), GetErrorAnalysisTarget());
	} // end GetErrorAnalysisEngine()



	
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
