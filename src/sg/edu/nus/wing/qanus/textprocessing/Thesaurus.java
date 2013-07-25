package sg.edu.nus.wing.qanus.textprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;

/**
 * Acts as a thesaurus.
 * Information courtesy of the Big Huge Thesaurus
 * http://words.bighugelabs.com/api.php
 *
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 28Dec2009
 */
public class Thesaurus implements ITextProcessingModule {


	public Thesaurus() {

	}


	/**
	 * Accepts a word to look up.
	 * @param a_Sentences array of words, only the first word will be looked up
	 * @return an array of strings containing results of the thesaurus look up, or null on errors
	 */
	public String[] ProcessText(String[] a_Sentences) {

		if (a_Sentences == null) {
			return null;
		}

		String l_WordToQuery = a_Sentences[0];
		String l_HTTPResponse = QueryWordViaHTTP(l_WordToQuery);

		StringTokenizer l_ST = new StringTokenizer(l_HTTPResponse, "\n");
		String[] l_Words = new String[l_ST.countTokens()];
		int i = 0;
		while (l_ST.hasMoreTokens()) {
			// A reply token has the format
			// POS|type|word
			// where POS can be noun, verb
			// type can be syn (synonym), ant (antonym), usr (user suggested) or rel (related words)
			// word is the actual word we want
			String l_Plain = l_ST.nextToken();

			// Filter out the antonyms
			StringTokenizer l_STReply = new StringTokenizer(l_Plain, "|");
			if (l_STReply.countTokens() != 3) {
				// Weird output, skip this one
				continue;
			}
			String l_POS = l_STReply.nextToken();
			String l_Type = l_STReply.nextToken();
			String l_Word = l_STReply.nextToken();
			if (l_Type.compareToIgnoreCase("ant") == 0 ||
				l_Type.compareToIgnoreCase("rel") == 0) {
				continue;
			}
			// TODO filter based on POS also?

			// If multi-word, enclose in quotations.
			if (l_Word.contains(" ")) {
				l_Word = "\"" + l_Word + "\"";
			}

			l_Words[i++] =  l_Word;
		}

		return l_Words;

	} // end ProcessText()


	/**
	 * Look up a given word over HTTP to the BHT.
	 * @param a_Word [in] word to look up
	 * @return HTTP response of the query, or an empty string on errors
	 */
	private String QueryWordViaHTTP(String a_Word) {

		String l_OutStr = "";
		String l_URLStr = "http://words.bighugelabs.com/api/2/f5e250ab8c6236ae03da813b187f34b9/" + a_Word + "/";
		try {
			InputStream l_HTTPResponseStream = new URL(l_URLStr).openStream();

			BufferedReader l_BR = new BufferedReader(new InputStreamReader(l_HTTPResponseStream));
			String l_ReadLine = "";
			while ((l_ReadLine = l_BR.readLine()) != null) {
				l_OutStr += l_ReadLine + "\n";
			}
		} catch (MalformedURLException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Unable to query thesarus for word [" + a_Word + "]", ex);
		} catch (IOException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Unable to query thesarus for word [" + a_Word + "]", ex);
		}

		return l_OutStr;

	} // end QueryWordViaHTTP()

	public String GetModuleID() {
		return "ThesaurusByBHT";
	}



} // end class Thesaurus
