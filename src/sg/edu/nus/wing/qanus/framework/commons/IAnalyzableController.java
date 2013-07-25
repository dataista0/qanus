package sg.edu.nus.wing.qanus.framework.commons;


import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalyzer;

/**
 * Interface to be implemented by controller components which will need to set an
 * error analysis engine.
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Feb 4, 2010
 */
public interface IAnalyzableController {

	/**
	 * Override this method if error analysis is to be carried out.	 
	 */
	public ErrorAnalyzer GetErrorAnalysisEngine();
	
}
