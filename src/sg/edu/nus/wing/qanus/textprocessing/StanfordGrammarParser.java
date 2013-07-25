package sg.edu.nus.wing.qanus.textprocessing;



import java.util.LinkedList;

import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Options;




/**
 * A grammar parser by the Stanford NLP group. (http://nlp.stanford.edu/)
 * We are basically doing a wrapper here around the original source files from the Stanford group.
 * 
 * @author Ng, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class StanfordGrammarParser implements ITextProcessingModule {
	
	
	private String m_PCFGParserFileName;
		
	private LexicalizedParser m_LP;
	
	
	public StanfordGrammarParser(String a_ParserFileName) {
			
		m_PCFGParserFileName = a_ParserFileName;
				
		LoadParser();
		
		
	} // end constructor
	
	
	
	/**
	 * This function attempts to load the Stanford parser.
	 * If not loaded, m_LP will be set to null.
	 * If m_LP is not null when this function is called, it is nullified,
	 *  and we attempt to re-load the parser.
	 * 
	 * @return true on successful load. false otherwise. If false is returned, m_LP is nullified.
	 */
	private boolean LoadParser() {
		
		// Try to init the stanford parser
		m_LP = null;
		Options l_Options = new Options();
		try {
			m_LP = new LexicalizedParser(m_PCFGParserFileName, l_Options);
		} catch (IllegalArgumentException e) {
			System.err.println("StanfordGrammarParser::LoadParser() -> Error loading parser.");
			m_LP = null;
			return false;
		}	
		
		return true;
		
	} // end LoadParser()



	@Override
	public String[] ProcessText(String[] a_Sentences) {
		
		LinkedList<String> l_TaggedSentences = new LinkedList<String>();
		
		// Check that the parser is loaded properly. We cannot proceed otherwise.
		boolean l_ParserLoaded = false;
		if (m_LP == null)  {
			l_ParserLoaded = LoadParser();
			if (!l_ParserLoaded) {
				return null;
			}
		} // else assume parser is loaded already since m_LP is not null.
		
		
		// Parse each sentence
		for (String l_Sentence : a_Sentences) {
			
			if (m_LP.parse(l_Sentence)) {
				l_TaggedSentences.add(m_LP.getBestParse().toString());				
			}
			
		} // end for
		
		
		// Return the results
		return l_TaggedSentences.toArray(new String[0]);

	} // end ProcessText()



	@Override
	public String GetModuleID() {	
		return "GP";
		// end GetModuleID()
	}
	

} // end class
