package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.Text;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.util.Pair;


/**
 * HMeasureFACC1TypePolysemy takes output from HMeasureFACC1TypePhrasePolysemy, 
 * and computes the weighted average polysemy across
 * phrases for each pair of types assuming each entity is either that type or not.  
 * This is the conditional entropy of entity distribution
 * given noun phrase.
 * 
 * @author Bill McDowell
 *
 */
public class HMeasureFACC1TypePolysemy {
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text typeKey = new Text();
		private Text outputValue = new Text();
		
		// phrase\tentropy\tcount\ttopic1,type11,..,type1n: value1\ttopic2,type21,...,type2n: value2...
		private Pattern phrasePolysemyPattern = Pattern.compile("[^\t]*\t[^\t]*\t([^\t]*)\t(.*)");
		
		private Gazetteer freebaseNELLCategoryGazetteer;
		
		protected void setup(Context context) {
			super.setup(context);
			this.freebaseNELLCategoryGazetteer = this.dataTools.getGazetteer("FreebaseNELLCategory");
		}
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher phrasePolysemyMatcher = this.phrasePolysemyPattern.matcher(valueStr);
			
			if (!phrasePolysemyMatcher.matches())
				return;
			
			double inputTotalCount = Double.valueOf(phrasePolysemyMatcher.group(1));
			String distributionStr = phrasePolysemyMatcher.group(2);
			String[] distributionStrParts = distributionStr.split("\t");
			
			// types -> (types, value in input distribution)
			Map<String, Pair<String[], Double>> inputDistribution = new HashMap<String, Pair<String[], Double>>();
			for (String distributionStrPart : distributionStrParts) {
				String[] typesAndValue = distributionStrPart.split(": ");
				String[] types = typesAndValue[0].split(",");
				
				double topicValue = Double.valueOf(typesAndValue[1]);
				
		
				inputDistribution.put(typesAndValue[0], new Pair<String[], Double>(types, topicValue));
			}	
			
			Set<String> allTypes = this.freebaseNELLCategoryGazetteer.getValues();
			
			for (String type : allTypes) {
				// Compute distribution over topics that only has type and rest
				Map<String, Double> distribution = new HashMap<String, Double>();
				double totalCount = 0;
				for (Entry<String, Pair<String[], Double>> entry : inputDistribution.entrySet()) {
					double countValue = entry.getValue().getSecond()*inputTotalCount;
					String currentType = "other";
					String[] topicTypes = entry.getValue().getFirst();
					for (int i = 0; i < topicTypes.length; i++) {
						if (topicTypes[i].equals(type)) {
							currentType = type;
							break;
						}
					}

					totalCount += countValue;
					
					if (!distribution.containsKey(currentType))
						distribution.put(currentType, 0.0);
					distribution.put(currentType, distribution.get(currentType) + countValue);
				}
					
				normalizeDistribution(distribution, totalCount);
				double entropy = computeEntropy(distribution);

				this.typeKey.set(type);
				this.outputValue.set(String.valueOf(entropy * totalCount) + "\t" + totalCount);
				context.write(this.typeKey, this.outputValue);
			}
						
		}
		
		private void normalizeDistribution(Map<String, Double> distribution, double normalizer) {
			for (Entry<String, Double> entry : distribution.entrySet())
				entry.setValue(entry.getValue() / normalizer);
		} 
		
		private double computeEntropy(Map<String, Double> distribution) {
			double entropy = 0.0;
			
			for (Double p : distribution.values()) {
				if (p == 0)
					continue;
				entropy -= p*Math.log(p)/Math.log(2);
			}
				
			return entropy;
		}
	}
	
	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {
		private Text outputValue = new Text();
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			double totalEntropy = 0.0;
			double totalCount = 0.0;
			
			for (Text value : values) {
				String[] valueParts = value.toString().split("\t");
				totalEntropy += Double.valueOf(valueParts[0]);
				totalCount += Double.valueOf(valueParts[1]);
			}
			

			double normalizedEntropy = totalEntropy / totalCount;
			this.outputValue.set(normalizedEntropy + "\t" + totalEntropy);
			
			context.write(key, this.outputValue);
		}
	}
}
