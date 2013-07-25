package sg.edu.nus.wing.qanus.textprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;

/**
 * Used to remove stop words.
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 */
public class StopWordsFilter implements ITextProcessingModule {


	private String[] m_StopWordFileNames;

	private HashSet<String> m_StopWords;


	/**
	 * Constructor that builds up a list of stop words.
	 * The data in the files are expected to be comma delimited stop words.
	 * @param a_FileNames [in] array of file names of stop words files.
	 */
	public StopWordsFilter(String[] a_FileNames) {

		m_StopWords = new HashSet<String>();

		m_StopWordFileNames = a_FileNames;

		// Load stop words
		for (String l_FileName : m_StopWordFileNames) {
			
			try {
				// Read in character by character from the file, individual words
				// are assumed to be delimited by commas
				FileReader l_Reader = new FileReader(new File(l_FileName));
				int l_Character;
				try {
					l_Character = l_Reader.read();
					String l_Word = "";
					while (l_Character != -1) {
						if (l_Character != ',') {
							l_Word += (char) l_Character;
						} else {
							m_StopWords.add(l_Word);
							l_Word = "";
						}
						l_Character = l_Reader.read();
					}
					// Don't let the last word get away
					if (l_Word.length() > 0) {
						m_StopWords.add(l_Word);
					}
				} catch (IOException ex) {
					Logger.getLogger(StopWordsFilter.class.getName()).log(Level.WARNING, "Error reading from stop words file [" + l_FileName + "]", ex);
				}
				
			} catch (FileNotFoundException ex) {
				Logger.getLogger(StopWordsFilter.class.getName()).log(Level.WARNING, "Stop word file [" + l_FileName + "] not found.", ex);
			} // end try-catch

		} // end for


	} // end constructor



	/**
	 * Checks if a given word belongs to the stop word list.
	 * @param a_Word [in] word to be checked
	 * @return true if the word is a stop word, false otherwise
	 */
	public boolean IsStopWord(String a_Word) {

		return (m_StopWords.contains(a_Word));

	} // end IsStopWord()
	

	/**
	 * Removes instances of stop words from a given string.
	 * @param a_String [in] the string to act on.	
	 * @return a string with stop words removed.
	 */
	private String RemoveStopWords(String a_String)  {	
				
		StringTokenizer l_ST = new StringTokenizer(a_String);
		String l_OutputString = "";
		while (l_ST.hasMoreTokens()) {

			String l_Word = l_ST.nextToken();
			if (!(m_StopWords.contains(l_Word))) {
				l_OutputString += l_Word + " ";
			}

		}

		return l_OutputString.trim();

	} // end RemoveStopWords()

	
	public String[] ProcessText(String[] a_Sentences) {
		
		// Sanity check
		if (a_Sentences == null) return null;

		String[] l_Result = new String[a_Sentences.length];
		int l_Index = 0;
		for (String l_Sentence : a_Sentences) {
			l_Result[l_Index] = RemoveStopWords(l_Sentence);
			l_Index++;
		} // end for

		return l_Result;

		
	} // end ProcessText()


	public String GetModuleID() {
		return "StopWords";
	} // end GetModuleID()



} // end class StopWordsFilter
