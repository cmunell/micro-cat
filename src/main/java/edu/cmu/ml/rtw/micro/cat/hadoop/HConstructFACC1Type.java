package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.io.Text;

public class HConstructFACC1Type {
	
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text documentTopicKey = new Text();
		private Text value = new Text();

		private Pattern FACC1Pattern = Pattern.compile("clueweb09-([^\t]*)\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*)");
		private Pattern documentTopicTypesPattern = Pattern.compile("([^\\s]*)[\\s]*([^\\s]*)[\\s]*([^\\s]*)");
	
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher FACC1Matcher = this.FACC1Pattern.matcher(valueStr);
			Matcher documentTopicTypesMatcher = this.documentTopicTypesPattern.matcher(valueStr);
			
			if (FACC1Matcher.matches()) {
				String cluewebDocument = FACC1Matcher.group(1);
				String freebaseTopic = FACC1Matcher.group(2);
				
				this.documentTopicKey.set(cluewebDocument + "," + freebaseTopic);
				this.value.set("FACC1\t" + valueStr);
			} else if (documentTopicTypesMatcher.matches()) {
				String cluewebDocument = documentTopicTypesMatcher.group(1);
				String freebaseTopic = documentTopicTypesMatcher.group(2);
				String freebaseTypes = documentTopicTypesMatcher.group(3);
				this.documentTopicKey.set(cluewebDocument + "," + freebaseTopic);
				this.value.set("freebaseTypes\t" + freebaseTypes);
			} else {
				return; // Ignore irrelevant FreeBase triple
			}
			
			context.write(this.documentTopicKey, this.value);
		}
	}

	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {		
		private Text outputKey = new Text();
		private Text outputValue = new Text();
	
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			String freebaseTypes = null;
			List<String> FACC1Values = new ArrayList<String>();
			for (Text value : values) {
				String valueStr = value.toString();
				
				if (valueStr.startsWith("freebaseTypes")) {
					freebaseTypes = valueStr.substring(valueStr.indexOf("\t") + 1);
				} else if (valueStr.startsWith("FACC1")) {
					FACC1Values.add(valueStr.substring(valueStr.indexOf("\t") + 1));
				}
			}
			
			if (freebaseTypes == null || FACC1Values.size() == 0) {
				// No NELL types or documents for this topic so ignore
				return;
			}
			
			this.outputValue.set(freebaseTypes);
			for (String FACC1Value : FACC1Values) {
				this.outputKey.set(FACC1Value);
				context.write(this.outputKey, this.outputValue);
			}
		}
	}
}


