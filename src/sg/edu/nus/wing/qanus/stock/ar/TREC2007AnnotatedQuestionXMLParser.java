
package sg.edu.nus.wing.qanus.stock.ar;

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
 * Build a representation from annotated XML question file from the QP
 * phase. This will allow us to start processing the questions for answer
 * retrieval.
 * 
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class TREC2007AnnotatedQuestionXMLParser extends DefaultHandler implements IXMLParser {


	// Modules which want to receive notification of parsed questions
	private Vector<IXMLDataReceipient> m_Receipients;


	// Controls whether or not to spill out status messages, such as error message and progress messages
	private boolean m_Verbose = false;


	// We implement the parsing as a state machine.
	private enum m_enumStates {Start, TARGET, QANNON, Q};

	// Track current state
	private m_enumStates m_CurrentState;


	// Used to progressive build up a DataItem to pass to intermediary	
	private DataItem m_DataItem_TARGET;
	private DataItem m_DataItem_QUES;
	// Temporarily hold characters as they come in bit by bit from the characters() call-back.
	private String m_CurrentTextBuffer;
	


	/**
	 * Constructor.
	 */
	public TREC2007AnnotatedQuestionXMLParser() {

		// Initialization
		m_CurrentState = m_enumStates.Start;		
		m_DataItem_TARGET = null;
		m_DataItem_QUES = null;

		m_Receipients = new Vector<IXMLDataReceipient>();
	} // end consturctor


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


		
		// Start state
		if (m_CurrentState == m_enumStates.Start) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("target") == 0) {

				// State transition
				m_CurrentState = m_enumStates.TARGET;
				l_GoodTransition = true;

				// Extract attributes and create new DataItem describing this XML instance
				if (attributes != null) {
					// Target ID
					String l_QID = attributes.getValue("id");
					// Name of Target
					String l_Text = attributes.getValue("type");
					if (m_Verbose && m_DataItem_TARGET != null) {
						Logger.getLogger("QANUS").logp(Level.WARNING, TREC2007AnnotatedQuestionXMLParser.class.getName(), "startElement", "Possible overwrite of incomplete TARGET DataItem.");
					}
					m_DataItem_TARGET = new DataItem("l_QID");
					m_DataItem_TARGET.AddAttribute("id", l_QID);
					m_DataItem_TARGET.AddAttribute("type", l_Text);

				}

			} // end if (qName.com....
		} // Start state
		 
		
		// TARGET state		
		else if (m_CurrentState == m_enumStates.TARGET) {

			// 2 valid out transitions - Q and Q-XXXX
			
			if (qName.compareToIgnoreCase("q") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Q;
				l_GoodTransition = true;			

			} else if (qName.substring(0,2).compareToIgnoreCase("q-") == 0) {

				// State transition
				m_CurrentState = m_enumStates.QANNON;
				l_GoodTransition = true;

			} // end if (qName.com....

			// Same set of action for
			if (l_GoodTransition) {

			// Extract attributes and create new DataItem describing this XML instance
				if (attributes != null) {

					// Question ID
					String l_QID = attributes.getValue("id");
					// Type of question, FACTOID, LIST, or OTHERS
					String l_Type = attributes.getValue("type"); // can be null

					if (m_DataItem_TARGET != null) {

						if (m_DataItem_TARGET.ContainsField(l_QID)) {
							m_DataItem_QUES = (m_DataItem_TARGET.GetFieldValues(l_QID))[0]; // Should only be 1 such structure
							if (m_DataItem_QUES.GetAttribute("id") == null) {
								m_DataItem_QUES.AddAttribute("id", l_QID);
							}
							if (l_Type != null && m_DataItem_QUES.GetAttribute("type") == null) {
								m_DataItem_QUES.AddAttribute("type", l_Type);
							}
						} else {
							m_DataItem_QUES = new DataItem("QUES");			
							m_DataItem_QUES.AddAttribute("id", l_QID);							
							if (l_Type != null) {
								m_DataItem_QUES.AddAttribute("type", l_Type);
							}
							m_DataItem_QUES.AddAttribute("TagName", qName);
							// Store info about the target in this question tag also for later user
							if (m_DataItem_TARGET.GetAttribute("type") != null) {
								m_DataItem_QUES.AddAttribute("Target", m_DataItem_TARGET.GetAttribute("type"));
							}
							m_DataItem_TARGET.AddField(l_QID, m_DataItem_QUES);
						}
						m_CurrentTextBuffer = "";						
					}

				}

			}


		} // TARGET state

		

		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, TREC2007AnnotatedQuestionXMLParser.class.getName(), "startElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			System.out.println("TREC2007AnnotatedQuestionXMLParser::startElement() ->> [" + m_CurrentState + "]");
			Logger.getLogger("QANUS").logp(Level.FINER, TREC2007AnnotatedQuestionXMLParser.class.getName(), "startElement", "[" + m_CurrentState + "]");
		}

	} // end startElement()




	@Override
	public void endElement( String uri, String localName, String qName ) throws SAXException {

		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;
	

		// TARGET state
		if (m_CurrentState == m_enumStates.TARGET) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("target") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Start;
				l_GoodTransition = true;

				// Notify XML receipient, one question at a time
				// remember that m_DataItem_TARGET contains several questions
				String[] l_QIDs = m_DataItem_TARGET.GetAllFieldNames();
				for (String l_QID : l_QIDs) {
					DataItem l_DataItem = (m_DataItem_TARGET.GetFieldValues(l_QID))[0]; // Only should have 1
					for (IXMLDataReceipient l_Receipients : m_Receipients) {
						l_Receipients.Notify(l_DataItem);
					} // end for IXMLData..
				} // end for DataItem...

				m_DataItem_TARGET = null;				

			} // end if (qName.com....
		} // TARGET State


		// Q State
		else if (m_CurrentState == m_enumStates.Q) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("q") == 0) {

				// State transition
				m_CurrentState = m_enumStates.TARGET;
				l_GoodTransition = true;

				// Add the text we have buffered to the DataItem
				// We need to find the correct structure to add to, differentiated by
				// the question ID
				if (m_DataItem_QUES == null) return; // This shouldn't be the case, should we throw a warning?
				if (m_CurrentTextBuffer.length() <= 0) return; // Nothing to add, should be a problem also?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();

				DataItem l_NewQuestion = new DataItem(qName);
				l_NewQuestion.AddValue(m_CurrentTextBuffer);

				m_DataItem_QUES.AddField(qName, l_NewQuestion);

				m_DataItem_QUES = null;

			} // end if (qName.com....
		} // Q State


		// QANNON State
		else if (m_CurrentState == m_enumStates.QANNON) {

			// Only valid out-transition from start state
			if (qName.substring(0,2).compareToIgnoreCase("q-") == 0) {

				// State transition
				m_CurrentState = m_enumStates.TARGET;
				l_GoodTransition = true;

				// Add the text we have buffered to the DataItem
				// We need to find the correct structure to add to, differentiated by
				// the question ID
				if (m_DataItem_QUES == null) return; // This shouldn't be the case, should we throw a warning?
				if (m_CurrentTextBuffer.length() <= 0) return; // Nothing to add, should be a problem also?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();

				DataItem l_NewQuestion = new DataItem(qName);
				l_NewQuestion.AddValue(m_CurrentTextBuffer);

				m_DataItem_QUES.AddField(qName, l_NewQuestion);

				m_DataItem_QUES = null;

			} // end if (qName.com....

		} // QANNON State



		if (m_Verbose && !l_GoodTransition) {			
			Logger.getLogger("QANUS").logp(Level.WARNING, TREC2007AnnotatedQuestionXMLParser.class.getName(), "endElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {			
			Logger.getLogger("QANUS").logp(Level.FINER, TREC2007AnnotatedQuestionXMLParser.class.getName(), "endElement", "[" + m_CurrentState + "]");
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
		if (m_CurrentState == m_enumStates.Q || m_CurrentState == m_enumStates.QANNON) {
			m_CurrentTextBuffer += l_Text;
		}

	}

} // end class TREC2007AnnotatedQuestionXMLParser
