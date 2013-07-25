package sg.edu.nus.wing.qanus.mitic.ibp;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class HelloLucene
{

    public static void buildIndex() throws IOException 
    {
        IndexWriter indexWriter = new IndexWriter(FSDirectory.getDirectory("..\\helloLuceneIndex"),
                                                  new StandardAnalyzer(),
                                                  IndexWriter.MaxFieldLength.LIMITED);
        String[] texts = new String[] {  "hello world",
                                         "hello sailor",
                                         "goodnight moon" };
        for (String text : texts) {
            Document doc = new Document();
            doc.add(new Field("text",text,
                        Field.Store.YES,Field.Index.ANALYZED));
            indexWriter.addDocument(doc);
        }
        indexWriter.close();
    }
    
    public static void searchIndex(String[] queryStrings) throws IOException, ParseException 
    {
      Searcher searcher = new IndexSearcher(FSDirectory.getDirectory("..\\helloLuceneIndex"));
      QueryParser parser = new QueryParser("text",new StandardAnalyzer());
      for (String queryString : queryStrings) 
      {
          System.out.println("\nsearching for: " + queryString);
          Query query = parser.parse(queryString);
          TopDocs results = searcher.search(query,10);
          System.out.println("total hits: " + results.totalHits);
          ScoreDoc[] hits = results.scoreDocs;
          for (ScoreDoc hit : hits) {
              Document doc = searcher.doc(hit.doc);
              System.out.printf("%5.3f %s\n",
                                hit.score, doc.get("text"));
          }
      }
      searcher.close();
    }
    
   	public static void main(String args[]) 
    {
        HelloLucene x = new HelloLucene();
        try
        {
          x.searchIndex(args);
        }
        catch(Exception e)
        {
            System.out.println("error");
        }
    }



}
