package sg.edu.nus.wing.qanus.framework.commons;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.util.DirectoryAndFileManipulation;



/**
 * Specialised class of BasicController which deals with the scenario where
 * we only want to make use of 2 source folder/file, and 1 target folder.
 *
 * Note that the target can only be a folder. This is because in UNIX
 * systems, Java doesn't seem to be able to properly recognise whether the
 * name we keyed in is a folder name or a file name.
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version v05Dec2009
 */
public abstract class ControllerForDoubleSourceAndTarget extends BasicController {


	private String m_SourceLabel1, m_SourceLabel2, m_TargetLabel;

	/**
	 * Constructor
	 */
	public ControllerForDoubleSourceAndTarget(String a_SourceLabel1, String a_SourceLabel2, String a_TargetLabel) {
		super();
		m_SourceLabel1 = a_SourceLabel1;
		m_SourceLabel2 = a_SourceLabel2;
		m_TargetLabel = a_TargetLabel;
	}


	/**
	 * Retrieves the folder/file specifed as the source
	 * @return pointer to folder/file
	 */
	protected File GetSourceFile1() {
		return (File) GetOptionArgument(m_SourceLabel1);
	} // end GetSourceFile()


	protected File GetSourceFile2() {
		return (File) GetOptionArgument(m_SourceLabel2);
	}


	/**
	 * Retrieves the folder/file specifed as the target
	 * @return pointer to folder/file
	 */
	protected File GetTargetFile() {
		return (File) GetOptionArgument(m_TargetLabel);
	} // end GetTargetFile()




	/**
	 * Check to see if the provided source folder/file exists.
	 *
	 * @return true if the source folder/file exists, false otherwise.
	 */
	protected boolean Source1Exists() {

		File l_SourceFile = GetSourceFile1();
		return l_SourceFile.exists();

	} // end Source1Exists()

		/**
	 * Check to see if the provided source folder/file exists.
	 *
	 * @return true if the source folder/file exists, false otherwise.
	 */
	protected boolean Source2Exists() {

		File l_SourceFile = GetSourceFile2();
		return l_SourceFile.exists();

	} // end Source1Exists()


	/**
	 * Makes sure that the specified target folder exists.
	 * It is created if it is not.
	 *
	 * @return true if the target exists, or is created successfully, false otherwise
	 */
	protected boolean EnsureTargetExists() {

		File l_TargetFile = GetTargetFile();

		if (!DirectoryAndFileManipulation.CreateDirectoryIfNonExistent(l_TargetFile)) {
			Logger.getLogger(ControllerForSingleSourceAndTarget.class.getName()).log(Level.WARNING, "Unable to access the target folder.");
			return false;
		}

		return true;

	} // end EnsureTargetExists()


} // end class ControllerForDoubleSourceAndTarget
