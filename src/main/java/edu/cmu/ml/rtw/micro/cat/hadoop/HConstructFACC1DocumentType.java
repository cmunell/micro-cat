package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.io.Text;

public class HConstructFACC1DocumentType {
	
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text freebaseTopicKey = new Text();
		private Text value = new Text();

		private Pattern FACC1Pattern = Pattern.compile("clueweb09-([^\t]*)\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*)");
		private Pattern freebaseTypeTopicPattern = Pattern.compile("([^\t]*)\t([^\t]*)");
	
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher FACC1Matcher = this.FACC1Pattern.matcher(valueStr);
			Matcher freebaseTypeTopicMatcher = this.freebaseTypeTopicPattern.matcher(valueStr);
			
			if (FACC1Matcher.matches()) {
				String cluewebDocument = FACC1Matcher.group(1);
				String freebaseTopic = FACC1Matcher.group(2);
				
				this.freebaseTopicKey.set(freebaseTopic);
				this.value.set("FACC1\t" + cluewebDocument);
			} else if (freebaseTypeTopicMatcher.matches()) {
				String freebaseType = freebaseTypeTopicMatcher.group(1);
				String freebaseTopic = freebaseTypeTopicMatcher.group(2);
				
				this.freebaseTopicKey.set(freebaseTopic);
				this.value.set("freebase\t" + freebaseType);
			} else {
				return; // Ignore irrelevant FreeBase triple
			}
			
			context.write(this.freebaseTopicKey, this.value);
		}
	}

	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {		
		private Text outputKey = new Text();
		private Text outputValue = new Text();
	
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			String freebaseTopic = key.toString();
			
			Set<String> freebaseTypes = new HashSet<String>();
			Set<String> cluewebDocuments = new HashSet<String>();
			for (Text value : values) {
				String[] valueParts = value.toString().split("\t");
				String valueType = valueParts[0];
				
				if (valueType.equals("freebase")) {
					String freebaseType = valueParts[1];
					freebaseTypes.add(freebaseType);
				} else if (valueType.equals("FACC1")) {
					String cluewebDocument = valueParts[1];
					cluewebDocuments.add(cluewebDocument);
				}
			}
			
			if (freebaseTypes.size() == 0 || cluewebDocuments.size() == 0) {
				// No NELL types or documents for this topic so ignore
				return;
			}
			
			List<String> freebaseTypesList = new ArrayList<String>();
			freebaseTypesList.addAll(freebaseTypes);
			Collections.sort(freebaseTypesList);
			StringBuilder freebaseTypesStr = new StringBuilder();
			for (String freebaseType : freebaseTypesList)
				freebaseTypesStr.append(freebaseType).append(","); 
			freebaseTypesStr = freebaseTypesStr.delete(freebaseTypesStr.length() - 1, freebaseTypesStr.length());
			
			for (String document : cluewebDocuments) {
				this.outputKey.set(document);
				this.outputValue.set(freebaseTopic + "\t" + freebaseTypesStr);
				context.write(this.outputKey, this.outputValue);
			}
		}
	}
}


