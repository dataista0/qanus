
package sg.edu.nus.wing.qanus.stock.ar;

/**
 * Used to store an answer candidate, alongside the source passage from which it
 * is extracted.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 04Jan2010
 */
public class AnswerCandidate {

	private String m_Answer;
	private String m_Source_Orig;
	

	/**
	 * Constructor
	 * @param a_Answer [in] answer candidate string
	 * @param a_Source [in] original source of candidate
	 * @param a_SourcewTags [in] source of candidate annotated with POS
	 */
	public AnswerCandidate(String a_Answer, String a_Source) {
		m_Answer = a_Answer;
		m_Source_Orig = a_Source;		
	}

	public String GetAnswer() {
		return m_Answer;
	}

	public String GetOrigSource() {
		return m_Source_Orig;
	}


} // end class AnswerCandidate
