package sg.edu.nus.wing.qanus.framework.commons;

/**
 * Interface for modules that can be registered with a StageEngine
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 15Jan2010
 */
public interface IRegisterableModule {


	/**
	 * Retrieves an identifier to recognise the module with.
	 * Ideally, within the whole system the text modules should have unique IDs.
	 *
	 * Some guidelines which are currently not enforced by the system :
	 * 1. unique system wide
	 * 2. no spaces in between, or any other XML special characters because
	 *    this name would be used to form a XML tag.
	 * @return a string which can be used to identify the module
	 */
	public String GetModuleID();
	
	
} // end interface
