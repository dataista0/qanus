package sg.edu.nus.wing.qanus.mitic.ibp;


import java.util.Random;
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
	//  ---> S0 -----DOC----> S1 -----DOCNO-------> S2
  //                           -----DOCNO-------> S2
  //                           -----NAMES-------> S2
  //                           -----AFFIL-------> S2
  //                           -----IDS-------> S2
  //                           -----BIBLIO-------> S2
  //                           -----CAT-------> S2
  //                           -----CATLIST-------> S2
  //                           -----TITLES-------> S2
  //                           -----ABSTS-------> S2
  //                           -----CONF-------> S2 
  //                           -----CONFL-------> S2
  //                           -----JOUR-------> S2
  //                           -----JOURL-------> S2
  //                           -----COAU-------> S2
  //                           -----COAULINKS-------> S2
  //                           -----KEYW-------> S2
  //                           -----ELECTRONIC_ED-------> S2
  
  
  
	//private enum m_enumStates {Start, DOC, HEADLINE, DATELINE, TEXT, SENTENCE};
  //private enum m_enumStates {Start, DOC, DOC_FIELD};
   private enum m_enumStates {Start, DOC, DOC_FIELD, ART, ART_FIELD, PUB, PUB_FIELD};
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
		
		
		
		//System.out.println(m_CurrentState+"   "+qName);
		// StartState S0
		if (m_CurrentState == m_enumStates.Start) {
			
			// Only valid out-transition from start state
			if (qName.compareToIgnoreCase("DOC") == 0) {
				
				// State transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
        
				// Extract attributes and create new DataItem describing this XML instance
				if (true ||attributes != null) {			
        
        Random random = new Random();
				String l_DocID = attributes.getValue("id");
					//String l_Type = "falso"; //attributes.getValue("type");			
					if (l_DocID != null && l_DocID.length() > 0) {
						if (m_Verbose && m_ReadDataItem != null) {
							Logger.getLogger("QANUS").logp(Level.WARNING, AQUAINT2XMLHandler.class.getName(), "startElement", "Possible overwrite of incomplete DataItem.");
						}
						m_ReadDataItem = new DataItem("DOC");
						m_ReadDataItem.AddAttribute("id", l_DocID);
						//m_ReadDataItem.AddAttribute("type", l_Type);
					}			
				}
				
			} // end if (qName.com....			
		}
		
		// DOC se bifurca en DOC_FIELD (Campos de texto) y ART, unico campo estructurado
		else if (m_CurrentState == m_enumStates.DOC) {
			
			// Transitiones para campos simples de DOC
			if(
                  qName.compareToIgnoreCase("DOCNO") == 0 ||
                  qName.compareToIgnoreCase("NAMES") == 0 ||
                  qName.compareToIgnoreCase("AFFIL") == 0 ||
                  qName.compareToIgnoreCase("IDS") == 0 ||
                  qName.compareToIgnoreCase("BIBLIO") == 0 ||
                  qName.compareToIgnoreCase("CAT") == 0 ||
                  qName.compareToIgnoreCase("CATLIST") == 0 ||
                  qName.compareToIgnoreCase("TITLES") == 0 ||
                  qName.compareToIgnoreCase("ABSTS") == 0 ||
                  qName.compareToIgnoreCase("CONF") == 0 || 
                  qName.compareToIgnoreCase("CONFL") == 0 ||
                  qName.compareToIgnoreCase("JOUR") == 0 ||
                  qName.compareToIgnoreCase("JOURL") == 0 ||
                  qName.compareToIgnoreCase("COAU") == 0 ||
                  qName.compareToIgnoreCase("COAULINKS") == 0 ||
                  qName.compareToIgnoreCase("KEYW") == 0 ||
                  qName.compareToIgnoreCase("ELECTRONIC_ED") == 0 
                  
/*          qName.compareToIgnoreCase("DOCNO") == 0 ||
          qName.compareToIgnoreCase("NAMES") == 0 ||
          qName.compareToIgnoreCase("AFFIL") == 0 ||
          qName.compareToIgnoreCase("IDS") == 0 ||
          qName.compareToIgnoreCase("BIBLIO") == 0 ||   
          qName.compareToIgnoreCase("CATM") == 0 ||          
          qName.compareToIgnoreCase("CAT") == 0 ||
          qName.compareToIgnoreCase("COAU") == 0 ||
          qName.compareToIgnoreCase("COAULINKS") == 0*/           
        ) 
      {
				m_CurrentState = m_enumStates.DOC_FIELD;
				l_GoodTransition = true;
				m_CurrentTextBuffer = ""; // <--- buffer used to build up inner text
			}  
      else if (qName.compareToIgnoreCase("ART") == 0) 
      {
				m_CurrentState = m_enumStates.ART;
				l_GoodTransition = true;
				m_CurrentTextBuffer = ""; // <--- strictly speaking not required, since we expect the text to be within  ART_FIELD tags.
			} 
		}
		
		
		// DOC_FIELD es un campo de texto, no realiza transicion
		else if (m_CurrentState == m_enumStates.DOC_FIELD) {
			
		}	
		
		
		// ART va a ART_FIELD (Campos con texto) y a PUB (PUBLICACION, con dos campos)
		else if (m_CurrentState == m_enumStates.ART) 
    {
      if (qName.compareToIgnoreCase("TITULO") == 0 ||
          qName.compareToIgnoreCase("RESUMEN") == 0 ||
          qName.compareToIgnoreCase("KEYWORDS") == 0 ||
          qName.compareToIgnoreCase("CAT") == 0 ) {				
				
        m_CurrentState = m_enumStates.ART_FIELD; //Los campos de articulo son texto simple
				l_GoodTransition = true;				
				m_CurrentTextBuffer = ""; 
			}
      else if(qName.compareToIgnoreCase("PUBLICACION") == 0 )
      {
        m_CurrentState = m_enumStates.PUB; //Las publicaciones tienen campos (ultimo nodo central)
				l_GoodTransition = true;				
				m_CurrentTextBuffer = "";
      }
			
		} 
    else if (m_CurrentState == m_enumStates.ART_FIELD) 
    {
        //Do nothing
			
		}
    else if (m_CurrentState == m_enumStates.PUB) 
    {
       
       if(qName.compareToIgnoreCase("JOURNAL") == 0 ||
          qName.compareToIgnoreCase("PUBLINK") == 0 ) 
        {
          m_CurrentState = m_enumStates.PUB_FIELD; 
  				l_GoodTransition = true;				
  				m_CurrentTextBuffer = "";
  		  }	
		}
    else if (m_CurrentState == m_enumStates.PUB_FIELD) 
    {
        //Do nothing
			
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
				    //System.out.println("Un recipient");
          l_Receipients.Notify(m_ReadDataItem);
          	//System.out.println("Post");
				} // end for
				m_ReadDataItem = null;
			}
		}
		
		//Start, DOC, DOC_FIELD, ART, ART_FIELD, PUB, PUB_FIELD
		
    
    //DOC_FIELD -> vuelve a DOC 
		else if (m_CurrentState == m_enumStates.DOC_FIELD) {
				//Este if esta funcionando como un checkeo al pedo
        if(
                  qName.compareToIgnoreCase("DOCNO") == 0 ||
                  qName.compareToIgnoreCase("NAMES") == 0 ||
                  qName.compareToIgnoreCase("AFFIL") == 0 ||
                  qName.compareToIgnoreCase("IDS") == 0 ||
                  qName.compareToIgnoreCase("BIBLIO") == 0 ||
                  qName.compareToIgnoreCase("CAT") == 0 ||
                  qName.compareToIgnoreCase("CATLIST") == 0 ||
                  qName.compareToIgnoreCase("TITLES") == 0 ||
                  qName.compareToIgnoreCase("ABSTS") == 0 ||
                  qName.compareToIgnoreCase("CONF") == 0 || 
                  qName.compareToIgnoreCase("CONFL") == 0 ||
                  qName.compareToIgnoreCase("JOUR") == 0 ||
                  qName.compareToIgnoreCase("JOURL") == 0 ||
                  qName.compareToIgnoreCase("COAU") == 0 ||
                  qName.compareToIgnoreCase("COAULINKS") == 0 ||
                  qName.compareToIgnoreCase("KEYW") == 0 ||
                  qName.compareToIgnoreCase("ELECTRONIC_ED") == 0 
       
        /*  qName.compareToIgnoreCase("DOCNO") == 0 ||
          qName.compareToIgnoreCase("NAMES") == 0 ||
          qName.compareToIgnoreCase("AFFIL") == 0 ||
          qName.compareToIgnoreCase("IDS") == 0 ||
          qName.compareToIgnoreCase("BIBLIO") == 0 ||   
          qName.compareToIgnoreCase("CATM") == 0 ||          
          qName.compareToIgnoreCase("CAT") == 0 ||
          qName.compareToIgnoreCase("COAU") == 0 ||
          qName.compareToIgnoreCase("COAULINKS") == 0
          */           
        ) 
      {
      	
				// Update transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
				// Add the <<Campos de nombre varialbe>> field to the DataItem being built up.
				if (m_ReadDataItem == null) return; // This shouldn't be the case, should we throw a warning?\
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				m_ReadDataItem.AddField(qName, m_CurrentTextBuffer);
			}
      else
      {
          //System.out.println("I don't know what to do");
      }
		}
		else if (m_CurrentState == m_enumStates.ART) 
    {
    	 if (qName.compareToIgnoreCase("ART") == 0) {
				
				// Update transition
				m_CurrentState = m_enumStates.DOC;
				l_GoodTransition = true;
				
			}			
    
    
    }
    else if (m_CurrentState == m_enumStates.ART_FIELD) 
    {
    	if (qName.compareToIgnoreCase("TITULO") == 0 ||
          qName.compareToIgnoreCase("RESUMEN") == 0 ||
          qName.compareToIgnoreCase("KEYWORDS") == 0 ||
          qName.compareToIgnoreCase("CAT") == 0 ) {					
				// Update transition
				m_CurrentState = m_enumStates.ART;
				l_GoodTransition = true;
        
        // Add the text we have buffered to the DataItem
				if (m_ReadDataItem == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_ReadDataItem.AddSubFieldValue("ART", qName, m_CurrentTextBuffer);
			
      
				
			}			
    
    
    }
    //Por ahora publicacion y pub fields no se agregan
    else if (m_CurrentState == m_enumStates.PUB) 
    {
    	 if (qName.compareToIgnoreCase("PUBLICACION") == 0) {
				
				// Update transition
				m_CurrentState = m_enumStates.ART;
				l_GoodTransition = true;
				
			}			
    
    
    }
    else if (m_CurrentState == m_enumStates.PUB_FIELD) 
    {
    	 if (qName.compareToIgnoreCase("JOURNAL") == 0 ||
          qName.compareToIgnoreCase("PUBLINK") == 0 ) {
				
				// Update transition
				m_CurrentState = m_enumStates.PUB;
				l_GoodTransition = true;
        
        // Add the text we have buffered to the DataItem
				if (m_ReadDataItem == null) return; // This shouldn't be the case, should we throw a warning?
				m_CurrentTextBuffer = m_CurrentTextBuffer.trim();
				if (m_CurrentTextBuffer.length() > 0) m_ReadDataItem.AddSubFieldValue("ART", qName, m_CurrentTextBuffer);
				
			}			
    
    
    }
		/*
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
*/		
		
		
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
		
    //Start, DOC, DOC_FIELD, ART, ART_FIELD, PUB, PUB_FIELD
		
		if (m_CurrentState == m_enumStates.Start) {
			// Nothing to do
		}
		
		if (m_CurrentState == m_enumStates.DOC_FIELD) {			
			m_CurrentTextBuffer += l_Text;
		}
		
		if (m_CurrentState == m_enumStates.ART_FIELD) {
			m_CurrentTextBuffer += l_Text;
		}
		
		if (m_CurrentState == m_enumStates.ART) {
			// Nothing to do
		}
    
    if (m_CurrentState == m_enumStates.PUB) {
			// Nothing to do
		}
		
		if (m_CurrentState == m_enumStates.PUB_FIELD) {
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
