package sg.edu.nus.wing.qanus.stock.ar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Used to query free base and verify the "type" of an item.
 *
 * For example we can check if "Virginia" is a state in the US.
 *
 *
 * @author NG, Jun Ping -- junping@nus.edu.sg
 * @version 31Dec2009
 */
public class FreebaseQuerier {

	private FreeBaseCache m_Cache;

	public enum ObjectTypes {
		HUMAN, GROUP, COUNTRY, CITY, STATE
	};

	public FreebaseQuerier(String a_CacheFileName) {
		m_Cache = new FreeBaseCache(a_CacheFileName);
	} // end constructor

	/**
	 * Call this function beyond destroying the querier to save the
	 * additional queries results from this session.
	 * If this function is not called the results garnered in this session will be lost.
	 */
	public void SaveCache() {
		m_Cache.SaveCache();
	} // end SaveCache()

//    public static void main(String args[]) {
//
//        FreebaseQuerier l = new FreebaseQuerier();
//        if (l.CorrespondsToType("Linda McMahon", ObjectTypes.HUMAN) == 1) {
//            System.out.println("HUMAN");
//        }
//    }
	/**
	 * Verifies if the stated target is of the desired type
	 * @param a_QueryTarget [in] target we want to enquire on
	 * @param a_Type [in] desired type of the target
	 * @return 1 if the target is of the type, 0 if there is an error, -1 if the target is not of the type.
	 */
	public int CorrespondsToType(String a_QueryTarget, ObjectTypes a_Type) {

		// Check the Cache first
		int l_CacheResult = m_Cache.GetResult(a_QueryTarget);
		switch (l_CacheResult) {
			case 1:
			case 0:
			case -1:				
				return l_CacheResult;
		} // end switch

		// Fall through from switch if no results from cache (i.e. -2)

		// No results from cache, carry out query
		String l_JSONResponse = SendFreebaseHTTPRequest(a_QueryTarget);
		int l_MatchResult = TargetMatchesType(a_QueryTarget, l_JSONResponse, a_Type);
		// If we get a confirmed result (i.e. not error), we save it in the cache
		if (l_MatchResult == 1 || l_MatchResult == -1) {
			// Matches! Store result in cache, then we can stop
			m_Cache.AddResult(a_QueryTarget, l_MatchResult);
		}

		return l_MatchResult;

	} // end CorrespondsToType()

	/**
	 * Verifies whether the type is listed within the provided JSON serliazation.
	 *
	 * The type is one of HUMAN, GROUP, etc...
	 * This method expands these into the types used within Freebase.
	 * For example, HUMAN corresponds to /people/person
	 * GROUP corresponds to /business/company, /organization/organization etc
	 *
	 * @param a_Query [in] the query to Freebase that gave us the JSON string
	 * @param a_JSONSerialization [in] the JSON string
	 * @param a_Type [in] the type we want to verify
	 * @return 1 if the type is found within the JSON string, or -1 if it is not. 0 is returned
	 *         if the JSON result is not valid.
	 */
	private int TargetMatchesType(String a_Query, String a_JSONSerialization, ObjectTypes a_Type) {

		JSONObject l_JSONObj = JSONObject.fromObject(a_JSONSerialization);

		String l_Code = l_JSONObj.getString("code");
		if (l_Code.compareToIgnoreCase("/api/status/ok") != 0) {
			// No result
			return 0;
		}

		// TODO
		if (a_Type == FreebaseQuerier.ObjectTypes.GROUP) System.out.println("query : " + a_Query);


		//
		try {

			// Walk through all returned results
			JSONArray l_ResultArray = l_JSONObj.getJSONArray("result");
			//for (int i = 0; i < l_ResultArray.size(); ++i) {
			// Do only for 1, intuition of "most common sense"			
			for (int i = 0; i < l_ResultArray.size() && i < 1; ++i) {
				JSONObject l_ResultObj = l_ResultArray.getJSONObject(i);
				String l_Type = l_ResultObj.getString("type");

				// Extract all the returned types
				// Start by dropping opening and closing brackets
				if (l_Type.startsWith("[") && l_Type.endsWith("]")) {
					l_Type = l_Type.substring(1, l_Type.length() - 1);
				}
				// Use tokenizer to break up the various types
				StringTokenizer l_ST = new StringTokenizer(l_Type, ",");
				while (l_ST.hasMoreTokens()) {

					String l_TypeInstance = l_ST.nextToken();

					// Drop quotation marks
					if (l_TypeInstance.startsWith("\"") && l_TypeInstance.endsWith("\"")) {
						l_TypeInstance = l_TypeInstance.substring(1, l_TypeInstance.length() - 1);
					}

					// TODO
					if (a_Type == FreebaseQuerier.ObjectTypes.GROUP) System.out.println("\t >>>> : " + l_TypeInstance);

					// This is the check
					switch (a_Type) {
						case HUMAN:
							if (l_TypeInstance.compareToIgnoreCase("/people/person") == 0) {
								return 1;
							}
							break;
						case COUNTRY:
							if (l_TypeInstance.compareToIgnoreCase("/location/country") == 0) {
								return 1;
							}
							break;
						case CITY:
							if (l_TypeInstance.compareToIgnoreCase("/location/citytown") == 0) {
								return 1;
							}
							break;
						case STATE:
							// This is a hard one to determine from Freebase, there's no proper defn for a "state"
							if (l_TypeInstance.compareToIgnoreCase("/location/us_state") == 0) {
								return 1;
							}
							if (l_TypeInstance.compareToIgnoreCase("/location/fr_region") == 0) {
								return 1;
							}
							if (l_TypeInstance.compareToIgnoreCase("/location/region") == 0) {
								return 1;
							}
						case GROUP:
							if (l_TypeInstance.compareToIgnoreCase("/business/company") == 0) {
								return 1;
							}
							if (l_TypeInstance.compareToIgnoreCase("/education/academic_institution") == 0) {
								return 1;
							}
							if (l_TypeInstance.compareToIgnoreCase("/organization/organization") == 0) {
								return 1;
							}
						default:

					}


				} // end while

			} // end for i


		} catch (JSONException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "No able to get results for [" + a_JSONSerialization + "]", ex);
			return 0;
		} catch (ClassCastException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "No able to get results for [" + a_JSONSerialization + "]", ex);
			return 0;
		}


		return -1;

	} // end TargetMatchesType()

	/**
	 * Composes a URI to query freebase with the provided target.
	 *
	 * @param a_QueryTarget [in] the target we want to query Freebase with
	 * @return The HTTP response from the Freebase server, or NULL on errors
	 */
	private String SendFreebaseHTTPRequest(String a_QueryTarget) {

		// Pre-process target to remove spaces
		a_QueryTarget = a_QueryTarget.replaceAll(" ", "%20");

		String l_URLString = "http://www.freebase.com/api/service/mqlread?query={\"query\":[{\"name\":\"" + a_QueryTarget + "\",\"type\":[]}]}";
		String l_HTTPResponse = "";
		try {

			URL l_URL = new URL(l_URLString);
			BufferedReader l_BR = new BufferedReader(new InputStreamReader(l_URL.openStream()));
			String l_ReadBuffer;
			while ((l_ReadBuffer = l_BR.readLine()) != null) {
				l_HTTPResponse += l_ReadBuffer;
			}

			return l_HTTPResponse;

		} catch (MalformedURLException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Unable to query freebase with target [" + a_QueryTarget + "]", ex);
			return null;
		} catch (IOException ex) {
			Logger.getLogger("QANUS").log(Level.WARNING, "Unable to query freebase with target [" + a_QueryTarget + "]", ex);
			return null;
		}

	} // end SendFreebaseHTTPRequest()
} // end class FreebaseQuerier

