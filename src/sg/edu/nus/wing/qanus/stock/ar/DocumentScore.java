package sg.edu.nus.wing.qanus.stock.ar;

/**
 * Used to store documents and their associated scores in a priority queue
 * for efficiency and easy bookkeeping.
 * Typically used during document ranking.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 04Jan2010
 */
public class DocumentScore implements Comparable {

	private int m_DocID;     // ID of the document
	private double m_Score;  // Score of document
	private String m_DocText;// Text of document

	public DocumentScore(int a_DocID, double a_Score, String a_DocText) {		
		m_DocID = a_DocID;
		m_Score = a_Score;
		m_DocText = a_DocText;
	}

	public double GetScore() {
		return m_Score;
	}

	public int GetDocID() {
		return m_DocID;
	}

	public String GetDocText() {
		return m_DocText;
	}

	public void AddToScore(double a_Part) {
		m_Score += a_Part;
	}

	public int compareTo(Object o) {
		if (o == null) {
			return -1;
		}
		DocumentScore l_Object = (DocumentScore) o;
		// We want objects with higher score to go in front, to be used in Java's priority queue
		// This is the score is larger, we return -1
		if (m_Score > l_Object.GetScore()) {
			return -1;
		} else if (m_Score == l_Object.GetScore()) {
			return 0;
		} else {
			return 1;
		}
	} // end compareTo()

	
	@Override
	public boolean equals(Object o) {
		// Check for self-comparison
		if (this == o) {
			return true;
		}
		//use instanceof instead of getClass here for two reasons
		//1. if need be, it can match any supertype, and not just one class;
		//2. it renders an explict check for "that == null" redundant, since
		//it does the check for null already - "null instanceof [type]" always
		//returns false. (See Effective Java by Joshua Bloch.)
		if (!(o instanceof DocumentScore)) {
			return false;
		}
		//Alternative to the above line :
		//if ( aThat == null || aThat.getClass() != this.getClass() ) return false;
		//cast to native object is now safe
		DocumentScore l_Object = (DocumentScore) o;
		//now a proper field-by-field evaluation can be made
		return m_DocID == l_Object.GetDocID();
	} // end equals()

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 41 * hash + this.m_DocID;
		return hash;
	} // end hashCode()

	/**
	 * Multiply a factor into existing score
	 * @param a_Factor [in] factor to use
	 */
	private void MultiplyToScore(double a_Factor) {
		m_Score = m_Score / a_Factor;
	} // end MultiplyToScore()

} // end class DocumentScore

