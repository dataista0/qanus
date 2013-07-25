package sg.edu.nus.wing.qanus.framework.eval;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.ControllerForDoubleSourceAndTarget;
import sg.edu.nus.wing.qanus.framework.commons.IStageEngineController;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;


/**
 * Prepares and sets up the evaluation stage for running.
 *
 * This class prepares the necessary set up for the stage.
 * To customise the entry point for the answer retrieval stage, extend
 * from this class.
 *
 * You should not have a need to change this class, unless you intend
 * to fundamentally change how the parts of the QANUS framework works.
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 16Jan2010
 */
public abstract class FrameworkController extends ControllerForDoubleSourceAndTarget implements IStageEngineController {


	// Field pointing to the StageEngine coupled with this Controller
	private AnswerChecker m_StageEngine = null;


	/**
	 * Constructor
	 */
	public FrameworkController() {

		// The command line argument names
		super("gen", "correct", "ans");

		// The command line options expected to use with this Controller
		AddOptionWithRequiredArgument("gen", "File name of generated answers", File.class);
			MakeOptionCompulsory("gen");
		AddOptionWithRequiredArgument("correct", "File name of correct answers", File.class);
			MakeOptionCompulsory("correct");
		AddOptionWithRequiredArgument("ans", "Folder name to store evaluation results", File.class);
			MakeOptionCompulsory("ans");

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
		if (!Source1Exists()) {
			// source file does not exist, we cannot continue.
			Logger.getLogger("QANUS").log(Level.SEVERE, FrameworkController.class.getName(), "Cannot access generated answers.");
			l_OkSoFar = false;
		}
		if (!Source2Exists()) {
			// source file does not exist, we cannot continue.
			Logger.getLogger("QANUS").log(Level.SEVERE, FrameworkController.class.getName(), "Cannot access gold-standard answers.");
			l_OkSoFar = false;
		}
		// Create the target file/folder if it doesn't exist
		if (!EnsureTargetExists()) {
			// No point continueing if the results produced later cannot be saved?
			Logger.getLogger("QANUS").log(Level.SEVERE, FrameworkController.class.getName(), "Cannot create file to store evaluation results in.");
			l_OkSoFar = false;
		}


		// Don't proceed if the requirements for a successfull execution are not met
		if (!l_OkSoFar) return false;




		// Set up StageEngine now that we have the required information
		m_StageEngine = new AnswerChecker(GetSourceFile2(), GetSourceFile1(), GetTargetFile());

		// Retrieve required components from derived class and set up this StageEngine
		IXMLParser[] l_Handlers = this.GetXMLHandlersForStageEngine();
		if (l_Handlers == null || l_Handlers.length != 2) return false;
		m_StageEngine.SetGeneratedAnswersXMLHandler(l_Handlers[0]);
		m_StageEngine.SetCorrectAnswersXMLHandler(l_Handlers[1]);
		m_StageEngine.RegisterModules(this.GetModulesForStageEngine());


		// Start StageEngine
		return m_StageEngine.Go();

	
	} // end main()

	

} // end class FrameworkController
