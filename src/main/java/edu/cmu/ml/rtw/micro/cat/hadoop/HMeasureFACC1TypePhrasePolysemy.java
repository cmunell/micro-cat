package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.Text;

/**
 * HMeasureFACC1TypePhrasePolysemy measures the polysemy of noun phrases on the
 * FACC1+Type data output from HConstructFACC1Type as the entropy of the Freebase entity 
 * distribution for each noun phrase.
 * 
 * @author Bill McDowell
 */
public class HMeasureFACC1TypePhrasePolysemy {
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text phraseKey = new Text();
		private Text value = new Text();

		private Pattern FACC1TypePattern = Pattern.compile("([^\t]*)\t[^\t]*\t([^\t]*)\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*)\t([^\t]*)");
		private Pattern clueWeb09FilterPattern;
		
		@Override
		protected void setup(Context context) {
			super.setup(context);
			this.clueWeb09FilterPattern = Pattern.compile(this.properties.getClueWeb09FilterPattern());
		}
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher FACC1TypeMatcher = this.FACC1TypePattern.matcher(valueStr);
			
			if (!FACC1TypeMatcher.matches())
				return;
			
			String document = FACC1TypeMatcher.group(1);
			Matcher documentFilterMatcher = this.clueWeb09FilterPattern.matcher(document);
			if (!documentFilterMatcher.matches())
				return;
			
			
			String phrase = FACC1TypeMatcher.group(2);
			//String freebaseTopic = FACC1TypeMatcher.group(3);
			String freebaseTypes = FACC1TypeMatcher.group(4);
			
			this.phraseKey.set(phrase);
			this.value.set(freebaseTypes);
			
			context.write(this.phraseKey, this.value);
		}
	}
	
	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {
		private Text outputValue = new Text();
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			Map<String, Double> entityDistribution = new HashMap<String, Double>();
			double totalCount = 0;
			
			for (Text value : values) {
				String valueStr = value.toString();
				if (!entityDistribution.containsKey(valueStr))
					entityDistribution.put(valueStr, 0.0);
				entityDistribution.put(valueStr, entityDistribution.get(valueStr) + 1);
				totalCount++;
			}
			
			normalizeDistribution(entityDistribution, totalCount);
			double entropy = computeEntropy(entityDistribution);
		
			StringBuilder outputValueStr = new StringBuilder();
			outputValueStr.append(entropy).append("\t")
						  .append(totalCount).append("\t");
			
			for (Entry<String, Double> entry : entityDistribution.entrySet())
				outputValueStr.append(entry.getKey())
							  .append(": ")
							  .append(entry.getValue())
							  .append("\t");
			
			outputValueStr.delete(outputValueStr.length() - 1, outputValueStr.length());
			
			this.outputValue.set(outputValueStr.toString());
			
			context.write(key, this.outputValue);
		}
		
		private void normalizeDistribution(Map<String, Double> distribution, double normalizer) {
			for (Entry<String, Double> entry : distribution.entrySet())
				entry.setValue(entry.getValue() / normalizer);
		} 
		
		private double computeEntropy(Map<String, Double> distribution) {
			double entropy = 0.0;
			
			for (Double p : distribution.values())
				entropy -= p*Math.log(p)/Math.log(2);
				
			return entropy;
		}
	}
}
