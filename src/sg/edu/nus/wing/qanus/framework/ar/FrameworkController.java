package sg.edu.nus.wing.qanus.framework.ar;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalyzer;
import sg.edu.nus.wing.qanus.framework.commons.ControllerForDoubleSourceAndTarget;
import sg.edu.nus.wing.qanus.framework.commons.IAnalyzableController;
import sg.edu.nus.wing.qanus.framework.commons.IStageEngineController;



/**
 * Prepares and sets up the answer retrieval stage for running.
 *
 * This class prepares the necessary set up for the stage.
 * To customise the entry point for the answer retrieval stage, extend
 * from this class.
 *
 * Error analysis can also be performed by overriding the functions specified by the
 * IAnalyzableController interface.
 *
 * You should not have a need to change this class, unless you intend
 * to fundamentally change how the parts of the QANUS framework works.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 15Jan2010
 */
public abstract class FrameworkController extends ControllerForDoubleSourceAndTarget implements IStageEngineController, IAnalyzableController {


	// Field pointing to the StageEngine coupled with this Controller
	private AnswerRetriever m_StageEngine = null;


	// Extra options if error analysis will be carried out
	private final String m_ErrorAnalysisSourceLabel = "errsrc";
	private final String m_ErrorAnalysisTargetLabel = "errtgt";


	/**
	 * Sets up the controller needed for answer retrieval.
	 */
	public FrameworkController() {

		// The command line argument names
		super("kbsrc", "qnsrc", "anstgt");

		// The command line options expected to use with this Controller
		AddOptionWithRequiredArgument("kbsrc", "Folder name where knowledge base is found", File.class);
			// kbsrc may be optional
		AddOptionWithRequiredArgument("qnsrc", "Folder/File name of posed questions", File.class);
			MakeOptionCompulsory("qnsrc");
		AddOptionWithRequiredArgument("anstgt", "Folder name to store generated answers", File.class);
			MakeOptionCompulsory("anstgt");
		AddOptionWithRequiredArgument(m_ErrorAnalysisSourceLabel, "Folder name to load error analysis reference information from", File.class);
		AddOptionWithRequiredArgument(m_ErrorAnalysisTargetLabel, "Folder name to store error analysis results to", File.class);

	} // end constructor


	/**
	 * Retrieves the argument passed in to load error analysis reference information from.
	 *
	 * This is implemented here because this is an extra argument just for the AR stage
	 * @return file, or null if not provided
	 */
	protected File GetErrorAnalysisSource() {
		return (File) GetOptionArgument(m_ErrorAnalysisSourceLabel);
	} // end GetErrorAnalysisSource()


	/**
	 * Retrieves the argument passed in for the folder name to store error analysis results to.
	 *
	 * This is implemented here because this is an extra argument just for the AR stage
	 * @return file, or null if not provided
	 */
	protected File GetErrorAnalysisTarget() {
		return (File) GetOptionArgument(m_ErrorAnalysisTargetLabel);
	} // end GetErrorAnalysisTarget()


	/**
	 * Override this method if error analysis is to be carried out.
	 * Currently this implementation returns null --> no error analysis engine to employ
	 * @return null
	 */
	public ErrorAnalyzer GetErrorAnalysisEngine() {
		return null;
	} // end GetErrorAnalysisEngine()
	
	
	/**
	 * This overrides the implementation BasicController.
	 * We want to use this chance to check if the required folders are present, as well
	 * as kick start the whole machinery
	 *
	 * @param args
	 * @return true on successful execution, false on any errors.
	 */
	@Override
	protected boolean Entry(String[] args) {

		// Check that the arguments are supplied correctly.
		boolean l_OkSoFar = super.Entry(args);

		// Ensure that the source file/folder exists
		if (!Source1Exists()) {
			// source file does not exist, we cannot continue.
			Logger.getLogger("QANUS").logp(Level.SEVERE, FrameworkController.class.getName(), "Entry", "Cannot access folder of the knowledge base.");
			l_OkSoFar = false;
		}
		if (!Source2Exists()) {
			// source file does not exist, we cannot continue.
			Logger.getLogger("QANUS").logp(Level.SEVERE, FrameworkController.class.getName(), "Entry", "Cannot access folder/file where the questions are found.");
			l_OkSoFar = false;
		}
		// Create the target file/folder if it doesn't exist
		if (!EnsureTargetExists()) {
			// No point continueing if the results produced later cannot be saved?
			Logger.getLogger("QANUS").logp(Level.SEVERE, FrameworkController.class.getName(), "Entry", "Cannot create file to store generated answers in.");
			l_OkSoFar = false;
		}

		// Don't proceed if the requirements for a successfull execution are not met
		if (!l_OkSoFar) return false;


		
		// Set up StageEngine now that we have the required information
		m_StageEngine = new AnswerRetriever(GetSourceFile2(), GetTargetFile());
		// Retrieve required components from derived class and set up this StageEngine
		m_StageEngine.SetXMLHandler(this.GetXMLHandlersForStageEngine()[0]);
		m_StageEngine.RegisterModules(this.GetModulesForStageEngine());

		// If there is error analysis to be done
		if (this.GetErrorAnalysisEngine() != null) {
			m_StageEngine.SetErrorAnalysisEngine(this.GetErrorAnalysisEngine());
		}

		// Start StageEngine
		return m_StageEngine.Go();
		

	} // end Entry()


	
 } // end FrameworkController
