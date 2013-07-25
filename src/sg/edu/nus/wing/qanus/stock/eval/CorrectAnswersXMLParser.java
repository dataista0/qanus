package sg.edu.nus.wing.qanus.stock.eval;



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
 * XML parser for XML file containing "correct" answers.
 * The XML file has the following format

   <DOCSTREAM>
	<AnswerItem>
		<QID>216.1</QID>
		<Answer>new york times</Answer>
	</AnswerItem>
    ....
    </DOCSTREAM>

 * 
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 */
public class CorrectAnswersXMLParser extends DefaultHandler implements IXMLParser {


	// Modules which want to receive notification of parsed questions
	private Vector<IXMLDataReceipient> m_Receipients;

	// Controls whether or not to spill out status messages, such as error message and progress messages
	private boolean m_Verbose = false;

		// We implement the parsing as a state machine.
	private enum m_enumStates {Start, AnswerItem, QID, Answer};

	// Track current state
	private m_enumStates m_CurrentState;

	// Build up XML item
	private DataItem m_DataItem_AnsItem;
	private DataItem m_DataItem_AnsItems;
	// Temporarily hold characters as they come in bit by bit from the characters() call-back.
	private String m_CurrentTextBuffer;


	/**
	 * Constructor.
	 */
	public CorrectAnswersXMLParser() {

		// Initialization
		m_Receipients = new Vector<IXMLDataReceipient>();

		m_CurrentState = m_enumStates.Start;

		m_DataItem_AnsItems = new DataItem("CorrectAnswers");
		m_DataItem_AnsItem = null;
		m_CurrentTextBuffer = null;

	} // end consturctor


	// TODO handlers override
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {

		// Models state transitions, see documentation for state machine details.

		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;


		
		if (m_CurrentState == m_enumStates.Start) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("AnswerItem") == 0) {

				// State transition
				m_CurrentState = m_enumStates.AnswerItem;
				l_GoodTransition = true;

				m_DataItem_AnsItem = new DataItem("AnsItem");
				m_CurrentTextBuffer = "";

				m_DataItem_AnsItems.AddField("Answer", m_DataItem_AnsItem);

			} // end if (qName.com....
		}

		else if (m_CurrentState == m_enumStates.AnswerItem) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("qid") == 0) {

				// State transition
				m_CurrentState = m_enumStates.QID;
				l_GoodTransition = true;
				
				m_CurrentTextBuffer = "";

			}
			else if (qName.compareToIgnoreCase("answer") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Answer;
				l_GoodTransition = true;

				m_CurrentTextBuffer = "";

			} // end if (qName
			
		}


		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, CorrectAnswersXMLParser.class.getName(), "startElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, CorrectAnswersXMLParser.class.getName(), "startElement", "[" + m_CurrentState + "]");
		}

	}


	@Override
	public void endElement( String uri, String localName, String qName ) throws SAXException {

		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;


		// End of TARGET tag
		if (m_CurrentState == m_enumStates.Answer) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("answer") == 0) {

				// State transition
				m_CurrentState = m_enumStates.AnswerItem;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_AnsItem == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_DataItem_AnsItem.AddValue(m_CurrentTextBuffer);

			} // end if (qName.com....

		}

		else if (m_CurrentState == m_enumStates.QID) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("qid") == 0) {

				// State transition
				m_CurrentState = m_enumStates.AnswerItem;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_AnsItem == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_DataItem_AnsItem.AddAttribute("QID", m_CurrentTextBuffer);

			} // end if (qName.com....

		}

		else if (m_CurrentState == m_enumStates.AnswerItem) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("AnswerItem") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Start;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_AnsItem == null) return; // This shouldn't be the case, should we throw a warning?
								
				m_DataItem_AnsItem = null;

			} // end if (qName.com....

		}

		else if (m_CurrentState == m_enumStates.Start) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("docstream") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Start;
				l_GoodTransition = true;

				
				// Notify intermediary, and perform housekeeping to get ready for next basic unit of info
				for (IXMLDataReceipient l_Receipients : m_Receipients) {
					l_Receipients.Notify(m_DataItem_AnsItems);
				} // end for

				m_DataItem_AnsItems = null;

			} // end if (qName.com....

		}



		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, CorrectAnswersXMLParser.class.getName(), "endElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, CorrectAnswersXMLParser.class.getName(), "endElement", "[" + m_CurrentState + "]");
		}

	}



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
		if (m_CurrentState == m_enumStates.QID || m_CurrentState == m_enumStates.Answer) {
			m_CurrentTextBuffer += l_Text;
		}

	}



	public void RegisterForNotification(IXMLDataReceipient a_Receipient) {

		// Check for duplicates - we only register each recipient once
		for (IXMLDataReceipient l_Receipient : m_Receipients) {
			if (l_Receipient.GetIdentifier().compareTo(a_Receipient.GetIdentifier()) == 0) {
				// Already present, we skip it
				return;
			}
		} // end for

		m_Receipients.add(a_Receipient);

	}

	public void DelistFromNotification(IXMLDataReceipient a_Receipient) {

		// Check for duplicates - we only register each recipient once
		for (int i = 0; i < m_Receipients.size(); ++i) {
			if (m_Receipients.get(i).GetIdentifier().compareTo(a_Receipient.GetIdentifier()) == 0) {
				// Found, delete it
				m_Receipients.removeElementAt(i);
			}
		} // end for

		return;

	}



} // end class CorrectAnswersXMLParser
