package sg.edu.nus.wing.qanus.framework.commons;



/**
 * Interface to be implemented by classes used as SAX-based XML handlers.
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public interface IXMLParser {

	// TODO Possibly merge this into a class that extend from DefaultHandler??

	/**
	 * Register with this parser to receive call back notifications.
	 * @param a_Receipient [in] receipient whose Notify() method will be invokved.
	 */
	public void RegisterForNotification(IXMLDataReceipient a_Receipient);


	/**
	 * Stop receiving callbacks from this parser
	 *
	 * @param a_Receipient [in] receipient who wants to be de-listed.
	 */
	public void DelistFromNotification(IXMLDataReceipient a_Receipient);
	
} // end interface
