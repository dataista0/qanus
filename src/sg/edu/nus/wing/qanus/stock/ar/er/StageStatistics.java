package sg.edu.nus.wing.qanus.stock.ar.er;

/**
 * Implements a simple class acting as a data structure to tabulate intermediate
 * results as error analysis is performed.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Feb 4, 2010
 */
public class StageStatistics {

	private int m_Total;
	private int m_PotentiallyCorrect;


	public StageStatistics() {
		m_Total = m_PotentiallyCorrect = 0;
	}

	public void AddPotentiallyCorrect() {
		m_PotentiallyCorrect++;
		m_Total++;
	}

	public void AddWrong() {
		m_Total++;
	}

	public double GetPercentagePotentiallyCorrect() {
		return (double) m_PotentiallyCorrect / m_Total;
	}
} // end class StageStatistics
