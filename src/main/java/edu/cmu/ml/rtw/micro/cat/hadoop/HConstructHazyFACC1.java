package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.Text;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.FACC1Annotation;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.FACC1DocumentNLPInMemory;

public class HConstructHazyFACC1 {
	public static class Mapper extends HRun.PolyMapper<Object, Text, Text, Text> {
		private Text cluewebDocumentKey = new Text();
		private Text value = new Text();

		private Pattern FACC1TypePattern = Pattern.compile("clueweb09-([^\t]*)\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*");
		private Pattern hazyPattern = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\tclueweb09-([^:]*).*");

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String valueStr = value.toString();
			Matcher FACC1TypeMatcher = this.FACC1TypePattern.matcher(valueStr);
			Matcher hazyMatcher = this.hazyPattern.matcher(valueStr);
			
			this.value.set(valueStr);
			
			if (FACC1TypeMatcher.matches()) {
				String cluewebDocument = FACC1TypeMatcher.group(1);
				this.cluewebDocumentKey.set(cluewebDocument);
			} else if (hazyMatcher.matches()) {
				String cluewebDocument = hazyMatcher.group(1);
				this.cluewebDocumentKey.set(cluewebDocument);
			} else {
				return; // Ignore irrelevant row
			}
			
			context.write(this.cluewebDocumentKey, this.value);
		}
	}
	
	public static class Reducer extends HRun.PolyReducer<Text, Text, Text, Text> {
		private Text outputValue = new Text();
		
		private static class HazyTokenAnnotation {
			private int sentenceIndex;
			private int tokenIndex;
			private String token;
			private PoSTag posTag;
			private String dependencyParentType;
			private int dependencyParentToken;
			private String nerEntity;
			private int startByte;
			private int endByte;
			
			private static Pattern hazyPattern = Pattern.compile("([^\t]*)\t([^\t]*)\t[^\t]*\t([^\t]*)\t[^\t]*\tclueweb09-[^:]*:([^:]*):([^:]*):([^:]*):([^\t]*)\t([^\t]*)\t([^\t]*)\t[^\t]*\t[^\t]*");
			
			public static HazyTokenAnnotation fromString(String str) {
				Matcher hazyMatcher = HazyTokenAnnotation.hazyPattern.matcher(str);
				if (!hazyMatcher.matches())
					return null;
				
				HazyTokenAnnotation hazy = new HazyTokenAnnotation();
				
				hazy.tokenIndex = Integer.valueOf(hazyMatcher.group(1))-1; 
				hazy.token = hazyMatcher.group(2);
				
				String posTagStr = hazyMatcher.group(3);
				if (!Character.isLetter(posTagStr.substring(0, 1).toCharArray()[0]))
					hazy.posTag = PoSTag.SYM;
				else
					hazy.posTag = PoSTag.valueOf(hazyMatcher.group(3));
				
				hazy.sentenceIndex = Integer.valueOf(hazyMatcher.group(4))-1; 
				hazy.nerEntity = hazyMatcher.group(5);
				hazy.startByte = Integer.valueOf(hazyMatcher.group(6)); 
				hazy.endByte = Integer.valueOf(hazyMatcher.group(7)); 
				hazy.dependencyParentToken = Integer.valueOf(hazyMatcher.group(8)); 
				hazy.dependencyParentType = hazyMatcher.group(9);
				
				return hazy;
			}
			
			public int getSentenceIndex() {
				return this.sentenceIndex;
			}
			
			public int getTokenIndex() {
				return this.tokenIndex;
			}
			
			public String getToken() {
				return this.token;
			}
			
			public PoSTag getPoSTag() {
				return this.posTag;
			}
			
			public String getNEREntity() {
				return this.nerEntity;
			}
			
			public int getStartByte() {
				return this.startByte;
			}
			
			public int getEndByte() {
				return this.endByte;
			}
			
			public int getDependencyParentToken() {
				return this.dependencyParentToken;
			}
			
			public String getDependencyParentType() {
				return this.dependencyParentType;
			}
		}
		
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			String documentName = key.toString(); 
			Map<Integer, Map<Integer, HazyTokenAnnotation>> hazyAnnotations = new HashMap<Integer, Map<Integer, HazyTokenAnnotation>>();
			Map<Integer, Integer>  maxTokenIndices = new HashMap<Integer, Integer>();
			int maxSentenceIndex = 0;
			List<FACC1Annotation> facc1Annotations = new ArrayList<FACC1Annotation>();
			
			for (Text value : values) {
				String valueStr = value.toString();
				HazyTokenAnnotation hazyToken = HazyTokenAnnotation.fromString(valueStr);
				
				if (hazyToken != null) {
					int sentenceIndex = hazyToken.getSentenceIndex();
					int tokenIndex = hazyToken.getTokenIndex();
					
					if (!hazyAnnotations.containsKey(sentenceIndex))
						hazyAnnotations.put(sentenceIndex, new HashMap<Integer, HazyTokenAnnotation>());
					hazyAnnotations.get(sentenceIndex).put(tokenIndex, hazyToken);
					
					if (!maxTokenIndices.containsKey(sentenceIndex) || tokenIndex > maxTokenIndices.get(sentenceIndex))
						maxTokenIndices.put(sentenceIndex, tokenIndex);
				
					if (sentenceIndex > maxSentenceIndex)
						maxSentenceIndex = sentenceIndex;
				} else {
					FACC1Annotation facc1Annotation = FACC1Annotation.fromString(valueStr);
					if (facc1Annotation == null)
						continue;
					facc1Annotations.add(facc1Annotation);
				}
			}
			
			if (hazyAnnotations.size() == 0 || facc1Annotations.size() == 0)
				return;
			
			int numSentences = maxSentenceIndex + 1;
			String[][] tokens = new String[numSentences][]; 
			PoSTag[][] posTags = new PoSTag[numSentences][]; 
			String[] dependencyParseStrs = new String[numSentences];
			String[][] nerEntities = new String[numSentences][];
			int[][] startBytes = new int[numSentences][];
			int[][] endBytes = new int[numSentences][];
			
			for (int i = 0; i < numSentences; i++) {
				Map<Integer, HazyTokenAnnotation> sentenceAnnotations = hazyAnnotations.get(i);
				if (sentenceAnnotations == null) {
					tokens[i] = new String[0];
					posTags[i] = new PoSTag[0];
					nerEntities[i] = new String[0];
					startBytes[i] = new int[0];
					endBytes[i] = new int[0];
					dependencyParseStrs[i] = "";
					
					continue;
				}
				
				int numTokens = maxTokenIndices.get(i) + 1;
				tokens[i] = new String[numTokens];
				posTags[i] = new PoSTag[numTokens];
				nerEntities[i] = new String[numTokens];
				startBytes[i] = new int[numTokens];
				endBytes[i] = new int[numTokens];
				
				StringBuilder dependencyParseStr = new StringBuilder();
				for (int j = 0; j < numTokens; j++) {
					HazyTokenAnnotation tokenAnnotation = sentenceAnnotations.get(j);
					tokens[i][j] = tokenAnnotation.getToken();
					posTags[i][j] = tokenAnnotation.getPoSTag();
					
					if (!tokenAnnotation.getNEREntity().equals("O"))
						nerEntities[i][j] = tokenAnnotation.getNEREntity();
					
					startBytes[i][j] = tokenAnnotation.getStartByte();
					endBytes[i][j] = tokenAnnotation.getEndByte();
					
					dependencyParseStr.append((tokenAnnotation.getDependencyParentType().equals("null")) ? 
							"ROOT" 
							: tokenAnnotation.getDependencyParentType());
					
					dependencyParseStr.append("(g-");
					dependencyParseStr.append(tokenAnnotation.getDependencyParentToken());
					dependencyParseStr.append(", ");
					dependencyParseStr.append("d-");
					dependencyParseStr.append(tokenAnnotation.getTokenIndex() + 1);
					dependencyParseStr.append(")");
					if (j != numTokens - 1)
						dependencyParseStr.append("\n");
				}
				
				dependencyParseStrs[i] = dependencyParseStr.toString();
			}
			
			FACC1DocumentNLPInMemory hazyDocument = 
					new FACC1DocumentNLPInMemory(
							this.dataTools,
							documentName, 
							Language.English, 
							tokens, 
							posTags, 
							dependencyParseStrs,
							nerEntities,
							startBytes,
							endBytes,
							facc1Annotations);
			
			this.outputValue.set(hazyDocument.toJSON().toString());
			context.write(key, this.outputValue);
		}
	}
}
