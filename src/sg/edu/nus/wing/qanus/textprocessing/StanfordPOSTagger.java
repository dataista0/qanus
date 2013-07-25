
package sg.edu.nus.wing.qanus.textprocessing;



import java.util.LinkedList;

import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


/**
 * A POS tagger by the Stanford NLP group. (http://nlp.stanford.edu/)
 * We are basically doing a wrapper here around the original source files from the Stanford group.
 * 
 * 
 * @author Ng, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class StanfordPOSTagger implements ITextProcessingModule {

	private String m_ModelFileName;
	
	private MaxentTagger m_Tagger = null;
	
	
	/**
	 * Initializes a new instance of the tagger.
	 * 
	 * @param a_ModelFileName [in] file name of the model file.
	 */
	public StanfordPOSTagger(String a_ModelFileName) {
		
		m_ModelFileName = a_ModelFileName;
		
		LoadTagger();
		
	} // end constructor
	
	
	/**
	 * This function attempts to load the Stanford tagger.
	 * If not loaded, m_Tagger will be set to null.
	 * If m_Tagger is not null when this function is called, it is nullified,
	 *  and we attempt to re-load the parser.
	 * 
	 * @return true on successful load. false otherwise. If false is returned, m_Tagger is nullified.
	 */
	private boolean LoadTagger() {
		
		m_Tagger = null;
		try {
			m_Tagger =  new MaxentTagger(m_ModelFileName);
		} catch (Exception e) {
			System.err.println("StanfordPOSTagger::LoadTagger() -> Error loading tagger :" + e);
			m_Tagger = null;
			return false;
		}
		
		return true;
		
	} // end LoadTagger()


	/* (non-Javadoc)
	 * @see sg.edu.nus.wing.qanus.knp.TextProcessorModule#GetModuleID()
	 */
	@Override
	public String GetModuleID() {		
		return "POS";
	} // end GetModuleID()

	
	/* (non-Javadoc)
	 * @see sg.edu.nus.wing.qanus.knp.TextProcessorModule#ProcessText(java.lang.String[])
	 */
	@Override
	public String[] ProcessText(String[] a_Sentences) {

		LinkedList<String> l_TaggedSentences = new LinkedList<String>();
		
		// Check that the tagger is loaded properly. We cannot proceed otherwise.
		boolean l_TaggerLoaded = false;
		if (m_Tagger == null)  {
			l_TaggerLoaded = LoadTagger();
			if (!l_TaggerLoaded) {
				return null;
			}
		} // else assume tagger is loaded already since m_Tagger is not null.
		
		
		// Parse each sentence
		for (String l_Sentence : a_Sentences) {
			
			try {
				l_TaggedSentences.add(MaxentTagger.tagString(l_Sentence));
			} catch (Exception e) {
				System.err.println("StanfordPOSTagger::LoadTagger() -> Error tagging sentence [" + l_Sentence + "]");
				continue;
			}
			
		} // end for
		
		
		// Return the results
		return l_TaggedSentences.toArray(new String[0]);
		
	} // end ProcessText()

} // end class

