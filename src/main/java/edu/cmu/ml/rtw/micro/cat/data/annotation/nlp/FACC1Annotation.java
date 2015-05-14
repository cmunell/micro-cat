package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.util.JSONSerializable;

public class FACC1Annotation implements JSONSerializable {
	private String phrase;
	private int startByte; // Phrase start byte in clueweb
	private int endByte; // Phrase end byte in clueweb
	private double mcp; // Mention context posterior
	private double cp; // Context posterior
	private String freebaseTopic; // Freebase topic
	private String[] freebaseTypes; // Freebase types
	
	private static Pattern facc1TypePattern = Pattern.compile("[^\t]*\t[^\t]*\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)");
	
	public static class FACC1StartByteComparator implements Comparator<FACC1Annotation> {            
		public int compare(FACC1Annotation a1, FACC1Annotation a2) {
			if (a1.startByte < a2.startByte)
				return -1;
			else if (a1.startByte > a2.startByte)
				return 1;
			else
				return 0;
		}
	}
	
	public static FACC1StartByteComparator facc1StartByteComparator = new FACC1StartByteComparator();
	
	public static FACC1Annotation fromString(String str) {
		Matcher facc1TypeMatcher = FACC1Annotation.facc1TypePattern.matcher(str);
		
		if (!facc1TypeMatcher.matches())
			return null;
		
		FACC1Annotation facc1 = new FACC1Annotation();
		
		facc1.phrase = facc1TypeMatcher.group(1);
		facc1.startByte = Integer.valueOf(facc1TypeMatcher.group(2)); 
		facc1.endByte = Integer.valueOf(facc1TypeMatcher.group(3)); 
		facc1.mcp = Double.valueOf(facc1TypeMatcher.group(4)); 
		facc1.cp = Double.valueOf(facc1TypeMatcher.group(5));
		facc1.freebaseTopic = facc1TypeMatcher.group(6);
		facc1.freebaseTypes = facc1TypeMatcher.group(7).split(",");
		
		return facc1;
	}
	
	public FACC1Annotation() {
		
	}
	
	public String getPhrase() {
		return this.phrase;
	}
	
	public String getFreebaseTopic() {
		return this.freebaseTopic;
	}
	
	public String[] getFreebaseTypes() {
		return this.freebaseTypes;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("phrase", this.phrase);
			json.put("startByte", this.startByte);
			json.put("endByte", this.endByte);
			json.put("mcp", this.mcp);
			json.put("cp", this.cp);
			json.put("freebaseTopic", this.freebaseTopic);
			json.put("freebaseTypes", new JSONArray(this.freebaseTypes));
		} catch (JSONException e) {
			e.printStackTrace();
		}		
		return json;
	}
	
	@Override
	public boolean fromJSON(JSONObject json) {
		FACC1Annotation facc1 = new FACC1Annotation();
		try {
			facc1.phrase = json.getString("phrase");
			facc1.startByte = json.getInt("startByte");
			facc1.endByte = json.getInt("endByte");
			facc1.mcp = json.getDouble("mcp");
			facc1.cp = json.getDouble("cp");
			facc1.freebaseTopic = json.getString("freebaseTopic");
			
			JSONArray freebaseTypes = json.getJSONArray("freebaseTypes");
			facc1.freebaseTypes = new String[freebaseTypes.length()];
			for (int i = 0; i < facc1.freebaseTypes.length; i++)
				facc1.freebaseTypes[i] = freebaseTypes.getString(i);
		} catch (JSONException e) { 
			return false;
		}
		
		return true;
	}
}
