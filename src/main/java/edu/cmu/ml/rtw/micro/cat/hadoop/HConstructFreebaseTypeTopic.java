package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.Text;

import edu.cmu.ml.rtw.generic.data.Gazetteer;

public class HConstructFreebaseTypeTopic {
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text freebaseTypeKey = new Text();
		private Text freebaseTopicValue = new Text();
		
		private Pattern freebaseTriplePattern = Pattern.compile("<http://rdf\\.freebase\\.com/ns([^>]*)>\t<http://rdf\\.freebase\\.com/ns/type\\.type\\.instance>\t<http://rdf\\.freebase\\.com/ns([^>]*)>\t\\.");
		private Gazetteer freebaseNELLCategoryGazetteer;
		
		
		protected void setup(Context context) {
			super.setup(context);
			this.freebaseNELLCategoryGazetteer = this.dataTools.getGazetteer("FreebaseNELLCategory");
		}
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher freebaseTripleMatcher = this.freebaseTriplePattern.matcher(valueStr);
			
			if (freebaseTripleMatcher.matches()) {
				String freebaseType = freebaseTripleMatcher.group(1).replaceAll("\\.", "/");
				String freebaseTopic = freebaseTripleMatcher.group(2).replaceAll("\\.", "/");
				if (this.freebaseNELLCategoryGazetteer.contains(freebaseType)) {
					this.freebaseTypeKey.set(freebaseType);
					this.freebaseTopicValue.set(freebaseTopic);
					context.write(this.freebaseTypeKey, this.freebaseTopicValue);
				}
			} 
		}
	}
	
	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {
		private Text outputKey = new Text();
		private Text outputValue = new Text();
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			this.outputKey.set(key);
			for (Text value : values) {
				this.outputValue.set(value);
				context.write(key, value);
			}
		}
	}
}
