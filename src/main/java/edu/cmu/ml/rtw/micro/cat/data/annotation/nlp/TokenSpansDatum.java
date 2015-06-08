package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNer;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.model.evaluation.metric.SupervisedModelEvaluationCategoryListFreebase;
import edu.cmu.ml.rtw.micro.cat.model.evaluation.metric.SupervisedModelEvaluationPolyAccuracy;
import edu.cmu.ml.rtw.micro.cat.model.evaluation.metric.SupervisedModelEvaluationPolyF;
import edu.cmu.ml.rtw.micro.cat.model.evaluation.metric.SupervisedModelEvaluationPolyPrecision;
import edu.cmu.ml.rtw.micro.cat.model.evaluation.metric.SupervisedModelEvaluationPolyRecall;
import edu.cmu.ml.rtw.micro.cat.model.evaluation.metric.SupervisedModelEvaluationPolysemy;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;


public class TokenSpansDatum<L> extends Datum<L> {
	private TokenSpan[] tokenSpans;
	private boolean polysemous;
	
	public TokenSpansDatum(int id, TokenSpan[] tokenSpans, L label, boolean polysemous) {
		this.id = id;
		this.tokenSpans = tokenSpans;
		this.label = label;
		this.polysemous = polysemous;
	}
	
	public TokenSpansDatum(int id, List<TokenSpan> tokenSpans, L label, boolean polysemous) {
		this(id, tokenSpans.toArray(new TokenSpan[] {}), label, polysemous);
	}
	
	public TokenSpansDatum(int id, TokenSpan tokenSpan, L label, boolean polysemous) {
		this(id, new TokenSpan[] { tokenSpan }, label, polysemous);
	}
	
	public <S> TokenSpansDatum(TokenSpansDatum<S> datum, L label, boolean polysemous) {
		this(datum.id, datum.tokenSpans, label, polysemous);
	}
	
	public TokenSpansDatum(int id, TokenSpansDatum<L> datum1, TokenSpansDatum<L> datum2, L label, boolean polysemous) {
		this.id = id;
		
		this.tokenSpans = new TokenSpan[datum1.tokenSpans.length + datum2.tokenSpans.length];
		for (int i = 0; i < datum1.tokenSpans.length; i++)
			this.tokenSpans[i] = datum1.tokenSpans[i];
		for (int i = 0; i < datum2.tokenSpans.length; i++)
			this.tokenSpans[datum1.tokenSpans.length + i] = datum2.tokenSpans[i];
	
		this.label = label;
		this.polysemous = polysemous;
	}
	
	public TokenSpansDatum(int id, Collection<TokenSpansDatum<L>> datums, L label, boolean polysemous) {
		this.id = id;
		
		int numTokenSpans = 0;
		for (TokenSpansDatum<L> datum : datums)
			numTokenSpans += datum.getTokenSpans().length;
		
		this.tokenSpans = new TokenSpan[numTokenSpans];
		int i = 0;
		for (TokenSpansDatum<L> datum : datums) {
			for (TokenSpan tokenSpan : datum.tokenSpans) {
				this.tokenSpans[i] = tokenSpan;
				i++;
			}
		}
	
		this.label = label;
		this.polysemous = polysemous;
	}
	
	public TokenSpan[] getTokenSpans() {
		return this.tokenSpans;
	}
	
	public boolean isPolysemous() {
		return this.polysemous;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(": ");
		
		for (TokenSpan tokenSpan : this.tokenSpans)
			str.append(tokenSpan.toString()).append(", ");
			
		return str.toString();
	}
	
	public static Tools<String> getStringTools(DataTools dataTools) {
		Tools<String> tools =  new Tools<String>(dataTools) {
			@Override
			public String labelFromString(String str) {
				return str;
			}
		};
	
		return tools;
	}
	
	public static Tools<CategoryList> getCategoryListTools(DataTools dataTools) {
		Tools<CategoryList> tools =  new Tools<CategoryList>(dataTools) {
			@Override
			public CategoryList labelFromString(String str) {
				if (str == null)
					return null;
				
				return CategoryList.fromString(str, (CatDataTools)dataTools);
			}
		};
		final NELLUtil nell = new NELLUtil((CatDataTools)dataTools);
		
		tools.addGenericEvaluation(new SupervisedModelEvaluationPolysemy());
		tools.addGenericEvaluation(new SupervisedModelEvaluationCategoryListFreebase());
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<CategoryList>() {
			public String toString() {
				return "Weighted";
			}
			
			@Override
			public CategoryList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				return new CategoryList(indicatorWeights);
			}
		});
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<CategoryList>() {
			public String toString() {
				return "Unweighted";
			}
			
			@Override
			public CategoryList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				List<Pair<String, Double>> weightedLabels = new ArrayList<Pair<String, Double>>(indicatorWeights.size());
				for (String positiveIndicator : positiveIndicators) {
					weightedLabels.add(new Pair<String, Double>(positiveIndicator, 1.0));
				}
				return new CategoryList(weightedLabels);
			}
		});
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<CategoryList>() {
			public String toString() {
				return "UnweightedGeneralized";
			}
			
			@Override
			public CategoryList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				Map<String, Double> generalizedIndicators = new TreeMap<String, Double>();
				for (String positiveIndicator : positiveIndicators) {
					generalizedIndicators.put(positiveIndicator, 1.0);
					Set<String> generalizations = nell.getCategoryGeneralizations(positiveIndicator);
					for (String generalization : generalizations)
						generalizedIndicators.put(generalization, 1.0);
				}
				return new CategoryList(generalizedIndicators);
			}
		});
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<CategoryList>() {
			public String toString() {
				return "UnweightedConstrained";
			}
			
			@Override
			public CategoryList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				Set<String> constrainedIndicators = new HashSet<String>();
				List<Pair<String, Double>> sortedWeights = new ArrayList<Pair<String, Double>>();
				for (Entry<String, Double> entry : indicatorWeights.entrySet())
					sortedWeights.add(new Pair<String, Double>(entry.getKey(), entry.getValue()));
				Set<String> positiveIndicatorSet = new HashSet<String>();
				positiveIndicatorSet.addAll(positiveIndicators);
				
				Collections.sort(sortedWeights, new Comparator<Pair<String, Double>>() {
					@Override
					public int compare(Pair<String, Double> p0,
							Pair<String, Double> p1) {
						if (p0.getSecond() > p1.getSecond())
							return -1;
						else if (p0.getSecond() < p1.getSecond())
							return 1;
						else 
							return 0;
					}
					
				});
				
				for (Pair<String, Double> weightedIndicator : sortedWeights) {
					if (!positiveIndicatorSet.contains(weightedIndicator.getFirst())
						|| constrainedIndicators.contains(weightedIndicator.getFirst()))
						continue;
					
					constrainedIndicators.add(weightedIndicator.getFirst());
					if (nell.areCategoriesMutuallyExclusive(constrainedIndicators)) {
						constrainedIndicators.remove(weightedIndicator.getFirst());
						continue;
					}
				
					constrainedIndicators.addAll(nell.getCategoryGeneralizations(weightedIndicator.getFirst()));
				}
				
				List<Pair<String, Double>> weightedLabels = new ArrayList<Pair<String, Double>>(constrainedIndicators.size());
				for (String constrainedIndicator : constrainedIndicators) {
					weightedLabels.add(new Pair<String, Double>(constrainedIndicator, 1.0));
				}
				
				return new CategoryList(weightedLabels);
			}
		});
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<CategoryList>() {
			public String toString() {
				return "WeightedGeneralized";
			}
			
			@Override
			public CategoryList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				Map<String, Double> generalizedWeights = new TreeMap<String, Double>();
				for (String positiveIndicator : positiveIndicators) {
					if (!generalizedWeights.containsKey(positiveIndicator))
						generalizedWeights.put(positiveIndicator, indicatorWeights.get(positiveIndicator));
					else {
						generalizedWeights.put(positiveIndicator, Math.max(indicatorWeights.get(positiveIndicator), generalizedWeights.get(positiveIndicator)));
					}
					
					Set<String> generalizations = nell.getCategoryGeneralizations(positiveIndicator);
					for (String generalization : generalizations) {
						if (!generalizedWeights.containsKey(generalization))
							generalizedWeights.put(generalization, indicatorWeights.get(positiveIndicator));
						else
							generalizedWeights.put(generalization, Math.max(
																		(indicatorWeights.containsKey(generalization) ? indicatorWeights.get(generalization) : 0.0), 
																		generalizedWeights.get(generalization)));
					}
				}
				
				return new CategoryList(generalizedWeights);
			}
		});
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<CategoryList>() {
			public String toString() {
				return "WeightedConstrained";
			}
			
			@Override
			public CategoryList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				List<Pair<String, Double>> sortedWeights = new ArrayList<Pair<String, Double>>();
				for (Entry<String, Double> entry : indicatorWeights.entrySet())
					sortedWeights.add(new Pair<String, Double>(entry.getKey(), entry.getValue()));
				Set<String> positiveIndicatorSet = new HashSet<String>();
				positiveIndicatorSet.addAll(positiveIndicators);
				
				Collections.sort(sortedWeights, new Comparator<Pair<String, Double>>() {
					@Override
					public int compare(Pair<String, Double> p0,
							Pair<String, Double> p1) {
						if (p0.getSecond() > p1.getSecond())
							return -1;
						else if (p0.getSecond() < p1.getSecond())
							return 1;
						else 
							return 0;
					}
					
				});
				
				Map<String, Double> constrainedWeights = new TreeMap<String, Double>();
				for (Pair<String, Double> weightedIndicator : sortedWeights) {
					if (!positiveIndicatorSet.contains(weightedIndicator.getFirst()))
						continue;
					
					if (!constrainedWeights.containsKey(weightedIndicator.getFirst()))
						constrainedWeights.put(weightedIndicator.getFirst(), weightedIndicator.getSecond());
					else {
						constrainedWeights.put(weightedIndicator.getFirst(), Math.max(weightedIndicator.getSecond(), constrainedWeights.get(weightedIndicator.getFirst())));
					}
					
					if (nell.areCategoriesMutuallyExclusive(constrainedWeights.keySet())) {
						constrainedWeights.remove(weightedIndicator.getFirst());
						continue;
					}
					
					Set<String> generalizations = nell.getCategoryGeneralizations(weightedIndicator.getFirst());
					for (String generalization : generalizations) {
						if (!constrainedWeights.containsKey(generalization))
							constrainedWeights.put(generalization, weightedIndicator.getSecond());
						else
							constrainedWeights.put(generalization, Math.max(weightedIndicator.getSecond(), constrainedWeights.get(generalization)));
					}
				}
				
				return new CategoryList(constrainedWeights);
			}
		});
		
		return tools;
	}
	
	public static Tools<Boolean> getBooleanTools(DataTools dataTools) {
		Tools<Boolean> tools =  new Tools<Boolean>(dataTools) {
			@Override
			public Boolean labelFromString(String str) {
				if (str == null)
					return null;
				return str.toLowerCase().equals("true") || str.equals("1");
			}
		};
	
		return tools;
	}
	
	public static abstract class Tools<L> extends Datum.Tools<TokenSpansDatum<L>, L> { 
		public Tools(DataTools dataTools) {
			super(dataTools);
			final NELLUtil nell = new NELLUtil((CatDataTools)dataTools);
			PoSTag[][] NPTagClass = { PoSTagClass.NNP, PoSTagClass.NN };
			
			this.addGenericFeature(new FeatureNer<TokenSpansDatum<L>, L>());
			
			this.addGenericEvaluation(new SupervisedModelEvaluationPolyAccuracy<L>());
			this.addGenericEvaluation(new SupervisedModelEvaluationPolyF<L>());
			this.addGenericEvaluation(new SupervisedModelEvaluationPolyPrecision<L>());
			this.addGenericEvaluation(new SupervisedModelEvaluationPolyRecall<L>());
			
			this.addStringExtractor(new StringExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstTokenSpan";
				}
				
				@Override 
				public String[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					System.out.println("FirstTokenSpan Extracting " + tokenSpansDatum.getTokenSpans()[0].toString() );
					return new String[] { tokenSpansDatum.getTokenSpans()[0].toString() };
				}
			});
			
			this.addStringExtractor(new StringExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "SentenceNELLNounPhrases";
				}
				
				@Override 
				public String[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					TokenSpan[] tokenSpans = tokenSpansDatum.getTokenSpans();
					Set<String> nps = new HashSet<String>();
					for (int i = 0; i < tokenSpans.length; i++) {
						int sentenceIndex = tokenSpans[i].getSentenceIndex();
						List<TokenSpan> npSpans = nell.extractNounPhrases(tokenSpans[i].getDocument(), sentenceIndex);
						for (TokenSpan npSpan : npSpans) {
							boolean npSpanInDatum = false;
							
							for (int j = 0; j < tokenSpans.length; j++) {
								if (tokenSpans[j].containsToken(sentenceIndex, npSpan.getStartTokenIndex())
										|| tokenSpans[j].containsToken(sentenceIndex, npSpan.getEndTokenIndex() - 1)
										|| npSpan.containsToken(tokenSpans[j].getSentenceIndex(), tokenSpans[j].getStartTokenIndex())
										|| npSpan.containsToken(tokenSpans[j].getSentenceIndex(), tokenSpans[j].getEndTokenIndex() - 1)) {
									npSpanInDatum = true;
									break;
								}
							}
							
							if (!npSpanInDatum)
								nps.add(npSpan.toString());
						}
					}
					
					return nps.toArray(new String[0]);
				}
			});
			
			this.addStringExtractor(new StringExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "DocumentNELLNounPhrases";
				}
				
				@Override 
				public String[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					TokenSpan[] tokenSpans = tokenSpansDatum.getTokenSpans();
					Set<String> nps = new HashSet<String>();
					for (int i = 0; i < tokenSpans.length; i++) {
						List<TokenSpan> npSpans = nell.extractNounPhrases(tokenSpans[i].getDocument());
						for (TokenSpan npSpan : npSpans) {
							boolean npSpanInSentence = false;
							
							for (int j = 0; j < tokenSpans.length; j++) {
								if (tokenSpans[j].getSentenceIndex() == npSpan.getSentenceIndex()) {
									npSpanInSentence = true;
									break;
								}
							}
							
							if (!npSpanInSentence)
								nps.add(npSpan.toString());
						}
					}
					
					return nps.toArray(new String[0]);
				}
			});
			
			
			this.addStringExtractor(new StringExtractorNGramPoSTag("AllSentenceUnigramsNP", NPTagClass, false, 1));
			this.addStringExtractor(new StringExtractorNGramPoSTag("AllDocumentUnigramsNP", NPTagClass, true, 1));
			this.addStringExtractor(new StringExtractorNGramPoSTag("AllSentenceBigramsNP", NPTagClass, false, 2));
			this.addStringExtractor(new StringExtractorNGramPoSTag("AllDocumentBigramsNP", NPTagClass, true, 2));
			this.addStringExtractor(new StringExtractorNGramPoSTag("AllSentenceTrigramsNP", NPTagClass, false, 3));
			this.addStringExtractor(new StringExtractorNGramPoSTag("AllDocumentTrigramsNP", NPTagClass, true, 3));
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "AllDocumentSentenceInitialTokens";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					Set<String> documents = new HashSet<String>();
					List<TokenSpan> sentenceInitialTokens = new ArrayList<TokenSpan>();
					
					for (TokenSpan tokenSpan : tokenSpansDatum.tokenSpans) {
						if (documents.contains(tokenSpan.getDocument().getName()))
							continue;
						DocumentNLP document = tokenSpan.getDocument();
						int sentenceCount = document.getSentenceCount();
						for (int i = 0; i < sentenceCount; i++) {
							if (document.getSentenceTokenCount(i) <= 0 || i == tokenSpan.getSentenceIndex())
								continue;
							sentenceInitialTokens.add(new TokenSpan(document, i, 0, 1));
						}
							
					}
					
					return sentenceInitialTokens.toArray(new TokenSpan[0]);
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "AllTokenSpans";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					return tokenSpansDatum.tokenSpans;
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstTokenSpan";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					return new TokenSpan[] { tokenSpansDatum.tokenSpans[0] };
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "LastTokenSpan";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					return new TokenSpan[] { tokenSpansDatum.tokenSpans[tokenSpansDatum.tokenSpans.length - 1] };
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstTokenSpanLastToken";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					TokenSpan tokenSpan = tokenSpansDatum.tokenSpans[tokenSpansDatum.tokenSpans.length - 1];
					return new TokenSpan[] { tokenSpan.getSubspan(tokenSpan.getLength()-1, tokenSpan.getLength()) };
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstTokenSpanNotLastToken";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					TokenSpan tokenSpan = tokenSpansDatum.tokenSpans[tokenSpansDatum.tokenSpans.length - 1];
					return new TokenSpan[] { tokenSpan.getSubspan(0, tokenSpan.getLength()-1) };
				}
			});
		}
		
		@Override
		public TokenSpansDatum<L> datumFromJSON(JSONObject json) {
			try {
				int id = json.getInt("id");
				boolean polysemous = json.getBoolean("polysemous");
				L label = (json.has("label")) ? labelFromString(json.getString("label")) : null;
				JSONArray jsonTokenSpans = json.getJSONArray("tokenSpans");
				List<TokenSpan> tokenSpans = new ArrayList<TokenSpan>();
				for (int i = 0; i < jsonTokenSpans.length(); i++) {
					tokenSpans.add(TokenSpan.fromJSON(jsonTokenSpans.getJSONObject(i), ((CatDataTools)this.dataTools).getDocumentSet()));
				}
				
				return new TokenSpansDatum<L>(id, tokenSpans, label, polysemous);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		public JSONObject datumToJSON(TokenSpansDatum<L> datum) {
			JSONObject json = new JSONObject();
			
			try {
				json.put("id", datum.id);
				json.put("polysemous", datum.polysemous);
				if (datum.label != null)
					json.put("label", datum.label.toString());
				
				JSONArray tokenSpans = new JSONArray();
				for (TokenSpan tokenSpan : datum.tokenSpans) {
					tokenSpans.put(tokenSpan.toJSON(true));
				}
				
				json.put("tokenSpans", tokenSpans);
				if (tokenSpans.length() > 0)
				json.put("str", datum.tokenSpans[0].toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return json;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> T makeBinaryDatum(
				TokenSpansDatum<L> datum,
				LabelIndicator<L> labelIndicator) {
			
			TokenSpansDatum<Boolean> binaryDatum = new TokenSpansDatum<Boolean>(datum.getId(), datum.getTokenSpans(), (labelIndicator == null || datum.getLabel() == null) ? null : labelIndicator.indicator(datum.getLabel()), datum.isPolysemous());
			
			if (labelIndicator != null && datum.getLabel() != null) {
				double labelWeight = labelIndicator.weight(datum.getLabel());
				binaryDatum.setLabelWeight(true, labelWeight);
			}
			
			return (T)(binaryDatum);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> Datum.Tools<T, Boolean> makeBinaryDatumTools(
				LabelIndicator<L> labelIndicator) {
			OutputWriter genericOutput = this.dataTools.getOutputWriter();
			OutputWriter output = new OutputWriter(
					(genericOutput.getDebugFilePath() != null) ? new File(genericOutput.getDebugFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getResultsFilePath() != null) ? new File(genericOutput.getResultsFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getDataFilePath() != null) ? new File(genericOutput.getDataFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getModelFilePath() != null) ? new File(genericOutput.getModelFilePath() + "." + labelIndicator.toString()) : null
				);
			
			CatDataTools dataTools = new CatDataTools(output, (CatDataTools)this.dataTools);
			dataTools.setRandomSeed(this.dataTools.getGlobalRandom().nextLong());
			return (Datum.Tools<T, Boolean>)TokenSpansDatum.getBooleanTools(dataTools);
		}
		
		private class StringExtractorNGramPoSTag implements StringExtractor<TokenSpansDatum<L>, L> {
			private String name;
			private PoSTag[][] posTags;
			private boolean fullDocument;
			private int n;
			
			public StringExtractorNGramPoSTag(String name, PoSTag[][] posTags, boolean fullDocument, int n) {
				this.name = name;
				this.posTags = posTags;
				this.fullDocument = fullDocument;
				this.n = n;
			}
			
			@Override
			public String toString() {
				return this.name;
			}
			
			@Override
			public String[] extract(TokenSpansDatum<L> datum) {
				TokenSpan[] tokenSpans = datum.getTokenSpans();
				Set<String> strs = new HashSet<String>();
				for (TokenSpan tokenSpan : tokenSpans) {
					if (this.fullDocument) {
						DocumentNLP document = tokenSpan.getDocument();
						
						for (int sentenceIndex = 0; sentenceIndex < document.getSentenceCount(); sentenceIndex++) {
							if (sentenceIndex != tokenSpan.getSentenceIndex())
								extractForSentence(document, sentenceIndex, strs);
						}
					} else {
						extractForSentence(tokenSpan.getDocument(), tokenSpan.getSentenceIndex(), strs);
					}
				}
				
				return strs.toArray(new String[0]);
			}
			
			private boolean extractForSentence(DocumentNLP document, int sentenceIndex, Set<String> strs) {
				int sentenceTokenCount = document.getSentenceTokenCount(sentenceIndex);
				for (int i = 0; i < sentenceTokenCount - this.n + 1; i++) {
					if (!ngramHasPosTag(document, sentenceIndex, i))
						continue;
					StringBuilder ngram = new StringBuilder();
					for (int j = i; j < i + this.n; j++) {
						ngram.append(document.getToken(sentenceIndex, j)).append(" ");
					}
					
					String ngramStr = ngram.toString().trim();
					
					strs.add(ngramStr);
				}
				
				return true;
			}
			
			private boolean ngramHasPosTag(DocumentNLP document, int sentenceIndex, int tokenIndex) {
				for (int i = tokenIndex; i < tokenIndex + this.n; i++) {
					for (PoSTag[] posTagClass : this.posTags) {
						PoSTag posTag = document.getPoSTag(sentenceIndex, i);
						if (PoSTagClass.classContains(posTagClass, posTag))
							return true;
					}
				}
				
				return false;
			}
		}
	}

}
