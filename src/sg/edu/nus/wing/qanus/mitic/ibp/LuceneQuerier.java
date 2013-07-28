package sg.edu.nus.wing.qanus.mitic.ibp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Iterator;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import sg.edu.nus.wing.qanus.mitic.ar.LuceneInformationBaseQuerier;

import sg.edu.nus.wing.qanus.mitic.ar.featurescoring.*;

/**
 * Response queries from a lucene-index
 * @author julian
 * @see LuceneInformationBaseQuerier
 */
public class LuceneQuerier 
{

  /**
   *  Cantidad de resultados que devuelve una consulta
   */
  public final int RESULTS_TO_RETRIEVE = 2000000; // was 100
  public LuceneInformationBaseQuerier m_InformationBase;
  public IndexSearcher searcher;
  
  public LuceneQuerier(String folder)
  {
     File a_IBFolder = new File(folder+"/Lucene-Index"); 
     m_InformationBase = new LuceneInformationBaseQuerier(a_IBFolder, RESULTS_TO_RETRIEVE);
     searcher = m_InformationBase.m_Searcher;     
  }
  
  public LuceneQuerier(String name, boolean new_version)
  {
     File a_IBFolder = new File("lucene-indexes"+File.separator+name+"-index"); 
     m_InformationBase = new LuceneInformationBaseQuerier(a_IBFolder, RESULTS_TO_RETRIEVE);
     searcher = m_InformationBase.m_Searcher;     
  }
  
  
  public void searchIndex(String query)  
  {
     	// Retrieve documents based on the search string from the search engine
			System.out.println(query);
      ScoreDoc[] l_RetrievedDocs = (ScoreDoc[]) m_InformationBase.SearchQuery(query);

			if (l_RetrievedDocs == null || l_RetrievedDocs.length  == 0) 
      {
          System.out.println("No search queries returned.");
          return;
      }
      
      String nombreAnterior = "";
      
			for (int i = 0; i < l_RetrievedDocs.length; i++) {
      
        
				ScoreDoc l_ScoreDoc = l_RetrievedDocs[i];
				Document l_Doc = m_InformationBase.GetDoc(l_ScoreDoc.doc);
				        
        //String[] l_ArrText = l_Doc.getValues("Text");
				String nombre = getName(l_Doc.get("NAMES"));
        String affil  = l_Doc.get("AFFIL");
        
        
        if( i % 2 == 1 && nombre.compareToIgnoreCase(nombreAnterior) == 0 ) continue; //El xml tiene todo repetido
        nombreAnterior =  nombre;

      

        System.out.println((Float.toString(l_ScoreDoc.score).substring(0, 4))+" "+nombre+" - "+affil);
        //System.out.println((Float.toString(l_ScoreDoc.score))+"   "+l_Headline);
      }
      
  }
  
  public String getName(String nombre)
  {
	  return nombre;
  }
 

  
  /**
   * Ejecuta una query y retorna RESULTS_TO_RETRIEVE documentos rankeados
   * @param String query: la query a ejecutar
   * @return ScoreDoc[], lista de documentos rankeados
   * @see ScoreDoc
   * 
   */
  public ScoreDoc[] query(String query)  
  {
     	// Retrieve documents based on the search string from the search engine
			//System.out.println(query);
      ScoreDoc[] l_RetrievedDocs = (ScoreDoc[]) m_InformationBase.SearchQuery(query);
      return l_RetrievedDocs;
  }
  
  
  public Document retrieveDoc(ScoreDoc doc)
  {
      return m_InformationBase.GetDoc(doc.doc);
  
  }
  
 /**
  * Muestra todos los valores del campo field_name en el indice 
  * @param field_name String
  * @return String[]
  */
 public String[] allValues(String field_name)
 {
	Document[] documents = allDocs();
	LinkedList<String> values = new LinkedList<String>();
 	String value;
 	
 	for(int i = 0; i < documents.length; i++)
 	{
 		value = documents[i].get(field_name);
 		if( ! values.contains(value))
 			values.add(value);
 
 		
 	}
 	return values.toArray(new String[0]);
	 
 }
  
 public LinkedList<String> allValues(String field_name, boolean lk)
 {
	 	Document[] documents = allDocs();
	 	LinkedList<String> values = new LinkedList<String>();
	 	HashMap<String, Integer> map = new HashMap<String, Integer>();
 	
 	String value;
 	
 	for(int i = 0; i < documents.length; i++)
 	{
 		value = documents[i].get(field_name);
 		if( ! values.contains(value))
 		{
 			values.add(value);
 			map.put(value, 1);
 		}
 		else
 		{
 			map.put(value, map.get(value) +1);
 		}
 		
 	}
 	
 	Set set=map.entrySet();

 	Iterator it = set.iterator();
 	
 	while(it.hasNext())
 	{
     // key=value separator this by Map.Entry to get key and value
     Map.Entry m =(Map.Entry)it.next();

     // getKey is used to get key of HashMap
     String key=(String)m.getKey();

     // getValue is used to get value of key in HashMap
     value=Integer.toString((Integer)m.getValue());

     //System.out.println("Key :"+key+"  Value: "+ value);
 	}
 	
 	return values;
	 
 }
 
  
  public Document[] allDocs()
  {
     IndexReader reader = searcher.getIndexReader();
     //System.out.println(reader.maxDoc());
     LinkedList<Document> res = new LinkedList<Document>();
     
     for (int i=0; i<reader.maxDoc(); i++) 
     {
        if (reader.isDeleted(i))
          continue;
        
        try{
            Document doc = reader.document(i);
            //String docId = doc.get("DocID");
            res.add(doc);
            //System.out.println(docId+"  "+getName(doc.get("NAMES")));
        }	
        catch (Exception ex) 
        {
  			   System.out.println("Excepcion: "+ex);
        	continue;
  			}
        
    }
     
    return res.toArray(new Document[0]);
  }
  
  public int total()
  {
	  return allDocs().length;
  }
  
  public LinkedList<String> fields()
  {
  	Document[] docs = allDocs();
		
		LinkedList<String> field_names = new LinkedList<String>();
		
		for(int d = 0; d < docs.length;d++)
		{
			List<Field> fields = docs[d].getFields();
			
			for(int i = 0; i < fields.size(); i++)
			{
				Field field = fields.get(i);
				String name = field.name();
				//if(name.compareTo("CONFL") == 0 || name.compareTo("JOURL") == 0 || name.compareTo("COAULINKS") == 0 || name.compareTo("Text") == 0 )continue;
				
				if(!field_names.contains(name))field_names.add(name);
				
			}
		}
			
		return field_names;
		
  }
  
  public String[] bestRankedFields(String search_str, Document doc, boolean split_search_str, String match_field_str)
  {
  	LinkedList<String> fields = fields();
  	
  	LinkedList<String> res = new LinkedList<String>();
  	
  	LinkedList<String> matched_strs = new LinkedList<String>();
  	
  	String field, value;
  	
  	
  	String[] values;
  	
  	double[] coverages = new double[fields.size()];
  	double coverage;
  	
  	double[] proxs = new double[fields.size()];
  	double prox;
  	
  	double[] freqs = new double[fields.size()];
  	double freq;
  	
  	double[] spans = new double[fields.size()];
  	double span;
  	  	
  	
  	FeatureSearchTermCoverage featureCoverage = new FeatureSearchTermCoverage();
  	FeatureSearchTermProximity featureProximity = new FeatureSearchTermProximity();
  	FeatureSearchTermFrequency featureFrequency = new FeatureSearchTermFrequency();
  	FeatureSearchTermSpan featureSpan = new FeatureSearchTermSpan();

  	String[] feature_param = new String[1];
  	
  	String[] search_strs;
  	
  	if(!split_search_str)
  	{
  		search_strs = new String[1];
  		search_strs[0] = search_str;
  	}
  	else
  	{
  		LinkedList<String> future_search_strs = new LinkedList<String>();
  		StringTokenizer tokens = new StringTokenizer(search_str);
			while (tokens.hasMoreTokens()) future_search_strs.add(tokens.nextToken());
			search_strs = future_search_strs.toArray(new String[0]);
					
  	}
  		
  	for(int s = 0; s < search_strs.length; s++)
  	{
  		feature_param[0] = search_strs[s];
  		    	
    	for(int i = 0; i < fields.size(); i++)
    	{
    		field =fields.get(i);
    		
    		if(!(match_field_str == null || field.compareTo(match_field_str) == 0))
    		{
    			continue;
    		}
    		//System.out.println("Field: "+field);
    		coverages[i] = 0.0;
    		freqs[i] = 0.0;
    		proxs[i] = 0.0;
    		spans[i] = 0.0;
    		
    		if(doc == null)
    		{
    			values = allValues(field);	
    		}
    		else
    		{
    			
    			values = new String[1];
    			values[0] = doc.get(field);
    		}
    		
    		for(int j = 0; j < values.length; j++)
    		{
      		coverage = featureCoverage.GetScore(feature_param, values[j]);
      		freq = featureCoverage.GetScore(feature_param, values[j]);
      		prox = featureCoverage.GetScore(feature_param, values[j]);
      		span = featureCoverage.GetScore(feature_param, values[j]);
      		
      		if(coverage == 1.0)coverages[i] = 1.0;
      		if(freq == 1.0)freqs[i] = 1.0;
      		if(prox == 1.0)proxs[i] = 1.0;
      		if(span == 1.0)spans[i] = 1.0;
    		
    		}
    		
    		if(coverages[i] > 0.0 || freqs[i] > 0.0 || proxs[i] > 0.0 || spans[i] > 0.0)
    		{
    			/*System.out.println(field+"  "+(coverages[i] > 0.0? "Cov": "" )+" "+
    																		(freqs[i] > 0.0? "Frq": "" )+" "+
    																		(proxs[i] > 0.0? "Prx": "" )+" "+
    																		(spans[i] > 0.0? "Spn": "" )+" ");
    																		*/
    			
    			if(match_field_str != null && field.compareTo(match_field_str) == 0 && !matched_strs.contains(search_strs[s]))matched_strs.add(search_strs[s]);
    			if(!res.contains(field)) res.add(field);
    		}
    	}
  	}
  	
  	
  	if(res.size() > 1 )
  	{
  		if(res.contains("ALL"))
  		{
  			res.remove("ALL");
  		}
  		
  	}
  	if(res.contains("CAT") && res.contains("CATLIST")) res.remove("CATLIST");
  	if(res.contains("DOCNO") && res.contains("NAMES")) res.remove("DOCNO");
  	if(res.contains("TITLES") && res.contains("ABSTS")) res.remove("ABSTS");  	
  	
  	
  	if(match_field_str != null)
  		return matched_strs.toArray(new String[0]);
  	
  	return res.toArray(new String[0]);
  	
  }
  
  public Document getDocumentById(String id)
  {
  	Document[] allDocs = allDocs();
  	
  	for(Document doc: allDocs)
  	{
  			String docId = doc.get("DocID");
  			if(docId.compareToIgnoreCase(id) == 0)
  			{
  				return doc;
  			}
  	}
  	
  	return null;
  	
  }
  
  public void printDoc(Document doc)
  {
  	List<Field> fields = (List<Field>) doc.getFields();
  	String str = "{";
  	if(fields != null)
  	{
  		for(Field field: fields)
  		{
  			if(field.name().compareTo("ALL") == 0) continue;
  			str+=field.name().toLowerCase()+": "+field.stringValue()+", ";
  		}
  	}
  	if(str.compareTo("{")!=0)
  	{
  		str =str.substring(0, str.length() -1)+"}";
  		System.out.println(str);
  	}
  	
  	else System.out.println("null doc");
  		
  	
  	
  }
  /**
	 * Entry point function.
	 * You don't have to change this usually, so you can just copy this to make your own
	 * Controller.
	 *
	 * @param args
	 */
	public static void main(String args[]) {
  
    LuceneQuerier lq;
    lq = new LuceneQuerier("data_mitic/ibp_out");
    //Document[] res = lq.allDocs();
    //String[] affils = lq.allValues("JOURL");
    
    Scanner user_input = new Scanner( System.in );
    String input;
    Document[] all_docs = lq.allDocs();
    
    //System.out.println(all_docs[0].get("NAMES"));
    
    while(true)
    {
    	
    	System.out.print("Ingrese Terminos o Salir:... ");
    	input = user_input.nextLine( );
    	if(input.compareToIgnoreCase("Salir") == 0) break;
    	
    	String[] res = lq.bestRankedFields(input, null, false, null);
    	for(int i = 0; i < res.length; i++)
    	{
    		System.out.println(res[i]);
    	}
    	
    }
    
    /*if(args.length == 0)
    {
      lq = new LuceneQuerier("data_mitic\\ibp_out");
    }
    else
    {
    	lq = new LuceneQuerier(args[0]);
    }
    
	  if(args[1].compareToIgnoreCase("all_index") == 0)
	  {
	      lq.allDocs();
	      return;
	  }
	  
	  lq.searchIndex(args[1]);
	  //System.out.println(res);*/
	  System.out.println("Ejecute correctamente");
	  
  }



}