
package sg.edu.nus.wing.qanus.textprocessing;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.StringTokenizer;

import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;
import sg.edu.nus.wing.qanus.framework.util.DirectoryAndFileManipulation;
import edu.stanford.nlp.classify.ColumnDataClassifier;



/**
 * Wrapper of Stanford's Classifier by the Stanford NLP group. (http://nlp.stanford.edu/)
 * The implementation of the classifier doesn't de-couple very well, most of the
 * innards are tied to each other, and it's not worth the effort to change Stanford's
 * code for this wrapper because this defeats the purpose of a wrapper.
 * Thus, we run the classifier through the most reliable interface we have to it
 * instead, by invoking the jar in a separate shell instead.
 *
 * The model that we are using is trained over the training data provided at
 * http://l2r.cs.uiuc.edu/~cogcomp/Data/QA/QC/
 * (Li and Roth)
 * 2000 labeled questions were used as the training data.
 * The resulting classifier is what we used for this class.
 *
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class QuestionClassifierWithStanfordClassifier implements ITextProcessingModule {


	private final String CONFIG_FILE_NAME = "StanfordClassifer.settings";

	private final String TEST_FILE_NAME = "QC.temp";

	private final String RES_FILE_NAME = "QC-ans.temp";


	private String m_TestFileName, m_ConfigFileName, m_ResFileName;

	/**
	 * Constructor
	 * @param a_ClassifierFileName [in] file name of the trained classifier we want to use
	 * @param a_TempFolderName [in] name of temp folder to store our intermediary files
	 * @throws Exception if there are errors during initialization
	 */
	public QuestionClassifierWithStanfordClassifier(String a_ClassifierFileName, String a_TempFolderName) throws Exception {


		// Check that classifier file exist
		File l_ClassifierFile = new File(a_ClassifierFileName);
		if (!l_ClassifierFile.exists()) {
			throw new Exception("Exception initializing Question Classifier - Classifier file not found!");
		}

		// Ensure that temp folder exists
		File l_TargetFolder = new File(a_TempFolderName);
		if (!DirectoryAndFileManipulation.CreateDirectoryIfNonExistent(l_TargetFolder)) {
			throw new Exception("Exception initializing Question Classifier - Error accessing temp folder.");
		}
		m_TestFileName = l_TargetFolder.getAbsolutePath() + File.separator + TEST_FILE_NAME;
		m_ConfigFileName = l_TargetFolder.getAbsolutePath() + File.separator + CONFIG_FILE_NAME;
		m_ResFileName = l_TargetFolder.getAbsolutePath() + File.separator + RES_FILE_NAME;


		
		// Generate a configuration file used by the classifier in the temp directory
		try {
			CreateConfigFile(a_ClassifierFileName);
		} catch (Exception e) {
			throw new Exception("Exception initializing Question Classifier - Unable to create configuration file!");
		}
		

	} // end constructor




	/**
	 * Writes a config file to the temp folder,
	 * specifying in the config file the classifier serialised file.
	 * @param a_ClassifierFileName [in] file name where serialised classifier is stored
	 */
	private void CreateConfigFile(String a_ClassifierFileName) throws Exception {
		/*
		// Write the configuration file inside temp folder
		File l_ConfigFile = new File(m_ConfigFileName);
		BufferedWriter l_BW = new BufferedWriter(new FileWriter(l_ConfigFile, false));
		l_BW.write("useClassFeature=true");
		l_BW.newLine();
		l_BW.write("1.useNGrams=true");
		l_BW.newLine();
		l_BW.write("1.usePrefixSuffixNGrams=true");
		l_BW.newLine();
		l_BW.write("1.maxNGramLeng=4");
		l_BW.newLine();
		l_BW.write("1.minNGramLeng=1");
		l_BW.newLine();
		l_BW.write("1.binnedLengths=10,20,30");
		l_BW.newLine();
		l_BW.write("printClassifier=HighWeight");
		l_BW.newLine();
		l_BW.write("printClassifierParam=200");
		l_BW.newLine();
		l_BW.write("goldAnswerColumn=0");
		l_BW.newLine();
		l_BW.write("displayedColumn=1");
		l_BW.newLine();
		l_BW.write("intern=true");
		l_BW.newLine();
		l_BW.write("sigma=3");
		l_BW.newLine();
		l_BW.write("useQN=true");
		l_BW.newLine();
		l_BW.write("QNsize=15");
		l_BW.newLine();
		l_BW.write("tolerance=1e-4");
		l_BW.newLine();
		l_BW.write("testFile=" + DoubleSlashes(m_TestFileName));
		l_BW.newLine();
		l_BW.write("loadClassifier=" + a_ClassifierFileName);
		l_BW.newLine();

		l_BW.flush();
		l_BW.close();
*/
	} // end CreateConfigFile()



	/**
	 * Given a path, we add an extra slash to every slash.
	 * This is used because the Stanford Classifier seems to treat a single slash as
	 * an escape character when they pass the settings file.
	 * @param a_String [in] string to add slashes to
	 * @return a string with two slashes for every existing one
	 */
	private String DoubleSlashes(String a_String) {

		java.util.regex.Pattern l_Regex = null;
		try {
			l_Regex = java.util.regex.Pattern.compile("\\\\");
			java.util.regex.Matcher l_Matcher = l_Regex.matcher(a_String);
			String l_Result = l_Matcher.replaceAll("\\\\\\\\");
			return l_Result;
		} catch (Exception e) {
			// Some error! TODO
		}
		
		// Fail safe, just return the original string
		return a_String;

	} // end DoubleSlashes()


	/**
	 * Takes in a question and return its classification according to
	 * Li and Roth 2002.
	 * We only expect one input question each time.
	 * @param a_Sentences [in] input sentence, only 1st element of array expected.
	 * @return Label of question as 1st element of array, according to Li and Roth 2002, or null on errors.
	 */
	@Override
	public String[] ProcessText(String[] a_Sentences) {

		if (a_Sentences == null || a_Sentences[0].length() == 0) {
			return null;
		}

		String[] l_Result = new String[1];

		try {
			// Create test file for Stanford Classifier
			CreateTestFile(a_Sentences[0]);

			// Invoke Stanford Classifier after re-directing stdout
			PrintStream stdout = System.out;
			File l_ResFile = new File(m_ResFileName);
			PrintStream l_NewStdOut = new PrintStream(new FileOutputStream(l_ResFile, false));
			System.setOut(l_NewStdOut);

			String[] l_Arguments = {"-prop", m_ConfigFileName};
			ColumnDataClassifier.main(l_Arguments);

			System.setOut(stdout);
			

			// Retreive result by accessing the result file
			l_NewStdOut.flush();
			l_NewStdOut.close();

			BufferedReader l_BR = new BufferedReader(new FileReader(l_ResFile));
			String l_ResultLine = l_BR.readLine();
			l_BR.close();
			if (l_ResultLine != null && l_ResultLine.length() > 0) {
				StringTokenizer l_ST = new StringTokenizer(l_ResultLine, "\t");				
				String l_Question = l_ST.nextToken();
				String l_DummyAnswer = l_ST.nextToken();
				String l_Answer = l_ST.nextToken();
				l_Result[0] = l_Answer;
			}
			
		} catch (Exception ex) {
			// Error encountered, unable to read result, just say no result.
			return null;
		}

		return l_Result;

	} // end ProcessText()


	@Override
	public String GetModuleID() {
		return "QC";
	} // end GetModuleID()



	/**
	 * Writes the question we want to classify to a temp file, in a format
	 * ready for use by the Stanford classifier.
	 * If the temp file already exists before this, it will be overwritten.
	 * @param a_Question [in] the question to be classified
	 */
	private void CreateTestFile(String a_Question) throws Exception {

		// Write the test file inside temp folder
		File l_TestFile = new File(m_TestFileName);
		BufferedWriter l_BW = new BufferedWriter(new FileWriter(l_TestFile, false));
		l_BW.write("dummy-answer\t" + a_Question);
		l_BW.newLine();


		l_BW.flush();
		l_BW.close();

	} // end CreateTestFile()




} // end class
