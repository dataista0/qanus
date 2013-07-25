package sg.edu.nus.wing.qanus.mitic.first_step;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import com.cybozu.labs.langdetect.DetectorFactory;

import sg.edu.nus.wing.qanus.textprocessing.PorterStemmer;
import sg.edu.nus.wing.qanus.textprocessing.QuestionClassifierWithStanfordClassifier;
import sg.edu.nus.wing.qanus.textprocessing.StanfordNER;
import sg.edu.nus.wing.qanus.textprocessing.StanfordPOSTagger;



public class Interface 
{

  // Retrieve 100 top documents from search engine for each search
	//public final int RESULTS_TO_RETRIEVE = 50; // was 100
	//public LuceneInformationBaseQuerier m_InformationBase;
  public StanfordPOSTagger posTagger;
	public StanfordNER nerTagger; // TODO remove after the information is included into Lucene index
  public QuestionClassifierWithStanfordClassifier m_ModuleQC;
   
  public Interface()
  {
   // m_ModulePOS = new StanfordPOSTagger("lib" + File.separator + "bidirectional-wsj-0-18.tagger");

  	
  }
  
  public void menu()
  {
      Scanner user_input = new Scanner( System.in );

      String input;
      Integer switcher;
      Boolean exit = false;
      while(!exit)
      {
          System.out.println("Interface");
          System.out.println("\t1-LuceneQuerier");
          System.out.println("\t2-NER");
          System.out.println("\t3-POS-Tagger");
          System.out.println("\t4-QC");
          System.out.println("\t5-Exit");
          System.out.println("");
          System.out.print("Adelante, elegi:... ");
          input = user_input.next( );
          switcher = Integer.parseInt(input);
          
          switch(switcher)
          {
              
              case 1:
              case 2:
                qa();
              case 3:
              case 4:
                  System.out.println("No implementado");
              break;
              case 5:
                System.out.println("Saliendo...");
                exit = true;
              break;
              default: 
                  System.out.println("Opcion invalida");
              break;      
          }
      }
      
  
  }
  
  public void qa()
  {
      try
      {  
        //System.out.println("NER..");
        
        PrintStream stdout = System.out;
        
        PrintStream archivoOut = new PrintStream(new FileOutputStream("choppingboard"+File.separator+"trash.tmp", false));

        System.setOut(archivoOut);
       
        Boolean exit = false;
        Scanner user_input = new Scanner( System.in );
        System.out.println(user_input.delimiter());
        String input;
        Integer switcher;
        String[] sentences = new String[1];
        String[] ner_res = new String[1];
        String[] pos_res = new String[1];
        String[] qc_res = new String[1];
        String[] stemm_res = new String[1];
        String[] grammar_res = new String[1];
        
        QuestionClassifierWithStanfordClassifier parserQC = null;
        
        try {
          
        //  parserQC = new QuestionClassifierWithStanfordClassifier("lib"+ File.separator +"trec_classifier.stanford-classifier", "choppingboard" + File.separator + "temp");
        }
        catch (Exception ex) {
    			System.out.println("Error al iniciar QC:" + ex);
			    return;
		    }
        
        String base_url="/home/julian/qanus/qanus/";
        DetectorFactory.loadProfile("lang_profiles");
        
        StanfordNER parserNER = new StanfordNER(base_url+"lib" + File.separator + "ner-eng-ie.crf-4-conll-distsim.ser.gz");
        StanfordPOSTagger parserPOS = new StanfordPOSTagger(base_url+"lib" + File.separator + "bidirectional-wsj-0-18.tagger");
        PorterStemmer parserStemming = new PorterStemmer();
        //StanfordGrammarParser parserGrammar = new StanfordGrammarParser("lib"+ File.separator+ "stanford-parser-2008-10-26.jar");
        //Thesaurus thesaurus = new Thesaurus();
        
        System.setOut(stdout);
        
        while(!exit)
        {
                
            System.out.print("Ingrese Pregunta, Print o Salir:... ");
            input = user_input.nextLine( );
            //input = user_input.next( );
            boolean print = false;
            if(input.compareToIgnoreCase("Salir") == 0) break;
            
            if(input.compareToIgnoreCase("Print") == 0)
            {
            	System.out.print("Ingrese Pregunta:... ");
            	input = user_input.nextLine( );
            	print = true;
            }
     
            sentences[0] = input; 
            ner_res = parserNER.ProcessText(sentences);
            pos_res = parserPOS.ProcessText(sentences);
            //qc_res = parserQC.ProcessText(sentences);
            qc_res[0] = "HUM:desc";
            stemm_res = parserStemming.ProcessText(sentences);
            //grammar_res = thesaurus.ProcessText(sentences);
            Pregunta pregunta = new Pregunta(sentences[0], pos_res[0], ner_res[0],qc_res[0]);
            
            procesar(pregunta, print);
            //System.out.println(grammar_res[0]);
            
        }
        
        } catch (Exception ex) {
			       // Error encountered, unable to read result, just say no result.

        	System.out.println("excepcion:"+ex.getMessage()); 
        	ex.printStackTrace();
        	return;
		      }
        
        
        return;
  
  }
  
  

  
  public void procesar(Pregunta pregunta, boolean print)
  {     
        
    	pregunta.imprimirAnalisis();
    	pregunta.getResponse(print);
       
    
  }
  
  /*public void searchIndex(String query)  
  {
     	// Retrieve documents based on the search string from the search engine
			ScoreDoc[] l_RetrievedDocs = (ScoreDoc[]) m_InformationBase.SearchQuery(query);

			if (l_RetrievedDocs == null || l_RetrievedDocs.length  == 0) 
      {
          System.out.println("No search queries returned.");
          return;
      }
			for (int i = 0; i < l_RetrievedDocs.length; i++) {
				ScoreDoc l_ScoreDoc = l_RetrievedDocs[i];
				Document l_Doc = m_InformationBase.GetDoc(l_ScoreDoc.doc);
				String[] l_ArrText = l_Doc.getValues("Text");
				String l_Headline = l_Doc.get("Headline");
        System.out.println((Float.toString(l_ScoreDoc.score))+"   "+l_Headline);
      }
      
  }*/
  
   

   
   
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