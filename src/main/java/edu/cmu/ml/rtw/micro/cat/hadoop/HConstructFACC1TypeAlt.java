package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.Text;

import edu.cmu.ml.rtw.generic.data.Gazetteer;

/**
 * @deprecated
 * HConstructFACC1TypeAlt is an alternative
 * method to producing the output of HConstructFACC1Type.
 * I think maybe it was too slow or something, so I 
 * stopped trying to do it this way.
 * 
 * @author Bill McDowell
 *
 */
public class HConstructFACC1TypeAlt {
	
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text documentKey = new Text();
		private Text outputValue = new Text();

		private Pattern FACC1Pattern = Pattern.compile("clueweb09\\-([^\t]*)\t([^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*)");

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher FACC1Matcher = this.FACC1Pattern.matcher(valueStr);
			
			if (!FACC1Matcher.matches())
				return;
			
			String cluewebDocument = FACC1Matcher.group(1);
			String otherFACC1Info = FACC1Matcher.group(2);
			
			this.documentKey.set(cluewebDocument);
			this.outputValue.set(otherFACC1Info);
			
			context.write(this.documentKey, this.outputValue);
		}
	}

	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {
		private Gazetteer freebaseTypeTopicGazetteer;
		private Text outputValue = new Text();
		
		@Override
		protected void setup(Context context) {
			super.setup(context);
			this.freebaseTypeTopicGazetteer = this.dataTools.getGazetteer("FreebaseTypeTopic");
		}
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {			
			for (Text value : values) {
				String valueStr = value.toString();
				String[] valueStrParts = valueStr.split("\t");
				String freebaseTopic = valueStrParts[valueStrParts.length - 1];
				
				if (!this.freebaseTypeTopicGazetteer.contains(freebaseTopic))
					continue;
				
				List<String> freebaseTypes = this.freebaseTypeTopicGazetteer.getIds(freebaseTopic);
				StringBuilder freebaseTypesStr = new StringBuilder();
				for (String freebaseType : freebaseTypes)
					freebaseTypesStr = freebaseTypesStr.append(freebaseType).append(",");
				freebaseTypesStr = freebaseTypesStr.delete(freebaseTypesStr.length()-1, freebaseTypesStr.length());
				
				this.outputValue.set(valueStr + "\t" + freebaseTypesStr.toString());
				
				context.write(key, this.outputValue);
			}
		}
	}
}


