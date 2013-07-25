package sg.edu.nus.wing.qanus.framework.commons;


import java.io.File;
import java.util.LinkedList;


/**
 * Class is the basic processing unit for each pipeline stage.
 * Typically takes in some input, invokes registered processing modules on these input,
 * and stores the output in another file.
 *
 * Usually paired with a FrameworkController in a stage which will invoke this class.
 * 
 * 
 * @author Ng, Jun Ping -- junping@comp.nus.edu.sg
 * @version 18Jan2010
 */
public abstract class StageEngine {
	
	
	// The XML handler used to parse and interpret input files
	private IXMLParser m_XMLHandler;
	
	// Source folder/file to read input XML files from
	private File m_SourceFile;
	
	// Folder/file to place result files in.
	private File m_TargetFile;

	// List of registered evaluation metrics
	protected LinkedList<IRegisterableModule> m_ModuleList;

	
	
	/**
	 * Constructor.
	 * Creates an object with no initialized component (XML handler, source and target folders)
	 */
	public StageEngine() {
		this(null, null, null);
	} // end constructor
	
	
	/**
	 * Constructor.
	 * 
	 * @param a_XMLHandler [in] XML handler to use to parse input XML files.
	 */
	public StageEngine(IXMLParser a_XMLHandler, File a_Source, File a_Target) {
		
		SetXMLHandler(a_XMLHandler);
		SetSourceFile(a_Source);
		SetTargetFile(a_Target);

		m_ModuleList = new LinkedList<IRegisterableModule>();
		
	} // end constructor


	/**
	 * Registers modules to be invokved by this StageEngine.
	 *
	 * @param a_Module [in] module to register with this stage engine to be invoked subsequently.
	 */
	public void RegisterModule(IRegisterableModule a_Module) {
		m_ModuleList.addLast(a_Module);
	}

	/**
	 * Register modules to be invoked by this stage engine
	 * 
	 * @param a_Modules [in] array of modules to be registered
	 */
	public void RegisterModules(IRegisterableModule[] a_Modules) {

		for (IRegisterableModule l_Module : a_Modules) {			
			RegisterModule(l_Module);			
		}

	} // end RegisterModules()
	

	/**
	 * Sets the source folder to read XML files from.
	 * 
	 * @param a_SourceFolder [in] source folder to read XML files from.
	 */
	public void SetSourceFile(File a_SourceFolder) {
		m_SourceFile = a_SourceFolder;
	} // end SetSourceFile()
	

	/**
	 * Retrieves the source folder to read XML files from.
	 * 
	 * @return the source folder
	 */
	public File GetSourceFile() {
		return m_SourceFile;
	} // end GetSourceFile()
	
	

	/**
	 * Sets the target folder to read XML files from.
	 * 
	 * @param a_TargetFolder [in] target folder to read XML files from.
	 */
	public void SetTargetFile(File a_TargetFolder) {
		m_TargetFile = a_TargetFolder;
	} // end SetTargetFile()
	

	
	/**
	 * Retrieves the target folder to read XML files from.
	 * 
	 * @return the target folder
	 */
	public File GetTargetFile() {
		return m_TargetFile;
	} // end GetTargetFile()

	/**
	 * Sets the XML handler to use to read the XML files with.
	 * 
	 * @param a_XMLHandler [in] the XML handler to use
	 */
	public void SetXMLHandler(IXMLParser a_XMLHandler) {
		m_XMLHandler = a_XMLHandler;
	} // end SetXMLHandler()
	
	
	/**
	 * Retrieves the XML handler to use with the input XML files.
	 * 
	 * @return the XML handler.
	 */
	public IXMLParser GetXMLHandler() {
		return m_XMLHandler;
	} // end GetXMLHandler()


	
	/**
	 * Kicks-start the processing.
	 * This function is to be implemented by derived classes, and determines the functionality of this module.
	 * Typically we will read in the XML files, parse them and process the results as desired and stored in the
	 * target folder.
	 * 
	 * @return true if source folder processed successfully, or false on fatal errors.
	 */
	public abstract boolean Go();
	
	

} // end StageEngine()


