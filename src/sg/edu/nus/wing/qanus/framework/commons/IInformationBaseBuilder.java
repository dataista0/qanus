package sg.edu.nus.wing.qanus.framework.commons;



/**
 * Interface to be implemented by information base builders.
 * Information base builders are invokved by the InformationBaseEngine when processing
 * the input corpus.
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Jan 18, 2010
 */
public interface IInformationBaseBuilder {

	/**
	 * Adds information to the information base.
	 * The interpretation of the input DataItem is subject to the implementation
	 * of the QA modules.
	 * 
	 * @param a_Item [in] info to be added to the information base
	 * @return true if info added successfully, false otherwise
	 */
	public boolean AddToInfoBase(DataItem a_Item);

} // end interface IInformationBaseBuilder
