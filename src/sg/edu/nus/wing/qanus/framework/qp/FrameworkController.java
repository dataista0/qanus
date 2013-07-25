package sg.edu.nus.wing.qanus.framework.qp;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.ControllerForSingleSourceAndTarget;
import sg.edu.nus.wing.qanus.framework.commons.IStageEngineController;



/**
 * Prepares and sets up the question processing stage for running.
 *
 * This class prepares the necessary set up for the stage.
 * To customise the entry point for the question processing stage, extend
 * from this class.
 *
 * You should not have a need to change this class, unless you intend
 * to fundamentally change how the parts of the QANUS framework works.
 * 
 * @author Ng, Jun Ping -- junping@comp.nus.edu.sg
 * @version 16Jan1020
 */
public abstract class FrameworkController extends ControllerForSingleSourceAndTarget implements IStageEngineController {


	// Field pointing to the StageEngine coupled with this Controller
	private QuestionProcessor m_StageEngine = null;


	/**
	 * Constructor
	 */
	public FrameworkController() {

		// The command line argument names
		super("src", "tgt");

		// The command line options expected to use with this Controller
		AddOptionWithRequiredArgument("src", "Folder/File name of XML files of questions", File.class);
			MakeOptionCompulsory("src");
		AddOptionWithRequiredArgument("tgt", "Output folder name to store processed questions", File.class);
			MakeOptionCompulsory("tgt");		

	} // end constructor


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
		if (!SourceExists()) {
			// source file does not exist, we cannot continue.
			Logger.getLogger("QANUS").log(Level.SEVERE, FrameworkController.class.getName(), "Cannot access folder/file of questions.");
			l_OkSoFar = false;
		}
		// Create the target file/folder if it doesn't exist
		if (!EnsureTargetExists()) {
			// No point continueing if the results produced later cannot be saved?
			Logger.getLogger("QANUS").log(Level.SEVERE, FrameworkController.class.getName(), "Cannot create file to store results in.");
			l_OkSoFar = false;
		}
		

		// Don't proceed if the requirements for a successfull execution are not met
		if (!l_OkSoFar) return false;



		// Set up StageEngine now that we have the required information
		m_StageEngine = new QuestionProcessor(GetSourceFile(), GetTargetFile());
		
		// Retrieve required components from derived class and set up this StageEngine
		m_StageEngine.SetXMLHandler(this.GetXMLHandlersForStageEngine()[0]);
		m_StageEngine.RegisterModules(this.GetModulesForStageEngine());

		// Start StageEngine
		return m_StageEngine.Go();


	} // end Entry()



} // end class FrameworkController


