package sg.edu.nus.wing.qanus.framework.commons;



/**
 * Interface to be implemented by classes which wants to be the target of callbacks
 * by our SAX based XML parser.
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public interface IXMLDataReceipient {

	/**
	 * Retrieves a string that uniquely identifies this class
	 * 
	 * @return unique identifier string.
	 */
	public String GetIdentifier();
	
	
	/**
	 * Gets notification for a new data item.
	 * This is the method that gets invokved when the callback on the SAX parser
	 * is activated.
	 * 
	 * @param a_Item [in] the data item to take in
	 */
	public void Notify(DataItem a_Item);
	
} // end interface
