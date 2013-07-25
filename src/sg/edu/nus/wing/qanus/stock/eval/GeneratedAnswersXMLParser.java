
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
 * XML parser for XML file containing generated answers.
 * The XML file has the following format

   <DOCSTREAM>
		<Result QID="216.5">business,</Result>
		<Result QID="216.3">Econ</Result>
    </DOCSTREAM>

 *
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 */
public class GeneratedAnswersXMLParser extends DefaultHandler implements IXMLParser {

	// Modules which want to receive notification of parsed questions
	private Vector<IXMLDataReceipient> m_Receipients;

	// Controls whether or not to spill out status messages, such as error message and progress messages
	private boolean m_Verbose = false;

		// We implement the parsing as a state machine.
	private enum m_enumStates {Start, RESULT, QUESTION_TYPE, ANSWER, ANSWER_STRING};

	// Track current state
	private m_enumStates m_CurrentState;

	// Build up XML items
	private DataItem m_DataItem_Result;
	private DataItem m_DataItem_Results;
	// Temporarily hold characters as they come in bit by bit from the characters() call-back.
	private String m_CurrentTextBuffer;


	/**
	 * Constructor.
	 */
	public GeneratedAnswersXMLParser() {

		// Initialization		
		m_Receipients = new Vector<IXMLDataReceipient>();

		m_CurrentState = m_enumStates.Start;

		m_DataItem_Results = new DataItem("GeneratedAnswers");
		m_DataItem_Result = null;
		m_CurrentTextBuffer = null;
		
	} // end consturctor

	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {

		// Models state transitions, see documentation for state machine details.

		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;



		// Look out for TARGET tag
		if (m_CurrentState == m_enumStates.Start) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("result") == 0) {

				// State transition
				m_CurrentState = m_enumStates.RESULT;
				l_GoodTransition = true;

				// Extract attributes and create new DataItem describing this XML instance
				if (attributes != null) {
					String l_QID = attributes.getValue("QID");

					if (m_Verbose && m_DataItem_Result != null) {
						Logger.getLogger("QANUS").logp(Level.WARNING, GeneratedAnswersXMLParser.class.getName(), "startElement", "Possible overwrite of in-complete RESULT DataItem.");
					}

					m_DataItem_Result = new DataItem("result");
					m_DataItem_Result.AddAttribute("QID", l_QID);
					m_CurrentTextBuffer = "";

					m_DataItem_Results.AddField("Answer", m_DataItem_Result);

				}

			} // end if (qName.com....
		} else
		if (m_CurrentState == m_enumStates.RESULT) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("questiontype") == 0) {

				m_CurrentTextBuffer = "";

				// State transition
				m_CurrentState = m_enumStates.QUESTION_TYPE;
				l_GoodTransition = true;
				
			} // end if (qName.com....
			else
			if (qName.compareToIgnoreCase("answer") == 0) {

				m_CurrentTextBuffer = "";

				// State transition
				m_CurrentState = m_enumStates.ANSWER;
				l_GoodTransition = true;

			} // end if (qName.com....
			else
			if (qName.compareToIgnoreCase("answerstring") == 0) {

				m_CurrentTextBuffer = "";
				
				// State transition
				m_CurrentState = m_enumStates.ANSWER_STRING;
				l_GoodTransition = true;

			} // end if (qName.com....
		}


		if (m_Verbose && !l_GoodTransition) {			
			Logger.getLogger("QANUS").logp(Level.WARNING, GeneratedAnswersXMLParser.class.getName(), "startElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, GeneratedAnswersXMLParser.class.getName(), "startElement", "[" + m_CurrentState + "]");
		}
		
	}


	@Override
	public void endElement( String uri, String localName, String qName ) throws SAXException {

		// Tracks whether there are errors in the XML file
		boolean l_GoodTransition = false;


		if (m_CurrentState == m_enumStates.ANSWER_STRING) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("answerstring") == 0) {

				// State transition
				m_CurrentState = m_enumStates.RESULT;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_Result == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_DataItem_Result.AddField("AnswerString", m_CurrentTextBuffer);

			} // end if (qName.com....

		}
		else
		if (m_CurrentState == m_enumStates.ANSWER) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("answer") == 0) {

				// State transition
				m_CurrentState = m_enumStates.RESULT;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_Result == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_DataItem_Result.AddField("Answer", m_CurrentTextBuffer);

			} // end if (qName.com....

		}
		else
		if (m_CurrentState == m_enumStates.QUESTION_TYPE) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("questiontype") == 0) {

				// State transition
				m_CurrentState = m_enumStates.RESULT;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_Result == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_DataItem_Result.AddField("QuestionType", m_CurrentTextBuffer);

			} // end if (qName.com....

		}
		else
		if (m_CurrentState == m_enumStates.RESULT) {

			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("result") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Start;
				l_GoodTransition = true;

				// Save value
				if (m_DataItem_Result == null) return; // This shouldn't be the case, should we throw a warning?								
				m_DataItem_Result = null;

			} // end if (qName.com....
		
		}
		else if (m_CurrentState == m_enumStates.Start) {

			if (qName.compareToIgnoreCase("docstream") == 0) {

				// State transition
				m_CurrentState = m_enumStates.Start;
				l_GoodTransition = true;


				// Notify intermediary, and perform housekeeping to get ready for next basic unit of info
				for (IXMLDataReceipient l_Receipients : m_Receipients) {
					l_Receipients.Notify(m_DataItem_Results);
				} // end for

				m_DataItem_Results = null;

			} // end if (qName.com...

		}


		if (m_Verbose && !l_GoodTransition) {
			Logger.getLogger("QANUS").logp(Level.WARNING, GeneratedAnswersXMLParser.class.getName(), "endElement", "Bad XML! Current state - [" + m_CurrentState + "]");
		}

		if (m_Verbose) {
			Logger.getLogger("QANUS").logp(Level.FINER, GeneratedAnswersXMLParser.class.getName(), "endElement", "[" + m_CurrentState + "]");
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
		if (m_CurrentState == m_enumStates.QUESTION_TYPE
				|| m_CurrentState == m_enumStates.ANSWER_STRING
				|| m_CurrentState == m_enumStates.ANSWER) {
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



} // end class GeneratedAnswersXMLParser
