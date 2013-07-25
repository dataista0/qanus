package sg.edu.nus.wing.qanus.framework.commons;


import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import joptsimple.OptionParser;
import joptsimple.OptionSet;



/**
 * Entry point for a typical control class in QANUS.
 * Used to kick start components like the information-base builder or
 * query processor.
 *
 * Provides common functionality for entry point modules, in particular
 * command line argument processing.
 *
 * @author Ng, Jun Ping -- ngjp@nus.edu.sg
 * @version 04Dec2009
 *
 */
public abstract class BasicController {

	
	// Command line arguments - Handle using JOpt Simple
	private OptionParser m_OptionParser;
	private OptionSet m_CmdLineArguments;

	private LinkedList<String> m_CompulsoryOptions;
	
	/**
	 * Constructor.
	 */
	protected BasicController() {

		m_OptionParser = new OptionParser();
		m_CmdLineArguments = null;

		m_CompulsoryOptions = new LinkedList<String>();

	} // end constructor


	/**
	 * Should be called after entry point main() of derived classes
	 * Will process incoming arguments, and other required entry tasks.
	 * Checks if compulsory arguments are provided amongst other things.
	 * 
	 * @param args
	 * @return true on successful execution, false on any errors.
	 */
	protected boolean Entry(String[] args) {
	
		if (!ProcessOptions(args)) { ShowUsage(); return false; }

		return true;

	} // end Entry()


	/**
	 * Process the incoming command line arguments.
	 * 
	 * @param args [in] the arguments
	 */
	private boolean ProcessOptions(String[] args) {

		m_CmdLineArguments = m_OptionParser.parse(args);

		// Check for compulsory options
		for (String l_Option : m_CompulsoryOptions) {
			if (!m_CmdLineArguments.has(l_Option)) {
				return false;
			}
		}

		return true;

	} // end ProcessOptions()



	/**
	 * Retrieves the argument associated with a particular option.
	 *
	 * @param a_OptionName [in] the option whose value we want to retrieve.
	 * @return argument associated with the option, or null if option is not found.
	 */
	protected Object GetOptionArgument(String a_OptionName) {

		if (m_CmdLineArguments.has(a_OptionName)) {
			return m_CmdLineArguments.valueOf(a_OptionName);
		} else {
			return null;
		} // end if-else

	} // end GetOptionArgument()



	/**
	 * Adds a option with compulsory arguments.
	 * This option MUST be present in the command line arguments.
	 *
	 * @param a_OptionKey [in] supported option key, without preceding '-'
	 */
	protected void AddOptionWithRequiredArgument(String a_OptionKey, String a_Description, Class a_Type) {
		m_OptionParser.accepts(a_OptionKey, a_Description).withRequiredArg().ofType(a_Type);
	} // end AddSupportedOption()


	/**
	 * Specify that an option must be included in the command line each time
	 * @param a_OptionKey [in] the option that must always be present
	 */
	protected void MakeOptionCompulsory(String a_OptionKey) {
		m_CompulsoryOptions.add(a_OptionKey);
	} // end MakeOptionCompulsory()



	/**
	 * Sets up the file required for the Java logging api to work.
	 * If this is not executed, the Logger.log statements will be black-holed.
	 */
	protected void SetUpLog() {


		// Start logger
		FileHandler l_FH;
		try {
			l_FH = new FileHandler("QANUS.log", true);
			Logger l_Log = Logger.getLogger("QANUS");
			l_Log.addHandler(l_FH);
			l_Log.setLevel(Level.ALL);
			SimpleFormatter l_Fmt = new SimpleFormatter();
			l_FH.setFormatter(l_Fmt);
		} catch (IOException ex) {
			Logger.getLogger("QANUS").log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			Logger.getLogger("QANUS").log(Level.SEVERE, null, ex);
		}

	} // end SetUpLog()


	/**
	 * Displays the correct usage for this program.
	 */
	public void ShowUsage()  {
		try {
			m_OptionParser.printHelpOn(System.out);
		} catch (java.io.IOException ex) {
			Logger.getLogger(BasicController.class.getName()).log(Level.SEVERE, "Unable to show usage information", ex);
		}
	} // end ShowUsage()




} // end class

