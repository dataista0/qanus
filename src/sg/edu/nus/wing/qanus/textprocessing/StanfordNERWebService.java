package sg.edu.nus.wing.qanus.textprocessing;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.ServiceFactory;

import sg.edu.nus.wing.qanus.framework.commons.ITextProcessingModule;


/**
 * Alternative way to employ named entity recognition.
 * The underlying engine used is similar to that in StanfordNER.java -- We are
 * using Stanford's NER. (http://nlp.stanford.edu/)
 * 
 * In some cases, it may be beneficial to invoke the NER engine via a web m_Service.
 * For example in the QANUS web demo, if we were to invoke the NER engine each time, it
 * will take considerable time for the engine to start. This increases the response time
 * of the system. In this scenario, we have the NER engine up and running already as a 
 * web m_Service.
 * 
 * This class is used to interact with the NER web service so as to avoid the lengthy
 * start up time.
 *
 * The NER web service is hosted on WING (http://wing.comp.nus.edu.sg).
 * The WSDL describing the web service can be found on
 * http://wing.comp.nus.edu.sg/~forecite/forecite.wsdl
 *
 * 
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 */
public class StanfordNERWebService implements ITextProcessingModule {

	// Hard coded values to connect to web service @ WING
	private String m_NameSpace = "urn:WING.NUS";
	private String m_ServiceName = "WING.NUS";
	private String m_WSDLURL = "http://wing.comp.nus.edu.sg/~wing.nus/wing.nus.wsdl";
	private String m_OperationName = "tag_sentence_ner";


	private ServiceFactory m_ServiceFactory = null;
	private Service m_Service = null;
	private Call m_Call = null;


	/*  Commented out typically - this is to test the web service on its own
	public static void main(String args[]) {
		StanfordNERWebService l_Engine = new StanfordNERWebService();
		System.out.println(l_Engine.TagSentence("Barack Obama is the president."));
	}
	*/


	/**
	 * Constructor.
	 */
	public StanfordNERWebService() {
		
		// Create the JAX-RPC Service to interact with the web m_Service						
		try {

			// Get a handle to the service
			QName l_Service_QN = new QName(m_NameSpace, m_ServiceName);
			m_ServiceFactory = ServiceFactory.newInstance();
			m_Service = m_ServiceFactory.createService(new URL(m_WSDLURL), l_Service_QN);

			// Set up the call
			m_Call = m_Service.createCall(new QName(m_NameSpace, "WING.NUSPort"), new QName(m_NameSpace, m_OperationName));
			m_Call.setReturnType(new QName(m_NameSpace,"tag_sentence_ner_response"), String.class);
			
		} catch (ServiceException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Unable to set up service.", ex);
		} catch (MalformedURLException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Bad URL for WSDL file", ex);
		} // end try-catch

	} // end constructor


	/**
	 * Invokes the web service and requests for it to tag the given sentence.
	 * @param a_Sentence [in] sentence to perform NER on
	 * @return sentence tagged with NER tags, or an empty string on any errors
	 */
	private String TagSentence(String a_Sentence) {

		String l_Result = "";

		
		try {

			// Invoke the call
			Object[] l_Parameters = new Object[] { a_Sentence };			
			l_Result = (String) m_Call.invoke(l_Parameters);

			Logger.getLogger("QANUS").log(Level.FINER, "Web service result [" + l_Result + "]");

		} catch (RemoteException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Error making call to tag [" + a_Sentence + "]", ex);
			l_Result = "";
		}
		

		return l_Result;

	} // end TagSentence()
	

	public String[] ProcessText(String[] a_Sentences) {

		if (a_Sentences == null) {
			return null;
		}

		
		LinkedList<String> l_ParsedSentences = new LinkedList<String>();

		// Parse each sentence
		for (String l_Sentence : a_Sentences) {

			try {
				String l_Result = TagSentence(l_Sentence);
				l_ParsedSentences.add(l_Result);
			} catch (Exception e) {
				Logger.getLogger("QANUS").log(Level.WARNING, "Unable to perform NER on sentence [" + l_Sentence + "]", e);
			}

		}

		// Return parsed sentences
		return l_ParsedSentences.toArray(new String[0]);
	}

	public String GetModuleID() {
		return "NER-Web";
	}

	

} // end class
