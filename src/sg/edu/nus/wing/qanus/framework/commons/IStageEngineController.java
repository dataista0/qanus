package sg.edu.nus.wing.qanus.framework.commons;


/**
 * This interface needs to be implemented by classes which will invoke
 * the StageEngines. Typically this would be the Controller classes which
 * setup and start the StageEngines.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 16Jan2010
 */
public interface IStageEngineController {

	/**
	 * Provides the XML handlers to use with the StageEngine.
	 * The number of XML handlers required varies according to the StageEngine
	 *
	 * @return array of XML handlers to use.
	 */
	public IXMLParser[] GetXMLHandlersForStageEngine();


	/**
	 * Provides the modules to be invoked by the StageEngine
	 * @return array of modules to be invoked.
	 */
	public IRegisterableModule[] GetModulesForStageEngine();
	
} // end interface
