package sg.edu.nus.wing.qanus.mitic.qp;

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
 * Handles the XML encoded question file from TREC 2007.
 * If you want to use other file formats, just change this XML handler and update
 * QuestionProcessor to use it.
 *
 * This handler builds a representation of the XML questions, each time when
 * it has parsed a <TARGET> tag set of questions, it invokes call backs to
 * inform waiting modules.
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class TRECQuestionXMLHandler extends DefaultHandler implements IXMLParser {

	// TODO the State engine part is untidy - to clean up

	// Holds modules which want to receive call backs
	Vector<IXMLDataReceipient> m_Receipients;

	
	// Controls whether or not to spill out status messages, such as error message and progress messages
	private boolean m_Verbose = false;
	
		// We implement the parsing as a state machine.
	private enum m_enumStates {Start, TRECQA, TARGET, QA, Q};
	
	// Track current state
	private m_enumStates m_CurrentState;
	
	
	// Used to progressive build up a DataItem to pass to intermediary	
	private DataItem m_DataItem_TARGET;
	private DataItem m_DataItem_Q;
	// Temporarily hold characters as they come in bit by bit from the characters() call-back.
	private String m_CurrentTextBuffer;
	
	
	
	/**
	 * Constructor.
	 * @param a_Intermediary [in] the intermediary class which contains all the text modules we want to run.
	 */
	public TRECQuestionXMLHandler() {
		
		m_CurrentState = m_enumStates.TRECQA;		
		m_DataItem_TARGET = null;
		m_DataItem_Q = null;
		
		m_Receipients = new Vector<IXMLDataReceipient>();
	}
	
	
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
	
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
	

		// Models state transitions, see documentation for state machine details.
		
		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;
		
				
		
		// Look out for TARGET tag
		if (m_CurrentState == m_enumStates.TRECQA) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("target") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.TARGET;
				l_GoodTransition = true;
				
				// Extract attributes and create new DataItem describing this XML instance
				if (attributes != null) {			
					String l_QID = attributes.getValue("id");
					String l_Text = attributes.getValue("text");
					
					//if (m_DataItem_TREC_QA != null) {
					if (m_Verbose && m_DataItem_TARGET != null) {
						Logger.getLogger("QANUS").logp(Level.WARNING, TRECQuestionXMLHandler.class.getName(), "startElement", "Possible overwrite of in-complete TARGET DataItem.");
					}

					m_DataItem_TARGET = new DataItem("target");
					m_DataItem_TARGET.AddAttribute("id", l_QID);
					m_DataItem_TARGET.AddAttribute("type", l_Text);


					//m_DataItem_TREC_QA.AddField("target", m_DataItem_TARGET);
					//}
								
				}
				
			} // end if (qName.com....			
		}
		
		// Look out for QA tag
		else if (m_CurrentState == m_enumStates.TARGET) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("qa") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.QA;
				l_GoodTransition = true;
								
			} // end if (qName.com....			
		}
		
		// The Q tag
		else if (m_CurrentState == m_enumStates.QA) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("q") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.Q;
				l_GoodTransition = true;
				
				// Extract attributes and create new DataItem describing this XML instance
				if (attributes != null) {			
					String l_QID = attributes.getValue("id");
					String l_Type = attributes.getValue("type");			

					if (m_DataItem_TARGET != null) {
						
						if (m_Verbose && m_DataItem_Q != null) {
							Logger.getLogger("QANUS").logp(Level.WARNING, TRECQuestionXMLHandler.class.getName(), "startElement", "Possible overwrite of in-complete Q DataItem.");
						}
						
						m_DataItem_Q = new DataItem("q");						
						m_DataItem_Q.AddAttribute("id", l_QID);
						m_DataItem_Q.AddAttribute("type", l_Type);						
						
						m_DataItem_TARGET.AddField("q", m_DataItem_Q);
						
						m_CurrentTextBuffer = "";
					}
							
				}
				
			} // end if (qName.com....			
		}
		
			
		// 
		else if (m_CurrentState == m_enumStates.Q) {
			// Nothing, no transitions expected out of this state
		}
		
		
		
		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, TRECQuestionXMLHandler.class.getName(), "startElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, TRECQuestionXMLHandler.class.getName(), "startElement", "[" + m_CurrentState + "]");
		}
		
		
	}
	
	@Override
	public void endElement( String uri, String localName, String qName ) throws SAXException {

		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;		
		
		
		// End of TARGET tag
		if (m_CurrentState == m_enumStates.TARGET) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("target") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.TRECQA;
				l_GoodTransition = true;

				// Notify intermediary, and perform housekeeping to get ready for next basic unit of info
				for (IXMLDataReceipient l_Receipients : m_Receipients) {
					l_Receipients.Notify(m_DataItem_TARGET);
				} // end for
				
				m_DataItem_TARGET = null;
				m_DataItem_Q = null;
								
			} // end if (qName.com....			
		}
		
		// End of QA tag
		else if (m_CurrentState == m_enumStates.QA) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("qa") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.TARGET;
				l_GoodTransition = true;
								
				
			} // end if (qName.com....			
		}
		
			
		// End of Q tag
		else if (m_CurrentState == m_enumStates.Q) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("q") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.QA;
				l_GoodTransition = true;								

				// Add the text we have buffered to the DataItem
				if (m_DataItem_Q == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_DataItem_Q.AddValue(m_CurrentTextBuffer);
				
				m_DataItem_Q = null;
				
			} // end if (qName.com....	

		}
		
		
		
		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, TRECQuestionXMLHandler.class.getName(), "endElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, TRECQuestionXMLHandler.class.getName(), "endElement", "[" + m_CurrentState + "]");
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
		if (m_CurrentState == m_enumStates.Q) {
			m_CurrentTextBuffer += l_Text;
		}
		
	}
		
	
} // end TRECQuestionXMLHandler()
