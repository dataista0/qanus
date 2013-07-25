package sg.edu.nus.wing.qanus.mitic.ibp;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import sg.edu.nus.wing.qanus.framework.commons.DataItem;
import sg.edu.nus.wing.qanus.framework.commons.IInformationBaseBuilder;




/**
 * Implements a Lucene based knowledge base.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version v18Jan2010
 */
public class InformationBaseWithLucene implements IInformationBaseBuilder {

	
	// Lucene components
	private IndexWriter m_LuceneIW = null;


	
	/**
	 * Constructor.
 	 * @param a_TargetFile [in] Folder where output will be stored
	 */
	public InformationBaseWithLucene(File a_TargetFile) {
		

		// Build the index writer by Lucene
		try
    {
		
    	String l_LuceneIndexFileName = a_TargetFile + File.separator + "Lucene-Index";
			m_LuceneIW = new IndexWriter(
                          					l_LuceneIndexFileName, 
                                    new StandardAnalyzer(), 
                                    true,
                          					IndexWriter.MaxFieldLength.UNLIMITED
                                  );
    
    } 
    catch (CorruptIndexException e) 
    {
			m_LuceneIW = null;
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseWithLucene.class.getName(), "Go", "Problem initializing Lucene", e);
		} 
    catch (LockObtainFailedException e)
    {
			m_LuceneIW = null;
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseWithLucene.class.getName(), "Go", "Problem initializing Lucene", e);
		} 
    catch (IOException e) 
    {
			m_LuceneIW = null;
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseWithLucene.class.getName(), "Go", "Problem initializing Lucene", e);
		}

		
	} // end constructor
		
	
	/**
	 * Given a data item for the <TEXT> XML instance, extract the different <P> sentences within it
	 * into a string array.
	 * 
	 * 
	 * @param a_FieldValues [in] list of <TEXT> XML instance data items.
	 * @return array of strings of each <P> sentence within the <TEXT> tags.
	 */
	private String[] ExtractSentencesIntoArray(DataItem[] a_FieldValues) {
		
		LinkedList<String> l_SentencesList = new LinkedList<String>();
		
		// Each data item is a TEXT XML instance.
		// Normally there should only be one, but we'd loop through anyway, just in case
		for (DataItem l_Item : a_FieldValues) 
    {
			
			DataItem[] l_SentenceValues =  l_Item.GetFieldValues("P");
			if (l_SentenceValues == null || l_SentenceValues.length == 0) continue;  // Nothing to do
			
			for (DataItem l_SentenceValue : l_SentenceValues) {
				
				String[] l_SentenceArray = l_SentenceValue.GetValue();
				if (l_SentenceArray == null || l_SentenceArray.length == 0) continue;
				
				for (String l_Sentence : l_SentenceArray) {
					l_SentencesList.add(l_Sentence);
				} // end for l_Sentence...
				
			} // end for l_SentenceValue...
			
		} // end for l_Item...
				
		
		if (l_SentencesList.size() == 0) 
			return new String[0];
		else
			return l_SentencesList.toArray(new String[0]);
		
	} // end ExtractSentencesIntoArray()



  /**
   * FunciÛn que elimina acentos y caracteres especiales de
   * una cadena de texto.
   * @param input
   * @return cadena de texto limpia de acentos y caracteres especiales.
   */
  public static String remove(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "·‡‰ÈËÎÌÏÔÛÚˆ˙˘uÒ¡¿ƒ…»ÀÕÃœ”“÷⁄Ÿ‹—Á«";
    // Cadena de caracteres ASCII que reemplazar·n los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i=0; i<original.length(); i++) {
        // Reemplazamos los caracteres especiales.
        output = output.replace(original.charAt(i), ascii.charAt(i));
    }//for i
    return output;
    
  }//remove


	/**
	 * @see IInformationBaseBuilder#AddToInfoBase
	 */
	public boolean AddToInfoBase(DataItem a_Item) 
  {

		Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseWithLucene.class.getName(), "AddToInfoBase", "Generado ");
		// Check that Lucene is properly initialized
		if (m_LuceneIW == null)
    {
			Logger.getLogger("QANUS").logp(Level.SEVERE, InformationBaseWithLucene.class.getName(), "Notify", "Lucene not initialized.");
			return false;
		}

		// ----------------------- Build Lucene Index -----------------------------
		// Extract information from the data item
		String l_Info_ID = a_Item.GetAttribute("id");
    
    
    
    Document l_LuceneDoc = new Document();
		l_LuceneDoc.add(new Field("DocID", l_Info_ID, Field.Store.YES, Field.Index.NOT_ANALYZED));
    
    String[] allFields = a_Item.GetAllFieldNames();
    String all = "";
    for(String campo : allFields)
    {
            System.out.println(campo);    
        
            DataItem[] aux = a_Item.GetFieldValues(campo);
            String val = (aux == null || aux.length == 0 || aux[0].GetValue().length == 0)?null:aux[0].GetValue()[0];
            if(val != null)
            {
            		if(!(campo.compareToIgnoreCase("COAULINKS") == 0 || campo.compareToIgnoreCase("JOURL") == 0 || campo.compareToIgnoreCase("CONFL") == 0))all+= remove(val);
                  l_LuceneDoc.add(new Field(campo, remove(val), Field.Store.YES, Field.Index.ANALYZED));
            }
            

        
    }
     l_LuceneDoc.add(new Field("ALL", all, Field.Store.YES, Field.Index.ANALYZED));
		//DataItem[] l_HeadlineValues = a_Item.GetFieldValues("DOCNO");
		//DataItem[] l_DatelineValues = a_Item.GetFieldValues("NAMES");
//		DataItem[] l_TextValues = a_Item.GetFieldValues("AFFIL");
		
    // Verify retrieve information for validity - esp critical values like DocID
//		if (l_Info_ID == null || l_TextValues == null) 
//    {
//			Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseWithLucene.class.getName(), "Notify", "Incomplete information about data item.");
//			return false;
//		}

		//String l_Info_Headline = l_HeadlineValues == null?null:l_HeadlineValues[0].GetValue()[0];
		//String l_Info_Dateline = l_DatelineValues == null?null:l_DatelineValues[0].GetValue()[0];
		
    //System.out.println("Headline:" +l_Info_Headline);
    //String[] l_Sentences = ExtractSentencesIntoArray(l_TextValues);
		//if (l_Sentences == null || l_Sentences.length == 0) {
//			Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseWithLucene.class.getName(), "Notify", "Data item has no text.");
//			return false;
//		}


		// Add to Lucene
		// 1. Build up the Lucene document
		
	//	if (l_Info_Type != null) l_LuceneDoc.add(new Field("Type", l_Info_Type, Field.Store.YES, Field.Index.NO));

		//if (l_Info_Headline != null) l_LuceneDoc.add(new Field("Headline", l_Info_Headline, Field.Store.YES, Field.Index.ANALYZED));
		//if (l_Info_Dateline != null) l_LuceneDoc.add(new Field("Dateline", l_Info_Dateline, Field.Store.YES, Field.Index.ANALYZED));

		//for (String l_Sentence : l_Sentences) 
//    {
//			l_LuceneDoc.add(new Field("Text", l_Sentence, Field.Store.YES, Field.Index.ANALYZED));
		//}

    l_LuceneDoc.add(new Field("Text", "Datos falsos", Field.Store.YES, Field.Index.ANALYZED));
		

    
    // 2. Add it to Lucene
		try 
    {
  			m_LuceneIW.addDocument(l_LuceneDoc);
    } 
    catch (CorruptIndexException e1) {Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseWithLucene.class.getName(), "Notify", "Unable to add to Lucene", e1);return false;} catch (IOException e1) {Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseWithLucene.class.getName(), "Notify", "Unable to add to Lucene", e1);return false;}
		
    
    // 3. Commit the add
		try 
    {
			m_LuceneIW.commit();
		
    }
     catch (CorruptIndexException e) {			Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseWithLucene.class.getName(), "Notify", "Unable to commit", e);			return false;		} catch (IOException e) {			Logger.getLogger("QANUS").logp(Level.WARNING, InformationBaseWithLucene.class.getName(), "Notify", "Unable to commit", e);			return false;		}



		return true;
			
	} // end AddToInfoBase()
	
} // end class InformationBaseWithLucene

