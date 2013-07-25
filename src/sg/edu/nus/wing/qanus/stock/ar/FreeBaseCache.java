package sg.edu.nus.wing.qanus.stock.ar;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Acts as a cache, storing results from Freebase.
 * We need this because Freebase's Terms of Service only allow a max of 
 * 100,000 queries per day per IP address.
 * If we can cache queries results, though the resulting disk space requirements would be
 * significant, it would help us stay in Freebase's good books.
 *
 * @author NG, Jun Ping -- junping@comp.nus.edu.sg
 * @version 31Dec2009
 */
public class FreeBaseCache {

    private String m_FileName;

    private ObjectInputStream m_CacheInputStream;

    private HashMap<String,Integer> m_Cache;


    public FreeBaseCache(String a_FileName) {
        
        m_CacheInputStream = null;
        m_Cache = null;
        
        
        OpenCache(a_FileName);        

    } // end constructor



    private void OpenCache(String a_FileName) {

        try {
            m_FileName = a_FileName;
            m_CacheInputStream = new ObjectInputStream(new FileInputStream(a_FileName));
            m_Cache = (HashMap) m_CacheInputStream.readObject();
            m_CacheInputStream.close();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger("QANUS").log(Level.WARNING, "Did not manage to read in cache.", ex);
            m_CacheInputStream = null;
        } catch (IOException ex) {
            Logger.getLogger("QANUS").log(Level.WARNING, "Error while accessing cache file.", ex);
            m_CacheInputStream = null;
        }

        // Even if we did not manage to read in cache, we keep a cache to save to later
        if (m_CacheInputStream == null) {
            m_Cache = new HashMap<String,Integer>();
        }

    } // end OpenCache()



    /**
     * Saves the additional queries added to the cache via AddResult().
     * If this function is not called the results will be lost.
     * Existing results will be retained.
     */
    public void SaveCache() {

        ObjectOutputStream l_OOS = null;
        try {
            
            l_OOS = new ObjectOutputStream(new FileOutputStream(m_FileName, false));
            l_OOS.writeObject(m_Cache);
            l_OOS.close();

        } catch (IOException ex) {
            Logger.getLogger("QANUS").log(Level.WARNING, "Unable to open cache file for writing.", ex);
        } finally {
            try {
                l_OOS.close();
            } catch (IOException ex) {
                Logger.getLogger("QANUS").log(Level.WARNING, "Error trying to close cache output stream.", ex);
            }
        } // end try-catch


    } // end SaveCache()


    /**
     * Obtain cache results
     * @param a_Query [in] the query to obtain results for
     * @return the result (either 1,0,-1) if found, or -2 if item is not in cache.
     */
    public int GetResult(String a_Query) {

        if (m_Cache.containsKey(a_Query)) {
            return m_Cache.get(a_Query);
        } else {
            return -2;
        }

    } // end GetResult()


    /**
     * Adds a result item to the cache.
     * If the item already exists, it is overwritten
     * @param a_Query [in] key to use
     * @param a_Result [in] result associated with key
     */
    public void AddResult(String a_Query, int a_Result) {
        
        m_Cache.put(a_Query, a_Result);

    } // end AddResult()

} // end FreeBaseCache

