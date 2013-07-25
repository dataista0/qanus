package sg.edu.nus.wing.qanus.framework.ibp;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.ControllerForSingleSourceAndTarget;
import sg.edu.nus.wing.qanus.framework.commons.IInformationBaseBuilder;
import sg.edu.nus.wing.qanus.framework.commons.IStageEngineController;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;



/**
 * Prepares and sets up the information base building stage for running.
 *
 * This class prepares the necessary set up for the stage.
 * To customise the entry point for this stage with desired components, extend
 * from this class.
 *
 * You should not have a need to change this class, unless you intend
 * to fundamentally change how the parts of the QANUS framework works.
 * 
 * @author Ng, Jun Ping -- junping@comp.nus.edu.sg
 * @version 25Jan2010
 */
public abstract class FrameworkController extends ControllerForSingleSourceAndTarget implements IStageEngineController {


	// Field to hold the StageEngine to use with this Controller
	private InformationBaseEngine m_StageEngine;
	
	
	public FrameworkController() {

		// The command line argument names
		super("src", "tgt");

		// The command line options expected to use with this Controller
		AddOptionWithRequiredArgument("src", "Folder containing the XML source documents", File.class);
			MakeOptionCompulsory("src");
		AddOptionWithRequiredArgument("tgt", "Folder where eventual information base will be stored", File.class);
			MakeOptionCompulsory("tgt");
		AddOptionWithRequiredArgument("mode", "Mode of operation when running, valid options include\n\tprocess (only do text processing)\n\tkbb(only do knowledge base building)\n\tboth (default, do both)\n", String.class);
			
	}

	
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
		if (l_OkSoFar && !SourceExists()) {
			// source file does not exist, we cannot continue.
			Logger.getLogger("QANUS").logp(Level.SEVERE, FrameworkController.class.getName(), "Entry", "Cannot access source folder.");
			l_OkSoFar = false;
		}
		// Verify that specified source is a folder		
		if (l_OkSoFar && !GetSourceFile().isDirectory()) {
			Logger.getLogger("QANUS").logp(Level.SEVERE, FrameworkController.class.getName(), "Entry", "Expecting a directory as the source folder.");
			l_OkSoFar = false;
		}
		
		// Create the target file/folder if it doesn't exist
		if (l_OkSoFar && !EnsureTargetExists()) {
			// No point continueing if the results produced later cannot be saved?
			Logger.getLogger("QANUS").logp(Level.SEVERE, FrameworkController.class.getName(), "Entry", "Cannot access target folder.");
			l_OkSoFar = false;
		}

		 
		// Don't proceed if the requirements for a successfull execution are not met
		if (!l_OkSoFar) return false;




		// Set up StageEngine now that we have the required information
		m_StageEngine = new InformationBaseEngine(GetSourceFile(), GetTargetFile());

		// Retrieve required components from derived class and set up this StageEngine
		IXMLParser[] l_Handlers = this.GetXMLHandlersForStageEngine();
		if (l_Handlers == null) return false;
		m_StageEngine.SetXMLHandler(l_Handlers[0]);		
		m_StageEngine.RegisterModules(this.GetModulesForStageEngine());
		m_StageEngine.SetKnowledgeBaseBuilder(this.GetInformationBaseBuilder());

		// Start StageEngine
		Logger.getLogger("QANUS").logp(Level.FINE, FrameworkController.class.getName(), "Entry", "Starting StageEngine...");
		return m_StageEngine.Go();


	} // end Entry()


	/**
	 * Customisable for information base preparation stage.
	 * The information base builder to use to create the information base
	 *
	 * @return a information base builder which will be invoked to build the information base.
	 */
	public abstract IInformationBaseBuilder GetInformationBaseBuilder();

		
} // end class FrameworkController
