package sg.edu.nus.wing.qanus.stock.ar.er;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import sg.edu.nus.wing.qanus.framework.ar.er.ErrorAnalysisInfoSourceReader;



/**
 * Reads in a provided file which contains informantion needed for error analysis.
 * Typically this will be a set of correct answers for posed questions so that during the
 * analysis we can know whether questions can be correctly answered.
 * But it's up to you based on what you need to achieve to use whatever you prefer.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version Feb 1, 2010
 */
public class AnalysisInfoSourceReader extends ErrorAnalysisInfoSourceReader {


	private Document m_Doc;


	/**
	 * Constructor
	 *
	 * @param a_Source [in] file containing source information
	 */
	public AnalysisInfoSourceReader(File a_Source) {

		super(a_Source);


		m_Doc = null;
		
		try {
			DocumentBuilderFactory l_DBF = DocumentBuilderFactory.newInstance();
			DocumentBuilder l_DB = l_DBF.newDocumentBuilder();
			m_Doc = l_DB.parse(a_Source);
		} catch (ParserConfigurationException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, AnalysisInfoSourceReader.class.getName(), "AnalysisInfoSourceReader", "Parser config error", ex);
			m_Doc = null;
		} catch (SAXException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, AnalysisInfoSourceReader.class.getName(), "AnalysisInfoSourceReader", "SAX Exception", ex);
			m_Doc = null;
		} catch (IOException ex) {
			Logger.getLogger("QANUS").logp(Level.WARNING, AnalysisInfoSourceReader.class.getName(), "AnalysisInfoSourceReader", "Unable to access source file", ex);
			m_Doc = null;
		} // end try-catch

	} // end constructor


	/**
	 * Retrieve the set of correct answers for a given question from the info source.
	 *
	 * @param a_QID [in] the ID of the desired question
	 * @return array of strings of the correct answers, or null on errors.
	 */
	public String[] GetCorrectAnswersForQID(String a_QID) {

		// Start from the root element
		Element l_Root = m_Doc.getDocumentElement();

		// Retrieve the various nodes
		NodeList l_NodeList = l_Root.getElementsByTagName("AnswerItem");
		if (l_NodeList != null && l_NodeList.getLength() > 0) {

			LinkedList<String> l_Results = new LinkedList<String>();
			for (int i = 0; i < l_NodeList.getLength(); ++i) {

				Element l_AnswerItemElement = (Element) l_NodeList.item(i);
				NodeList l_QIDNodeList = (NodeList) l_AnswerItemElement.getElementsByTagName("QID");
				NodeList l_AnswerNodeList = (NodeList) l_AnswerItemElement.getElementsByTagName("Answer");
				if (l_QIDNodeList != null && l_QIDNodeList.getLength() > 0
						&& l_AnswerNodeList != null && l_AnswerNodeList.getLength() > 0) {					
					if (((Element)l_QIDNodeList.item(0)).getFirstChild().getNodeValue().compareToIgnoreCase(a_QID) == 0) {
						l_Results.add(((Element)l_AnswerNodeList.item(0)).getFirstChild().getNodeValue());
					}
				}
			} // end for

			if (l_Results.size() > 0) {
				return l_Results.toArray(new String[0]);
			} else {
				return null;
			}

		} else {
			return null;
		} // end if


	} // end GetCorrectAnswersForQID()



} // end class AnalysisInfoSourceReader

