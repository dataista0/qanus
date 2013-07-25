package sg.edu.nus.wing.qanus.stock.ibp;


import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IXMLDataReceipient;
import sg.edu.nus.wing.qanus.framework.commons.IXMLParser;


/**
 * The handler class used for call-backs when sending the AQUAINT2 corpus through Java's SAX parser.
 * Typically if you are using a different corpus, you need to provide your own
 * implementation for this handler. Your custom class just needs to extend from DefaultHandler.
 *  
 * You can change the handler you want to use in the Controller class.
 * 
 * The ITextModuleIntermediary must be implemented by any class which contains all the text modules to run.
 * This handler will inform the Intermediary of parsed XML nodes. The Intermediary will be responsible
 * for sending this information to the text modules.
 * 
 * When the intermediary is notified, it will activate each text module in turn, serially.
 * 
 * 
 * The aim of this module is to build up a representation of the XML we are parsing, and then
 * hand this representation over to the Intermediary for easier processing.
 * The representation, called a DataItem is essentially a basic unit of information in the XML.
 * For example for the AQUAINT2 corpus, this would be the <DOC> item. Each data item will thus
 * capture the information of each <DOC> item. By doing this we de-couple the structure of the XML
 * from the rest of the implementation.
 * 
 * To receive the parsed data items, we can create a class that implements
 * IXMLDataReceipient and register with this class for notifcation whenever new 
 * data items are parsed.
 * 
 * @author Ng, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class AQUAINT2XMLHandler extends DefaultHandler implements IXMLParser {
	
	
	Vector<IXMLDataReceipient> m_Receipients;
	
	
	// Controls whether or not to spill out status messages, such as error message and progress messages
	private boolean m_Verbose = false;
	
	
	// We implement the parsing as a state machine.
	// The FSM looks something like this for the AQUAINT2 corpus :
	//
	//  ---> S0 -----DOC----> S1 -----HEADLINE----> S2
	//                           -----DATELINE----> S3
	//                           --------TEXT-----> S4 <--- P -- (self transition)
	//                                               |---------|
	//
	private enum m_enumStates {Start, DOC, HEADLINE, DATELINE, TEXT, SENTENCE};
		// State names corresponding to S0, S1, S2, S3, S4 in diagram above
	
	// Track current state
	private m_enumStates m_CurrentState;
	
	
	// Used to progressive build up a DataItem to pass to intermediary
	private DataItem m_ReadDataItem;
	// Temporarily hold characters as they come in bit by bit from the characters() call-back.
	private String m_CurrentTextBuffer;
	private String m_CurrentTextTag;
	
	
	
	/**
	 * Constructor.
	 * @param a_Intermediary [in] the intermediary class which contains all the text modules we want to run.
	 */
	public AQUAINT2XMLHandler() {	
		m_CurrentState = m_enumStates.Start;
		m_ReadDataItem = null;
		m_Receipients = new Vector<IXMLDataReceipient>();
	}
	
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		
			
		// 5 states, 5 separate IF statements
		// Models state transitions, see documentation for state machine details.
		
		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;
		
		
		
		
		// StartState S0
		if (m_CurrentState == m_enumStates.Start) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("DOC") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
				// Extract attributes and create new DataItem describing this XML instance
				if (attributes != null) {			
					String l_DocID = attributes.getValue("id");
					String l_Type = attributes.getValue("type");			
					if (l_DocID != null && l_DocID.length() > 0) {
						if (m_Verbose && m_ReadDataItem != null) {
							Logger.getLogger("QANUS").logp(Level.WARNING, AQUAINT2XMLHandler.class.getName(), "startElement", "Possible overwrite of incomplete DataItem.");
						}
						m_ReadDataItem = new DataItem("DOC");
						m_ReadDataItem.AddAttribute("id", l_DocID);
						m_ReadDataItem.AddAttribute("type", l_Type);
					}			
				}
				
			} // end if (qName.com....			
		}
		
		// State DOC, S1
		else if (m_CurrentState == m_enumStates.DOC) {
			
			// Transitions
			if (qName.compareToIgnoreCase("HEADLINE") == 0) {
				m_CurrentState = m_enumStates.HEADLINE;
				l_GoodTransition = true;
				m_CurrentTextBuffer = ""; // <--- buffer used to build up inner text
			} else
			if (qName.compareToIgnoreCase("DATELINE") == 0) {
				m_CurrentState = m_enumStates.DATELINE;
				l_GoodTransition = true;
				m_CurrentTextBuffer = "";
			} else
			if (qName.length() >=4 && (qName.substring(0, 4).compareTo("TEXT") == 0)) {
					
				m_CurrentTextTag = qName;		
					
				m_CurrentState = m_enumStates.TEXT;
				l_GoodTransition = true;
				m_CurrentTextBuffer = ""; // <--- strictly speaking not required, since we expect 
										  //      the text to be within <P> tags.
			}
		}
		
		
		// State HEADLINE, S2
		else if (m_CurrentState == m_enumStates.HEADLINE) {
			
		}	
		
		
		// State DATELINE, S3
		else if (m_CurrentState == m_enumStates.DATELINE) {
			
		}
		
		
		// State TEXT, S4
		else if (m_CurrentState == m_enumStates.TEXT) {
			// Acceptable states include P - but no need to transit out
			if (qName.compareToIgnoreCase("P") == 0) {				
				m_CurrentState = m_enumStates.SENTENCE;
				l_GoodTransition = true;				
				m_CurrentTextBuffer = ""; // <--- important, clear buffer before each <P> tag.
			}
		}
		
		
		// State SENTENCE, S5
		else if (m_CurrentState == m_enumStates.SENTENCE) {
			// Nothing, no transitions expected out of this state
		}
		
		
		
		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, AQUAINT2XMLHandler.class.getName(), "startElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}
		
		if (m_Verbose) {			
			Logger.getLogger("QANUS").logp(Level.FINER, AQUAINT2XMLHandler.class.getName(), "startElement", "[" + m_CurrentState + "]");
		}

	} // end startElement()
	
	
	
	@Override
	public void endElement( String uri, String localName, String qName ) throws SAXException {

		
		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;			
		
		
		// State DOC - S1
		if (m_CurrentState == m_enumStates.DOC) {
			if (qName.compareToIgnoreCase("DOC") == 0) {
				
				// Update transition
				m_CurrentState = m_enumStates.Start;
				l_GoodTransition = true;
				
				// Notify intermediary, and perform housekeeping to get ready for next basic unit of info				
				for (IXMLDataReceipient l_Receipients : m_Receipients) {
					l_Receipients.Notify(m_ReadDataItem);
				} // end for
				m_ReadDataItem = null;
			}
		}
		
		
		// State HEADLINE - S2
		else if (m_CurrentState == m_enumStates.HEADLINE) {
			if (qName.compareToIgnoreCase("HEADLINE") == 0) {
				
				// Update transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
				// Add the HEADLINE field to the DataItem being built up.
				if (m_ReadDataItem == null) return; // This shouldn't be the case, should we throw a warning?\
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				m_ReadDataItem.AddField("HEADLINE", m_CurrentTextBuffer);
			}
		}
		
		
		// State DATELINE - S3
		else if (m_CurrentState == m_enumStates.DATELINE) {
			if (qName.compareToIgnoreCase("DATELINE") == 0) {
				
				// Update transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
				// Add the DATELINE field to the DataItem being built up.
				if (m_ReadDataItem == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				m_ReadDataItem.AddField("DATELINE", m_CurrentTextBuffer);
			}
		}
		
		
		// State TEXT - S4
		else if (m_CurrentState == m_enumStates.TEXT) {
			
			if (qName.length() >=4 && (qName.substring(0, 4).compareTo("TEXT") == 0)) {
				
				// Update transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
			}			
		}
		
		
		// State SENTENCE - S5
		else if (m_CurrentState == m_enumStates.SENTENCE) {
			if (qName.compareToIgnoreCase("P") == 0) {
				
				// Update transition
				m_CurrentState = m_enumStates.TEXT;
				l_GoodTransition = true;
				
				// Add the text we have buffered to the DataItem
				if (m_ReadDataItem == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_ReadDataItem.AddSubFieldValue(m_CurrentTextTag, "P", m_CurrentTextBuffer);
			}			
		}
		
		
		
		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, AQUAINT2XMLHandler.class.getName(), "endElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}
		
		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, AQUAINT2XMLHandler.class.getName(), "endElement", "[" + m_CurrentState + "]");
		}


		
	} // end endElement()
	
	
	@Override
	public void characters(char buffer[], int offset, int length) throws SAXException {
		
		
		String l_Text = new String(buffer, offset, length);
		if (l_Text.length() == 0) return;
		// Strip away any stray carriage returns
		l_Text = l_Text.replaceAll("\n", " ");		
		
		
		// Note that over here, we only append the received text to our CurrentTextBuffer.
		// We do this because due to limited buffer size, not all the inner text enclosed between
		// a XML tag will be obtained in just one call-back. There could be multiple call-backs
		// for one set of inner text. We will buffer all of these, until the endElement() call-back
		// is invoked, then we will know we have all the text.
		
		
		if (m_CurrentState == m_enumStates.Start) {
			// Nothing to do
		}
		
		if (m_CurrentState == m_enumStates.HEADLINE) {			
			m_CurrentTextBuffer += l_Text;
		}
		
		if (m_CurrentState == m_enumStates.DATELINE) {
			m_CurrentTextBuffer += l_Text;
		}
		
		if (m_CurrentState == m_enumStates.TEXT) {
			// Nothing to do
		}
		
		if (m_CurrentState == m_enumStates.SENTENCE) {
			m_CurrentTextBuffer += l_Text;
		}
		
		
	} // end characters()


	/**
	 * Stop receiving notifications from this XML handler about new
	 * data items. If the recipient has yet to be registered for 
	 * notification previously, nothing is done.
	 * 
	 * @param a_Receipient [in] the recipient to delist.
	 */
	@Override
	public void DelistFromNotification(IXMLDataReceipient a_Receipient) {
		
		// Check for duplicates - we only register each recipient once
		for (int i = 0; i < m_Receipients.size(); ++i) {
			if (m_Receipients.get(i).GetIdentifier().compareTo(a_Receipient.GetIdentifier()) == 0) {
				// Found, delete it
				m_Receipients.removeElementAt(i);
			}
		} // end for
	
		return;
		
	} // end DelistFromNotification()


	
	/**
	 * Use this method to register a class to receive notifications whenever
	 * a new data item is parsed from the XML file.
	 * 
	 * If the intended recipient (of the notification) is already registered,
	 * nothing is done. Recipients are identified by their unique identifiers
	 * (using GetIdentifier()).
	 * 
	 * @param a_Receipient [in] the class to receive the notification with
	 */
	@Override
	public void RegisterForNotification(IXMLDataReceipient a_Receipient) {
		
		// Check for duplicates - we only register each recipient once
		for (IXMLDataReceipient l_Receipient : m_Receipients) {
			if (l_Receipient.GetIdentifier().compareTo(a_Receipient.GetIdentifier()) == 0) {
				// Already present, we skip it
				return;
			}
		} // end for
		
		m_Receipients.add(a_Receipient);
		
	} // end RegisterForNotification()
		
	
} // end class
