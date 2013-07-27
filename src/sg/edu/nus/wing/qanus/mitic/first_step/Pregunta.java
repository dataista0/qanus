package sg.edu.nus.wing.qanus.mitic.first_step;


import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.google.gson.*;

import java.io.*;

import org.apache.lucene.document.Field;



import org.apache.lucene.document.Document;

import org.apache.lucene.search.ScoreDoc;


//import org.apache.commons.httpclient.*;
//import org.apache.commons.httpclient.methods.*;
//import org.apache.commons.httpclient.params.HttpMethodParams;

import java.net.URL;
import java.net.URLEncoder;

import com.cybozu.labs.langdetect.*;

import sg.edu.nus.wing.qanus.mitic.ibp.LuceneQuerier;

/**
 * 	Representa una pregunta con algunos preprocesamientos
 */
public class Pregunta 
{
    
		public LuceneQuerier querier;
	
    
	/**La pregunta misma (input constructor)*/
	public String pregunta;
    
	/**La pregunta con POS-Tags (input constructor)*/
	public String pos;
    /**La pregunta con NER (input constructor)*/
	public String ner;

	/**String "hum:desc" (input constructor)*/
    public String qc;
    
    /**
     * Se cargan en el constructor
     * Primera parte del QC
     * */
    public QCEnumTypes qc_class;
    /**
     * Se cargan en el constructor
     * Segunda parte del QC
     * */
    public QCEnumTypes qc_subclass;
   
    public QCEnumTypes asked_entity;
    
    public Entity[] question_words;
    public Entity[] verbs;
    public Entity[] entities;    
    public String[] organizations;
    public String[] persons;
    public String[] locations;
    public String query;
    
    public String megaorganization ="";
    public String megaperson ="";
    
    
    public ScoreDoc[] score_docs;
    
    public float max_score;
    public LinkedList<Integer> scores;
    public LinkedList<Integer> distancias;

    
    public Pregunta(String in_pregunta,String  in_pos,String in_ner,String in_qc)
    {
    	try
    	{
    		Detector detector = DetectorFactory.create();
    		detector.append(in_pregunta);	
    		String lang = detector.detect();
    		System.out.println("Lenguaje detectado:"+lang);
    	}
    	catch (Exception ex)
    	{
    		System.out.println(ex);
    		return;
    	}
    	
    		
    		
    	querier = new LuceneQuerier("data_mitic/ibp_out");
    	pregunta = in_pregunta;
        pos = in_pos;
        ner = in_ner;
        qc = in_qc;  
        qc_class = QCEnumTypes.val(getQcClass(false));
        qc_subclass = QCEnumTypes.val(getQcClass(true));
        verbs = getVerbs();
        question_words = getQuestionWords();
        persons = getEntities("PERSON");
        locations = getEntities("LOCATION");
        organizations = getEntities("ORGANIZATION");
        asked_entity = getAskedEntity();
        query = getQuery();
        score_docs = getScoreDocs();
        getScoresAndDistances();
    
    }
    
    //Parte un string qc_class:qc_subclass
    public String getQcClass(Boolean get_subclass)
    {
      if(!get_subclass)
      {
          return qc.substring(0, qc.indexOf(":"));
      }
      else
      {
          return qc.substring( qc.indexOf(":")+1, qc.length());
      }
      
    }
    
    public String translate(String query)
    {
    	Gson gson = new Gson();
    	
    	try
    	{
    		URL url = new URL("http://api.mymemory.translated.net/get?q="+URLEncoder.encode(query, "UTF-8")+"&langpair=es|en");	
    		BufferedReader reader = null;

      	try {
      	    reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

      	    for (String line; (line = reader.readLine()) != null;) {
      	       return gson.fromJson(line, MyMemoryResponse.class).responseData.get("translatedText") ;
      	    }
      	} finally {
      	    if (reader != null) try { reader.close(); } catch (IOException ignore) {}
      	}
    	
    	}
    	catch (Exception ex)
    	{
    			System.out.println("Malformada la url esta");
    			return "404";
    	}
    	
    	return "404";
    	

    }
    
   	private String StripXMLChar(String a_String) 
    {

      		Pattern l_Pattern_APOSS = Pattern.compile("&apos;s");
      		Matcher l_Matcher_APOSS = l_Pattern_APOSS.matcher(a_String);
      		a_String = l_Matcher_APOSS.replaceAll("");
      
      		Pattern l_Pattern_APOS = Pattern.compile("&apos;");
      		Matcher l_Matcher_APOS = l_Pattern_APOS.matcher(a_String);
      		a_String = l_Matcher_APOS.replaceAll("'");
      
      		Pattern l_Pattern_QUOT = Pattern.compile("&quot;");
      		Matcher l_Matcher_QUOT = l_Pattern_QUOT.matcher(a_String);
      		a_String = l_Matcher_QUOT.replaceAll("");
      
      		Pattern l_Pattern_AMP = Pattern.compile("&amp;");
      		Matcher l_Matcher_AMP = l_Pattern_AMP.matcher(a_String);
      		a_String = l_Matcher_AMP.replaceAll("&");

		return a_String;

	   } // end StripXMLChar()

    
    public Entity[] getVerbs()
    {
    
		    ArrayList<Entity> res = new ArrayList<Entity>();
        
        StringTokenizer tokens = new StringTokenizer(pos);
        
				while (tokens.hasMoreTokens()) 
        {
						String word = tokens.nextToken();
            //System.out.println("Word: " + word);
						int delimPos = word.indexOf('/');
            
						if (delimPos != -1) 
            {
							try 
              {
								String posTag = word.substring(delimPos+1);
							//	System.out.println("Postag: " + posTag);
                if (posTag.substring(0,2).compareToIgnoreCase("NN") == 0) { // Match all NN tags like NNS NNP NN NNPS
									//l_Query += StripXMLChar(word.substring(0, delimPos)) + " ";
								}
								if (posTag.substring(0,2).compareToIgnoreCase("VB") == 0) { // Match all VB tags like VB VBZ VBG VBD
									//l_Query += StripXMLChar(word.substring(0, delimPos)) + " ";
                //  System.out.println("Verbo: " + posTag.substring(0,2));
                 // System.out.println("Enty: " + word.substring(0, delimPos));
                  res.add( new Entity(StripXMLChar(word.substring(0, delimPos)), posTag.substring(0,2)));
								}
							} catch (Exception ex) {
								continue;
							}
						}
            
         }
        
        
        return res.toArray(new Entity[0]);
    }
    
    public Entity[] getQuestionWords()
    {
    
		    ArrayList<Entity> res = new ArrayList<Entity>();
        
        StringTokenizer tokens = new StringTokenizer(pos);
        
				while (tokens.hasMoreTokens()) 
        {
						String word = tokens.nextToken();
            //System.out.println("Word: " + word);
						int delimPos = word.indexOf('/');
            
						if (delimPos != -1) 
            {
							try 
              {
								String posTag = word.substring(delimPos+1);
                if (posTag.substring(0,1).compareToIgnoreCase("W") == 0) {

                  res.add( new Entity(StripXMLChar(word.substring(0, delimPos)), posTag.substring(0,2)));
								}
							} catch (Exception ex) {
								continue;
							}
						}
         }

        return res.toArray(new Entity[0]);
    }
    
    public boolean wordTypeExists(String type)
    {
    
     StringTokenizer tokens = new StringTokenizer(pos);
        
		 while (tokens.hasMoreTokens()) 
     {
					String word = tokens.nextToken();
					int delimPos = word.indexOf('/');
					if (delimPos != -1) 
          {
						try 
            {
							String posTag = word.substring(delimPos+1);
              if (posTag.substring(0,2).compareToIgnoreCase("IN") == 0) return true;
						} catch (Exception ex) {
							continue;
						}
					}
      }
		 
			return false;
			
    }
    
    public String[] getEntities(String type)
    {

        ArrayList<String> res = new ArrayList<String>();
        
        StringTokenizer tokens = new StringTokenizer(ner);
        
        String lastNer = "";
        String entity = "";
        String mega_entity = "";
        
		while (tokens.hasMoreTokens()) 
        {
			String word = tokens.nextToken();
            //System.out.println("Word: " + word);
			int delimNer = word.indexOf('/');

			if (delimNer != -1) 
            {
				
			  try
              {
              
              	String nerTag = word.substring(delimNer+1);
                String theWord = StripXMLChar(word.substring(0, delimNer));
		              
                //La palabra es del tipo que queremos (por ejemplo: PERSON)
                if(nerTag.length() >= type.length() && nerTag.substring(0,type.length()).compareToIgnoreCase(type) == 0)
                {

                  if(lastNer.compareToIgnoreCase(type) == 0)//Venimos de otra palabra del tipo (PERSON...)
                  {
                     entity +=" "+theWord;
                  
                  }
                  else if(lastNer.compareToIgnoreCase("") == 0) //No venimos de nui
                  {
                      //System.out.println("Vengo de null");
                      entity = theWord;
                      lastNer = type;
                  }
                  else
                  {
                      //System.out.println("Vengo de "+ lastNer);
                      res.add( entity);
                      lastNer=type;
                      entity = theWord;
                      
                  }
									
                }
                else 
                {
                    //System.out.println("no matchee");
                    if(lastNer.compareToIgnoreCase("") != 0)
                    {
                          //System.out.println("Vengo de "+ lastNer);
                          res.add( entity);
                    }                    
                    lastNer = "";
                    entity = "";
                }
                
                
					} catch (Exception ex) {
                    System.out.println("excepcion: "+ex);
					continue;
					}
				}
            
         }
        
        if(lastNer.compareToIgnoreCase("") != 0)
        {
            res.add(entity);
        }                     
         
        return res.toArray(new String[0]);

    }
    

    
    public void imprimirAnalisis()
    {
        
        
        System.out.println("Pregunta: " + pregunta); 
        System.out.println("QC: " + qc);
        System.out.println("NER: " + ner);
        System.out.println("POS: " + pos);
        System.out.println("Entidad Preguntada: " + asked_entity.toString());
        
        for(String person: persons)
        {
            System.out.println("Persona: "+person);
        }
        
        for(String organization: organizations)
        {
            System.out.println("Organizacion: "+organization);
        }
        
        for(String location: locations)
        {
            System.out.println("Lugar: "+location);
        }
        for (Entity verb : verbs)
        {
            System.out.println("Verbo: "+verb.entity+ "  "+verb.type);    
        }
    
    }
    
    public String getQuery()
    {
      
    	String query_local = "";
    	
    	if(!(pregunta.startsWith("Cruda: ")))
      {
    		
          
    		for(String organization: organizations)
          {
              query_local += "(AFFIL:("+organization+"))^2 ";
              query_local += "(CONF:("+organization+"))^1.5 ";
              query_local += "(JOUR:("+organization+"))^1.5 ";
              query_local += "(TITLES:("+organization+"))^1.5 ";
              query_local += "(KEYW:("+organization+"))^1.5 ";
              megaorganization += organization+" ";
          }
  
          for(String person: persons)
          {
              query_local += "(NAMES:("+person+"))^2 ";
              query_local += "(COAU:("+person+"))^1.5 ";
              megaperson += person+" ";
          }
          
          for(String location: locations)
          {
              query_local += "(AFFIL:("+location+"))^2 ";
              query_local += "(CONF:("+location+"))^1.5 ";
              query_local += "(JOUR:("+location+"))^1.5 ";
              query_local += "(TITLES:("+location+"))^1.5 ";
              query_local += "(KEYW:("+location+"))^1.5 ";
              megaorganization += location+" ";
          }
          
          if(megaorganization.length() > 0 )
          {
          	megaorganization = megaorganization.substring(0,megaorganization.length()-1);
          	query_local += "(AFFIL:("+megaorganization+"))^2 ";
            query_local += "(CONF:("+megaorganization+"))^1.5 ";
            query_local += "(JOUR:("+megaorganization+"))^1.5 ";
            query_local += "(TITLES:("+megaorganization+"))^1.5 ";
            query_local += "(KEYW:("+megaorganization+"))^1.5 ";
          			
          }
          if(megaperson.length() > 0 )
          {
          	megaperson = megaperson.substring(0,megaperson.length()-1);
          	query_local += "(NAMES:("+megaperson+"))^2 ";
            query_local += "(COAU:("+megaperson+"))^1.5 ";
            
          			
          }
          
          //query_local += "(ALL:("+pregunta+"))";
          
          String aux_pregunta = pregunta.replaceAll("(?i)( is | in )", " ");
          query_local += "(ALL:("+aux_pregunta.replaceAll("(?i)(what |what|who |who|works |works|with |with|\\?)", "")+"))";
          
      }
      else
      {
        query_local = pregunta.substring(7);
      }
    
      
      
      
      if(query_local.length() == 0)
      {
          if(pregunta.startsWith("ALL")) query_local = pregunta;
          else query_local = "ALL:"+pregunta;
          //query_local = pregunta;            
      }
      
      return query_local;
      
      
      
      
    	
    }
    
    public void imprimirDoc(ScoreDoc scoreDoc)
    {
    	Document doc = querier.retrieveDoc(scoreDoc);
			String names = querier.getName(doc.get("NAMES"));
			
			List<Field> fields = doc.getFields();
			System.out.println("DOC");
			for(int i = 0; i < fields.size(); i++)
			{
				Field field = fields.get(i);
				String name = field.name();
				
				if(name.compareTo("CONFL") == 0 || name.compareTo("JOURL") == 0 || name.compareTo("COAULINKS") == 0 || name.compareTo("Text") == 0 )continue;
				
				System.out.println("\t"+field.name()+": "+field.stringValue());
			}
		
    }
    
    
    public ScoreDoc[]  getScoreDocs()
    {
        //Se ejecuta la query creada
    		ScoreDoc[] resQuery = querier.retrieveDocs(query);
        
        String names = "";
        String docNo = "";
        LinkedList<String> nombresVistos = new LinkedList<String>();
        LinkedList<String> docNoVistos = new LinkedList<String>();
        
        LinkedList<ScoreDoc> docs = new LinkedList<ScoreDoc>();
        
        for (int i = 0; i < resQuery.length; i++) 
        {
  				ScoreDoc scoreDoc = resQuery[i];
  				Document doc = querier.retrieveDoc(scoreDoc);
  				names = querier.getName(doc.get("NAMES"));
          docNo = doc.get("DOCNO");
          
          if(nombresVistos.contains(names) || docNoVistos.contains(docNo))
          {
              continue;    
          }
          else
          {
              nombresVistos.add(names);
              docNoVistos.add(docNo);
              docs.add(scoreDoc);
          }
          
        }
        
        return docs.toArray(new ScoreDoc[0]);
        
    
    }
    
    public QCEnumTypes getAskedEntity()
    {
    	 
    	//TODO if(question_words.length > 0)
    	
    	if(question_words.length == 0)
    	{
    		asked_entity = QCEnumTypes.val("NOVALUE");
    		return asked_entity;
    	}
    	
    	Entity question_word = question_words[0];
    	boolean which = question_word.entity.compareToIgnoreCase("which") == 0 || question_word.type.compareToIgnoreCase("WDT") == 0;
    	boolean where = question_word.entity.compareToIgnoreCase("where") == 0 || question_word.type.compareToIgnoreCase("WRB") == 0;
    	boolean who = question_word.entity.compareToIgnoreCase("who") == 0 || question_word.type.compareToIgnoreCase("WP") == 0;
    	boolean whom = question_word.entity.compareToIgnoreCase("whom") == 0 || (question_word.type.compareToIgnoreCase("WP") == 0 && wordTypeExists("IN"));
    	
      if (where) asked_entity = QCEnumTypes.val("WHERE");
      else if (who && !whom) asked_entity = QCEnumTypes.val("WHO");
      else if (whom) asked_entity = QCEnumTypes.val("WHOM");
      else if (which) asked_entity = QCEnumTypes.val("WHICH");
      else
      {
      	asked_entity = QCEnumTypes.val("NOVALUE");
      }
      
      return asked_entity;

    	
    	
    	
    }
    
    public void getResponsePerson(boolean print)
    {
    	ScoreDoc scoreDoc = score_docs[0];
  		Document doc = querier.retrieveDoc(scoreDoc);
  		String name = querier.getName(doc.get("NAMES"));
  		//TODO Verificar cercania a de la pregunta a la NER
  		System.out.println("Score: "+ (Float.toString(max_score)).substring(0, 4));
  		//System.out.println("Distancia (sobre 100): "+ (Integer.toString(distancias.get(1))));
  		
  		switch(asked_entity)
  		{
  			case NOVALUE:
  					System.out.println("Respuesta: Su pregunta no fue identificada");
  			
  			case WHO:
  					System.out.println("Respuesta: "+name+" es el/la investigador/a con ID "+doc.get("DocID"));
  				break;
  				
  			case WHOM:
  					System.out.println("Respuesta: "+name+" trabaja con "+doc.get("COAU"));
  				break;
  				
  			case WHERE:
  					System.out.println("Respuesta: "+name+" trabaja en "+doc.get("AFFIL"));
  				break;
  			case WHICH:
					System.out.println("Respuesta: "+name+" trabaja temas como: "+doc.get("CAT"));
				break;
  		}
  			
  		
  		if(print)imprimirDoc(scoreDoc);
    	
    }
    
    
    public void getResponseAFFIL(String entidad)
    {
    	
    	System.out.println("Institucion reconocida: "+entidad);
  		switch(asked_entity)
  		{
  			case NOVALUE:
  					System.out.println("Respuesta: Su pregunta no fue identificada");
  			
  			case WHO:
  					System.out.println("Respuesta: Quienes trabajan en la institucion");
  				break;
  				
  			case WHOM:
  				System.out.println("Respuesta: Quienes trabajan en la institucion");
  				break;
  				
  			case WHERE:
  					System.out.println("Respuesta: Ubicacion de la institucion");
  				break;
  			case WHICH:
					System.out.println("Respuesta: Algo relacionado con la institucion");
				break;
  		}
    	
    }
    
    public void getResponseCONF(String entidad)
    {
    	
    	System.out.println("Conferencia reconocida: "+entidad);
  		switch(asked_entity)
  		{
  			case NOVALUE:
  					System.out.println("Respuesta: Su pregunta no fue identificada");
  			
  			case WHO:
  					System.out.println("Respuesta: Quienes asistieron a la conferencia");
  				break;
  				
  			case WHOM:
  				System.out.println("Respuesta: Quienes asistieron a la conferencia");
  				break;
  				
  			case WHERE:
  					System.out.println("Respuesta: Donde fue la conferencia");
  				break;
  			case WHICH:
					System.out.println("Respuesta: Algo relacionado con la coferencia");
				break;
  		}
    	
    }
    
    public void getResponseJOUR(String entidad)
    {
    	
    	System.out.println("Journal reconocida: "+entidad);
  		switch(asked_entity)
  		{
  			case NOVALUE:
  					System.out.println("Respuesta: Su pregunta no fue identificada");
  			
  			case WHO:
  					System.out.println("Respuesta: Quienes escriben en el journal");
  				break;
  				
  			case WHOM:
  				System.out.println("Respuesta: Quienes escriben en el journal");
  				break;
  				
  			case WHERE:
  					System.out.println("Respuesta: Donde (?)");
  				break;
  			case WHICH:
					System.out.println("Respuesta: Algo relacionado con el journal");
				break;
  		}
    	
    }
    
    public void getResponseTOPIC(String entidad)
    {
    	
    	System.out.println("Tema reconocido (CAT|CATLIST|TITLES|KEYW): "+entidad);
  		switch(asked_entity)
  		{
  			case NOVALUE:
  					System.out.println("Respuesta: Su pregunta no fue identificada");
  			
  			case WHO:
  					System.out.println("Respuesta: Quienes se relacionan con este tema");
  				break;
  				
  			case WHOM:
  				System.out.println("Respuesta: Quienes se relacionan con este tema");
  				break;
  				
  			case WHERE:
  					System.out.println("Respuesta: Donde (?)");
  				break;
  			case WHICH:
					System.out.println("Respuesta: Algo relacionado con el tema");
				break;
  		}
    	
    }
    
    public boolean esCampo(String campo, String entidad, boolean hard_way)
    {
    	
    	if(querier.retrieveDoc(score_docs[0]).get(campo).compareToIgnoreCase(entidad) == 0)
			{
				return true;
			}
			else
			{
				ScoreDoc[] resQuery = querier.retrieveDocs(campo+":("+entidad+")");
				if(resQuery.length > 0 && querier.retrieveDoc(resQuery[0]).get(campo).compareToIgnoreCase(entidad) == 0)
				{
					return true;
				}	
			}
			
			if(hard_way)
			{
				LinkedList<String> all_values = querier.allValues(campo, true);
     		if(all_values.contains(entidad))
     		{
     				return true; 
     		}
			}
			
			return false;
    }
    
    public void getResponse(boolean print)
    {
    	 
    	
      System.out.println("Query: "+ query);
      System.out.println("Resultados totales: "+ Integer.toString(score_docs.length));
      //System.out.println("Score maximo: "+ (Float.toString(max_score)).substring(0, 4));
      
      boolean alguna_ner = persons.length > 0 || organizations.length > 0 || locations.length > 0;
      boolean solo_personas = persons.length > 0 && organizations.length == 0 && locations.length == 0;
      boolean solo_una_persona = solo_personas && persons.length == 1;
      boolean solo_otras_entidades = alguna_ner && persons.length == 0;
      boolean solo_organizaciones = solo_otras_entidades && locations.length == 0;
      boolean solo_una_organizacion = solo_organizaciones && organizations.length == 1;
      boolean solo_locations = solo_otras_entidades && organizations.length == 0;
      boolean solo_una_location = solo_locations && locations.length == 1;
      boolean solo_una_otra_entidad = solo_otras_entidades && ( solo_una_location || solo_una_organizacion);  

      String entidad = "";
 			String[] fields;
 			String[] matched_strs;
 			String name;

      
      if(alguna_ner)
      {
      		if(solo_una_persona) //Solo un NER = PERSON
      		{
      			getResponsePerson(print);
        		
      		}
      		else if(solo_una_otra_entidad) //Solo un NER = ORGANIZATION  LOCATION
          {
      
      			if(solo_una_location) entidad = locations[0]; //Por ahora se tratan igual
      			else if(solo_una_organizacion) entidad = organizations[0]; //Por ahora se tratan igual
      		
      			
      			boolean hard_way = false;
      			
      			if(esCampo("AFFIL", entidad, hard_way)) getResponseAFFIL(entidad);
      			else if(esCampo("CONF", entidad, hard_way)) getResponseCONF(entidad);
      			else if(esCampo("JOUR", entidad, hard_way)) getResponseJOUR(entidad);
      			else if(esCampo("CAT", entidad, hard_way) || esCampo("CATLIST", entidad, hard_way) || esCampo("KEYW", entidad, hard_way) || esCampo("TITLES", entidad, hard_way)) getResponseTOPIC(entidad);
      			else
      			{
      				System.out.println("Entidad reconocida, no matcheada con nada: "+entidad);
      				
      				for(int i = 0; i < score_docs.length; i++)
             	{
             		
      					Document doc = querier.retrieveDoc(score_docs[i]);
             		name = querier.getName(doc.get("NAMES"));
             		fields = querier.bestRankedFields(pregunta, doc, true,null);
             		
             		if(fields.length > 0)
             		{
             			System.out.print(String.format("%-20s", name)+"  ");
               		for(int j=0; j < fields.length; j++)
               		{
               			System.out.print(fields[j]+"(");//+": "+doc.get(fields[j])+"  ");
               			matched_strs = querier.bestRankedFields(pregunta, doc, true,fields[j]);
               			for(int m=0; m < matched_strs.length; m++)
               			{
               				System.out.print(matched_strs[m]+" ");
               			}
               			
               			System.out.print(") ");
               			
               		}
               		System.out.println("");	
             		}
              }      				
      			}


          }
      		else //Mas de un NER, cualquier combinaci�n
      		{
      			System.out.println("Mas de una entidad reconocida");
      			
      			LinkedList<String> entidades = new LinkedList<String>();
      			for(int j = 0; j < persons.length; j++) entidades.add(persons[j]);
         		for(int j = 0; j < organizations.length; j++) entidades.add(organizations[j]);
         		for(int j = 0; j < locations.length; j++) entidades.add(locations[j]);
      			
      			String out;
      			
         		for(int i = 0; i < score_docs.length; i++)
           	{
         			
           		Document doc = querier.retrieveDoc(score_docs[i]);
           		out = "";
           		for(int j = 0; j < entidades.size(); j++)
           		{
	           			//fields = querier.bestRankedFields(, doc, false,null);
	             		fields = querier.bestRankedFields(entidades.get(j), doc, true,null);
	             		
	             		if(fields.length > 0)
	             		{
	               		for(int k=0; k < fields.length; k++)
	               		{
	               			
	               			matched_strs = querier.bestRankedFields(entidades.get(j), doc, false,fields[k]);
	               			if(matched_strs.length > 0 ) out+=fields[k]+"(";//+": "+doc.get(fields[j])+"  ");
	               			
	               			for(int m=0; m < matched_strs.length; m++)
	               			{
	               				out+=matched_strs[m]+" ";
	               			}
	               			
	               			if(matched_strs.length > 0 )out=out.substring(0, out.length() -1)+") ";
	               			
	               		}
	             		}
		             	
	             	
	             		
	             		
           		}
           		if(out.length() > 0 )
             	{
             		name = querier.getName(doc.get("NAMES"));
             		out = String.format("%-20s", name)+"  "+out;
             		System.out.println(out);
             	}
           			
            }
      			
      		}
      		
      }
      else //Ninguna NER
      {
      	System.out.println("Ninguna entidad reconocida");
      	pregunta = pregunta.replaceAll("(?i)( is | in )", " ");
      	pregunta = pregunta.replaceAll("(?i)(what |what|who |who|works|with |with|\\?)", "");
      	
      	for(int i = 0; i < score_docs.length; i++)
       	{
       		Document doc = querier.retrieveDoc(score_docs[i]);
       		name = querier.getName(doc.get("NAMES"));
       		fields = querier.bestRankedFields(pregunta, doc, true,null);
       		
       		if(fields.length > 0)
       		{
       			System.out.print(String.format("%-20s", name)+"  ");
         		for(int j=0; j < fields.length; j++)
         		{
         			System.out.print(fields[j]+"(");//+": "+doc.get(fields[j])+"  ");
         			matched_strs = querier.bestRankedFields(pregunta, doc, true,fields[j]);
         			for(int m=0; m < matched_strs.length; m++)
         			{
         				System.out.print(matched_strs[m]+" ");
         			}
         			
         			System.out.print(") ");
         			
         		}
         		System.out.println("");	
       		}
        }
      }
     
     
     
      
    }
    
    public void getScoresAndDistances()
    {
      
    	distancias = new LinkedList<Integer>();
      
      //Scores guarda un valor normalizado sobre maxScore
      scores = new LinkedList<Integer>();
    	
      
    	max_score = 1.0f;
    	
      if(score_docs.length > 0)
      	max_score = score_docs[0].score;
      
      
    	for(int i = 0; i < score_docs.length; i ++)
    	{
    		
    		ScoreDoc scoreDoc = score_docs[i];
				
    		scores.add((int)( (scoreDoc.score / max_score ) * 100));
    		
        if(i == 0)
        	distancias.add(0);	
        else
        	distancias.add( (scores.get(i-1) - scores.get(i)));

    	}
    	 
     
/*     
      for(int i = 0; i < total; i++)
      {
      	
      	ScoreDoc scoreDoc = score_docs[i];
				Document doc = querier.retrieveDoc(scoreDoc);
				String names = querier.getName(doc.get("NAMES"));
        String docNo = doc.get("DOCNO");
        String affil = doc.get("AFFIL");
      	System.out.println((Integer.toString(scores.get(i)))+" "+names+" "+affil);
      	
      }
    }
    */
    }
		
    public void qcResponse()
    {
      switch(qc_class)
      {
          case HUM:
              qcResponseHum();
          break;
          case ENTY:
          	qcResponseEnty();
          break;
          case LOC:
          	qcResponseLoc();
          break;
          case NUM:
          	qcResponseNum();
          break;
          
            case ABBR:
              //ABBR:exp
              //ABBR:abb
            	qcResponseAbbr();
              //System.out.println("La respuesta es una abreviacion");
              //System.out.println("Detalle: "+qc);
          break;
          case DESC:
              //DESC:manner
              //DESC:reason
              //DESC:def
              //DESC:desc
          	qcResponseDesc();
              //System.out.println("La respuesta es una descripcion");
              //System.out.println("Detalle: "+qc);
          break;
      
      }
    }
    
    //HUMAN	human beings
    public void qcResponseHum()
    {

      //HUM:ind   an individual                      => Investigador
      //HUM:gr    a group or organization of persons => Universidad / [Investigador]
      //HUM:title title of a person                  => Investigador
      //HUM:desc 	description of a person            => Investigador

    	System.out.println("La respuesta es un humano");
        

        
        switch(qc_subclass)
        {
            case ind:
              
                    
            
            break;
            
            case gr:
            
            break;
            
            case title:
            
            break;
            
            case desc:
            
            break;
        }
    }
    
    //LOCATION	human beings
    public void qcResponseLoc()
    {

        //LOC:state
        //LOC:other
        //LOC:city
        //LOC:country
        //LOC:mount

        System.out.println("La respuesta es una localidad");

       
        
        switch(qc_subclass)
        {
            case state:
            
            break;
            
            case city:
            
            break;
            
            case country:
            
            break;
            
            case other:
            
            break;
            
            case mount:
            
            break;
        }
    }


    /*
      NUMERIC	numeric values
        NUM:date
        NUM:count
        NUM:money
        NUM:period
        NUM:volsize
        NUM:other
        NUM:speed
        NUM:perc
        NUM:code
        NUM:dist
        NUM:temp
        NUM:ord
        NUM:weight
    */
    public void qcResponseNum()
    {

        
        System.out.println("La respuesta es un numero");
        
        switch(qc_subclass)
        {
            case date:
            
            break;
                        
            case count:
                        
            break;
                                                
            case money:
                        
            break;
                                                
            case period:
                        
            break;
                                                
            case volsize:
                        
            break;
                                                
            case other:
                        
            break;
                                                
            case speed:
                        
            break;
                                                
            case perc:
                        
            break;
                                                
            case code:
                        
            break;
                                                
            case dist:
                        
            break;
                                                
            case temp:
                        
            break;
                                                
            case ord:
                        
            break;
                                                
            case weight:
                        
            break;
                                                
        }
    }
    
    public void qcResponseEnty()
    {

        
        System.out.println("La respuesta es una entidad");
        
        switch(qc_subclass)
        {
            
            case substance:
            
            break;
            
            case sport:
            
            break;            
            case plant:
                        
            break;                                    
            case techmeth:
                        
            break;                                    
            case cremat:
                        
            break;                                    
            case animal:
                        
            break;                                    
            case event:
                        
            break;                                    
            case other:
                        
            break;                                    
            case letter:
                        
            break;                                    
            case religion:
                        
            break;                                    
            case food:
                        
            break;                                    
            case product:
                        
            break;                                    
            case color:
                        
            break;                                    
            case termeq:
                        
            break;                                    
            case body:
                        
            break;                                    
            case dismed:
                        
            break;                                    
            case instru:
                        
            break;                                    
            case word:
                        
            break;                                    
            case lang:
                        
            break;                                    
            case symbol:
                        
            break;                                    
            case veh:
                        
            break;                                    
            case currency:
                        
            break;                                    
            
            default:
                  System.out.println("Detalle: "+qc);
            break;
            
        }
    }
    
    public void qcResponseAbbr()
    {

        
        System.out.println("La respuesta es una abreviacion");
        
        switch(qc_subclass)
        {
            case exp:
            
            break;
            
            case abb:
            
            break;
        }
    }
    
    
    public void qcResponseDesc()
    {

        
        System.out.println("La respuesta es una descripcion");
        
        switch(qc_subclass)
        {
            case manner:
            
            break;
            
            case reason:
            
            break;
            
            case def:
            
            break;
            
            case desc:
            
            break;
        }
    }
    
    /*
    public Entity[] getEntities()
     {
         ArrayList<Entity> res = new ArrayList<Entity>();
         
         StringTokenizer tokens = new StringTokenizer(ner);
         
         String[] recognizedEntities = {"PERSON", "LOCATION", "ORGANIZATION"};
         String lastNer = "";
         String entity = "";
         
 				while (tokens.hasMoreTokens()) 
         {
 						String word = tokens.nextToken();
             //System.out.println("Word: " + word);
 						int delimNer = word.indexOf('/');
             
 						if (delimNer != -1) 
             {
 				
 							try
               {
               
               	String nerTag = word.substring(delimNer+1);
                 String theWord = StripXMLChar(word.substring(0, delimNer));
 		              
                 if(nerTag.length() > 5 && nerTag.substring(0,6).compareToIgnoreCase("PERSON") == 0)
                 {
                   //System.out.println("Person");
                   if(lastNer.compareToIgnoreCase("PERSON") == 0)
                   {
                      //System.out.println("Vengo de person");
                      entity +=" "+theWord;
                   
                   }
                   else if(lastNer.compareToIgnoreCase("") == 0)
                   {
                       //System.out.println("Vengo de null");
                       entity = theWord;
                       lastNer = "PERSON";
                   }
                   else
                   {
                       //System.out.println("Vengo de "+ lastNer);
                       res.add( new Entity(entity, lastNer));
                       lastNer="PERSON";
                       entity = theWord;
                       
                   }
 									
 								}
 								else if (nerTag.length() > 11 && nerTag.substring(0,12).compareToIgnoreCase("ORGANIZATION") == 0)
                 {
                   // System.out.println("la orga");
                   if(lastNer.compareToIgnoreCase("ORGANIZATION") == 0)
                   {
                      //System.out.println("vengo de la orga");
                      entity +=" "+theWord;
                   
                   }
                   else if(lastNer.compareToIgnoreCase("") == 0)
                   {
                       //System.out.println("vengo de null");
                       entity = theWord;
                       lastNer = "ORGANIZATION";
                   }
                   else
                   {
                       //System.out.println("Vengo de "+ lastNer);
                       res.add( new Entity(entity, lastNer));
                       lastNer="ORGANIZATION";
                       entity = theWord;
                   }
                 } 
                 else if (nerTag.length() > 7 && nerTag.substring(0,8).compareToIgnoreCase("LOCATION") == 0)
                 {
                 
                   //System.out.println("la loca");
                   if(lastNer.compareToIgnoreCase("LOCATION") == 0)
                   {
                      //System.out.println("vengo de la loca");
                      entity +=" "+theWord;
                   
                   }
                   else if(lastNer.compareToIgnoreCase("") == 0)
                   {
                       //System.out.println("vengo de null");
                       entity = theWord;
                       lastNer = "LOCATION";
                   }
                   else
                   {
                       //System.out.println("Vengo de "+ lastNer);
                       res.add( new Entity(entity, lastNer));
                       lastNer="LOCATION";
                       entity = theWord;
                   }
                 }
                 else 
                 {
                     //System.out.println("no matchee");
                     if(lastNer.compareToIgnoreCase("") != 0)
                     {
                           //System.out.println("Vengo de "+ lastNer);
                           res.add( new Entity(entity, lastNer));
                     }                    
                     lastNer = "";
                     entity = "";
                 }
                 
                 
 							} catch (Exception ex) {
                     System.out.println("excepcion: "+ex);
 								continue;
 							}
 						}
             
          }
         
         if(lastNer.compareToIgnoreCase("") != 0)
         {
             res.add( new Entity(entity, lastNer));
         }                     
          
         return res.toArray(new Entity[0]);
     }
     */
     
    
    
    
    
    
    
    
    
    
    /**
  	 * Entry point function.
  	 * You don't have to change this usually, so you can just copy this to make your own
  	 * Controller.
  	 *
  	 * @param args
  	 */
  	public static void main(String args[]) {
   
        
  		Interface interfaz = new Interface();
        
        interfaz.qa();
        
        //LuceneQuerier lq = new LuceneQuerier(args[0]);
        
        //lq.searchIndex(args[1]);
        //System.out.println(res);
        System.out.println("Ejecute correctamente");
   
    }






}