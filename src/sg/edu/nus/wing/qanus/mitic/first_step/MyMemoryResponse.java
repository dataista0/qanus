package sg.edu.nus.wing.qanus.mitic.first_step;

import java.util.HashMap;

public class MyMemoryResponse {
	
	public HashMap<String, String> responseData;
	public String responseDetails;
	public String responseStatus;
	public Object[] matches;
	
	public MyMemoryResponse MyMemoryResponse(String translatedText)
	{
				 responseData = new HashMap<String, String>();
				 responseData.put("translatedText", translatedText);
		
				 
				 return this;
	}

}
