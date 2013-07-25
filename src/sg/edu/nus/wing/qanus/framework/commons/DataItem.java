package sg.edu.nus.wing.qanus.framework.commons;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * Houses a unit of data item parsed by the XML handler.
 * Typically in a corpus, we can identify a smallest logical unit of information. For example
 *  in the AQUAINT2 corpus, this is a <DOC>. 
 * The DOC item is extracted by the XML handler, packaged as this DataItem class, and passed
 *  to the ITextModuleIntermediary. The use of this class makes it easy for generic code to be
 *  written with ITextModuleIntermediary.
 *  
 * The structure of this class corresponds to the XML structure. Some terminology :
 * 
 * <DOC id="1" type="a">
 *   <HEADLINE>Testing</HEADLINE>
 *   <TEXT>
 *      <P>Apple</P>
 *      <P>Banana</P>
 *    </TEXT>
 * </DOC>
 * 
 * - Each tag (ie. DOC, HEADLINE, TEXT) will be made into a DataItem object each.
 * - Attributes of DOC are id and type. HEADLINE and TEXT have no attributes.
 * - HEADLINE and TEXT are fields within DOC, P is a tag within TEXT
 * - Testing is the value of HEADLINE. Apple is the value of TEXT. We call this inner text.
 * - Typically, within a tag (TEXT for example), it can contain several other tags (see P).
 *   We call P a sub-field of TEXT.
 *
 * To initialize the XML instance above, we can do the following calls :
 * 
 * DataItem l_Main = new DataItem(DOC);
 *   l_Main.AddAttributes("id","1");
 *   l_Main.AddAttributes("type","a");
 * l_Main.AddField("HEADLINE", "Testing"); // HEADLINE is a field of DOC
 * l_Main.AddField("TEXT", ""); // TEXT is a field of DOC, but has no inner text
 * -------------------------
 * l_Main.AddSubFieldValue("TEXT","P","Apple"); // Alternative 1 - 
 * l_Main.AddSubFieldValue("TEXT","P","Banana");// To add P as field of TEXT (ie. sub-field of DOC)
 * -------------------------
 * DataItem l_P1 = new DataItem("P");           // Alternative 2 -
 *    P1.AddValue("Apple"); 					// Very long way, don't bother with this.
 * DataItem l_P2 = new DataItem("P");
 *    P2.AddValue("Banana");
 * // Assuming l_TextField is pointer to DataItem of TEXT in l_Main
 * l_TextField.AddField("P", l_P1); 
 * l_TextField.AddField("P", l_P2);   
 *
 *
 * Of course you are free to define your own structure for DataItem
 * 
 * @author Ng, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class DataItem {

	
	// Holds the attributes
	private Map<String, String> m_Attributes;
	
	// Holds child tags and their values
	private Map<String, LinkedList<DataItem>> m_Fields;
	
	// Holds inner text within XML tags
	private LinkedList<String> m_Value;
	
	// The XML tag associated with this data item
	private String m_ItemTag;
	
	
	/**
	 * Constructor.
	 */
	public DataItem(String a_ItemTag) {
		
		m_Fields = new HashMap<String, LinkedList<DataItem>>();
		m_Attributes = new HashMap<String, String>();
		m_Value = new LinkedList<String>();
		
		m_ItemTag = a_ItemTag;
	}
	
	
	
	/**
	 * Adds an attribute to this data item.
	 *  
	 * An attribute with name ID is equivalent to "document-id-1" in the DOC tag below :
	 * 
	 * <DOC ID="document-id-1"> ... </DOC>
	 * 
	 * If an existing attribute shares the name of the attribute we are adding, the new value is 
	 * appended to the existing attribute. This behavior is chosen because the characters() callback
	 * in the SAX handler may not always return the complete set of inner text within XML tags
	 * due to a limited buffer size. So we allow multiple calls to append values together.
	 * 
	 * @param a_Name [in] the name of the attribute
	 * @param a_Value [in] the value to associate to this attribute
	 */
	public void AddAttribute(String a_Name, String a_Value) {
		if (m_Attributes.containsKey(a_Name)) {
			String l_ExistingValue = (String) m_Attributes.get(a_Name);
			m_Attributes.put(a_Name, l_ExistingValue + a_Value);
		} else {
			m_Attributes.put(a_Name, a_Value);
		}
	} // end AddAttribute()
	
	
	/**
	 * Retrieves the value stored for a particular attribute.
	 * @param a_Name [in] name of the attribute whose value we want to retrieve
	 * @return string value of desired attribute, or null if attribute does not exist
	 */
	public String GetAttribute(String a_Name) {
		if (m_Attributes.containsKey(a_Name)) {
			String l_ExistingValue = (String) m_Attributes.get(a_Name);
			return l_ExistingValue;
		} else {
			return null;
		}
	} // end GetAttribute()
	
	
	/**
	 * Adds a new field to this data item.
	 * 
	 * For example, a field with name "DATELINE" is equivalent to a nested tag in the example below : 
	 * 
	 * <DOC>
	 *   <DATELINE>...</DATELINE>
	 * </DOC>
	 * 
	 * Note that adding a field with a name that already exist does not overwrite the previous value, rather
	 * both fields will exist side by side of each other.
	 * 
	 * @param a_Name [in] name of the field to add
	 * @param a_Value [in] value of the field to add, which in this case is another DataItem
	 */
	public void AddField(String a_Name, DataItem a_Value) {					
		
		if (m_Fields.containsKey(a_Name)) {
			// Field of same name already exist -> We add another entry beside it.
			LinkedList<DataItem> l_ExistingValue = (LinkedList<DataItem>) m_Fields.get(a_Name);
			if (l_ExistingValue != null && a_Value != null)
				l_ExistingValue.add(a_Value);			
		} else {
			// Add a new entry to the fields list
			LinkedList<DataItem> l_NewList = new LinkedList<DataItem>();
				if (a_Value != null) l_NewList.add(a_Value);
			m_Fields.put(a_Name, l_NewList);
		}		
	} // end AddField()
	
	
	
	/**
	 * Overloaded AddField(), which makes it convenient to add a field consisting only of a single string 
	 * as its value.
	 * 
	 * The new field will be created with no attributes. Similar to AddField(String, DataItem), we do not
	 * need to overwrite an existing field of the same name if it exists.
	 * 
	 * @param a_Name [in] name of the field to add
	 * @param a_Value [in] value of the field to add, which is a string
	 */
	public void AddField(String a_Name, String a_Value) {
					
		DataItem l_NewItem = new DataItem(a_Name);
			if (a_Value != null && a_Value.length() > 0)
				l_NewItem.AddValue(a_Value);
					
		AddField(a_Name, l_NewItem);
		
	} // end AddField()
	
	
	
	/**
	 * Adds inner text to a sub-field of a field of this data item.
	 * Recall that :
	 * 
	 *  <DOC>
	 *     <TEXT> <P> APPLE </P> </TEXT>
	 *     
	 * TEXT is a field of DOC, P is a field of TEXT
	 * P is a sub-field of DOC
	 * APPLE is the inner text of P.
	 * 
	 * So this method allows us a quick way to add "APPLE" to the DataItem describing DOC.
	 * ie.
	 *   AddSubFieldValue("TEXT","P","APPLE");
	 * 
	 * This is for convenience, as otherwise the caller has to maintain a copy of the DataItem for a 
	 * field to access its sub-fields. With this method, it just has to send in the inner text, and we will 
	 * append it to the existing field's sub-field.
	 * 
	 * If a sub-field already exists of the same sub-field name, a new sub-field of the same tag is created
	 *  alongside it.
	 * 
	 * @param a_Name [in] the field to add to
	 * @param a_SubFieldName [in] the sub-field name to add
	 * @param a_Value [in] the inner text to add to the field.
	 */
	public void AddSubFieldValue(String a_Name, String a_SubFieldName, String a_Value) {
		
		LinkedList<DataItem> l_ListOfDataItems = null;
		
		// Locate the field first, create it if not present
		if (m_Fields.containsKey(a_Name)) {		
			 l_ListOfDataItems = (LinkedList<DataItem>) m_Fields.get(a_Name);
		} else {
			l_ListOfDataItems = new LinkedList<DataItem>();
			m_Fields.put(a_Name, l_ListOfDataItems);
		}
		
		// Locate the DataItem to insert the sub-field into. If it doesn't exist, create it.
		// If there are multiple DataItems of the same name, we choose the first one
		DataItem l_Field = null;
		try {
			l_Field = l_ListOfDataItems.getFirst();
		} catch (NoSuchElementException e) {		
			l_Field = new DataItem(a_Name);				
			l_ListOfDataItems.add(l_Field);
		}
			
		DataItem l_NewItem = new DataItem(a_SubFieldName);
			l_NewItem.AddValue(a_Value);
			
		l_Field.AddField(a_SubFieldName, l_NewItem);			
					
		
	} // end AddToSubField()
	
	
		
	@Override
	public String toString() {
		
		String l_OutString = "-------------------------------\n";
		
		l_OutString += "Attributes : \n";
		l_OutString += m_Attributes.toString();
		
		l_OutString += "\n\nFields : \n";
		l_OutString += m_Fields.toString();
		
		l_OutString += "\n-------------------------------\n";
		
		return l_OutString;
		
	} // end toString()
	
	
	
	/**
	 * Adds to this data item inner text, defined by text enclosed within a XML tag.
	 * Repeated adds will all be stored seaprately.
	 * @param a_Value [in] the inner text to add
	 */
	public void AddValue(String a_Value) {		
		m_Value.add(a_Value);		
	} // end AddValue()
	
	
	/**
	 * Retrieve an XML string describing the data item
	 * 
	 * @return an XML string describing the data item.
	 */
	public String toXMLString() {
	
		
		// Form opening tag with attributes
		String l_OutString = "\n<" + m_ItemTag;
		for (String l_Attribute : m_Attributes.keySet()) {
			l_OutString += " " + l_Attribute + "=\"" + (String) m_Attributes.get(l_Attribute) + "\"";
		}
		l_OutString += ">";
		
		
		// Sub tags
		for (String l_Field : m_Fields.keySet()) {
			
			LinkedList<DataItem> l_ListOfItems = m_Fields.get(l_Field);
			if (l_ListOfItems == null) continue;
					
			for (DataItem l_Item : l_ListOfItems) {
				l_OutString += l_Item.toXMLString();
			}			
					
		}	
		
		
		// Inner text
		for (String l_Value : m_Value) {
			
			// Filter away special characters first
			l_Value = l_Value.replaceAll("&", "&amp;");
			l_Value = l_Value.replaceAll("<", "&lt;");
			l_Value = l_Value.replaceAll(">", "&gt;");
			l_Value = l_Value.replaceAll("\"", "&quot;");
			l_Value = l_Value.replaceAll("'", "&apos;");
			
			// Add it to the string we are building
			l_OutString += l_Value ;
			
		} // end for
		
		// Close the tag
		l_OutString += "</" + m_ItemTag + ">\n";
		
		
		return l_OutString;
		
	} // end toXMLString()
	

	/**
	 * Retrieves the value of the required field
	 * @param a_FieldName [in] name of the desired field
	 * @return array of DataItem containing value of the stated field, could be null if field is empty.
	 */
	public DataItem[] GetFieldValues(String a_FieldName) {
		
		LinkedList<DataItem> l_ListOfItems = m_Fields.get(a_FieldName);
		if (l_ListOfItems == null) return null;
				
		return l_ListOfItems.toArray(new DataItem[0]); 
						
	} // end GetFieldValue()

	
	/**
	 * Retrieves the inner text stored with this data item.
	 * @return array of strings holding the inner text.
	 */
	public String[] GetValue() {		
		return m_Value.toArray(new String[0]);
	}


	/**
	 * Retrieves the tag associated with this DataItem
	 * @return String containing XML tag of this DataItem
	 */
	public String GetXMLTag() {
		return m_ItemTag;
	} // end GetXMLTag()



	/**
	 * Check whether this structure contains a field with the given name
	 * @param a_Name [in] the name of the field we want to look up
	 * @return true if a field with the given name exists, false otherwise
	 */
	public boolean ContainsField(String a_Name) {
		return m_Fields.containsKey(a_Name);
	} // end ContainsField


	/**
	 * Retrieve names of all stored fields
	 * @return array of names of all fields stored with this structure. null returned on errors. The array can be empty.
	 */
	public String[] GetAllFieldNames() {

		LinkedList<String> l_ResultNames = new LinkedList<String>();
		Set<String> l_SetOfNames = m_Fields.keySet();
		if (l_SetOfNames != null) {
		for (String l_Name : l_SetOfNames) {
			l_ResultNames.add(l_Name);
		}
		}

		return l_ResultNames.toArray(new String[0]);

	} // end GetAllFieldNames()
	
	
} // end class
