package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.DocumentSetNLPFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLDataSetFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;

public class ConstructAnnotationData {
	private static String categoriesStr;
	private static int randomSeed;
	private static double nellConfidenceThreshold;
	private static int nonPolysemousExamplesPerLabel; 
	private static int polysemousTestExamples;
	private static int lowConfidenceTestExamples;
	private static int noBeliefTestExamples;
	private static String nonPolysemousDataSetName;
	private static DocumentSetNLPFactory.SetName documentSetName;
	private static int maxThreads;
	private static File featuresFile;
	private static String modelFilePathPrefix;
	private static String outputFilePathPrefix;
	
	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		NELLDataSetFactory.MentionDataSetCollection dataSets = 
				new NELLDataSetFactory.MentionDataSetCollection(randomSeed, 
																categoriesStr, 
																documentSetName, 
																nellConfidenceThreshold, 
																lowConfidenceTestExamples, 
																noBeliefTestExamples, 
																polysemousTestExamples, 
																nonPolysemousDataSetName, 
																nonPolysemousExamplesPerLabel);
		
		dataSets.getDatumTools().getDataTools().getOutputWriter().setDataFile(new File(outputFilePathPrefix + ".data.out"), false);
		dataSets.getDatumTools().getDataTools().getOutputWriter().setDebugFile(new File(outputFilePathPrefix + ".debug.out"), false);
		
		NELLMentionCategorizer categorizer = new NELLMentionCategorizer(dataSets.getDatumTools(), 
																		dataSets.getCategories(), 
																		Double.MAX_VALUE, 
																		NELLMentionCategorizer.LabelType.WEIGHTED_CONSTRAINED, 
																		featuresFile, 
																		modelFilePathPrefix, 
																		maxThreads);
		
		constructAnnotationsForData("lc", dataSets.getCategories(), categorizer, maxThreads, nellConfidenceThreshold, dataSets.getLowConfidenceTestData());
		constructAnnotationsForData("nb", dataSets.getCategories(), categorizer, maxThreads, nellConfidenceThreshold, dataSets.getNoBeliefTestData());
		constructAnnotationsForData("hc_poly", dataSets.getCategories(), categorizer, maxThreads, nellConfidenceThreshold, dataSets.getPolysemousTestData());
		constructAnnotationsForData("hc_nonpoly", dataSets.getCategories(), categorizer, maxThreads, nellConfidenceThreshold, dataSets.getNonPolysemousTestData());
	}
	
	private static void constructAnnotationsForData(final String name, CategoryList categories, NELLMentionCategorizer categorizer, int maxThreads, double nellConfidenceThreshold, DataSet<TokenSpansDatum<CategoryList>, CategoryList> data) {
		final DataSet<TokenSpansDatum<CategoryList>, CategoryList> mentionLabeledData = categorizer.categorizeNounPhraseMentions(data, true);
		final DataSet<TokenSpansDatum<CategoryList>, CategoryList> nellLabeledData = nellLabelData(data, maxThreads, nellConfidenceThreshold);
		final OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();
		
		output.debugWriteln("Constructing " + name + " data (" + data.size() + ")");
		
		ThreadMapper<String, Pair<String, Integer>> threads = new ThreadMapper<String, Pair<String, Integer>>(new Fn<String, Pair<String, Integer>>() {
			public Pair<String, Integer> apply(String label) {
				LabelIndicator<CategoryList> labelIndicator = nellLabeledData.getDatumTools().getLabelIndicator(label);
				Datum.Tools<TokenSpansDatum<Boolean>, Boolean> binaryTools = nellLabeledData.getDatumTools().makeBinaryDatumTools(labelIndicator);
				Context<TokenSpansDatum<Boolean>, Boolean> binaryContext = new Context<TokenSpansDatum<Boolean>, Boolean>(binaryTools);
				DataSet<TokenSpansDatum<Boolean>, Boolean> binaryData = nellLabeledData.makeBinary(labelIndicator, binaryContext);
				DataSet<TokenSpansDatum<Boolean>, Boolean> mentionLabeledBinaryData = mentionLabeledData.makeBinary(labelIndicator, binaryContext);
				int predictionCount = 0;
				List<Pair<TokenSpansDatum<Boolean>, Double>> scoredDatums = new ArrayList<Pair<TokenSpansDatum<Boolean>, Double>>();
				
				for (TokenSpansDatum<Boolean> datum : binaryData) {
					boolean labelValue = datum.getLabel();
					boolean mentionLabeledValue = mentionLabeledBinaryData.getDatumById(datum.getId()).getLabel();
					
					if (mentionLabeledValue)
						predictionCount++;
					
					if (labelValue != mentionLabeledValue) {
						double weight = mentionLabeledData.getDatumById(datum.getId()).getLabel().getCategoryWeight(label);
						double confidence = Math.abs(weight - 0.5);
						scoredDatums.add(new Pair<TokenSpansDatum<Boolean>, Double>(datum, 
								confidence));
					}
				}
				
				scoredDatums.sort(new Comparator<Pair<TokenSpansDatum<Boolean>, Double>>() {
					@Override
					public int compare(Pair<TokenSpansDatum<Boolean>, Double> arg0,
							Pair<TokenSpansDatum<Boolean>, Double> arg1) {
						if (arg0.getSecond() > arg1.getSecond())
							return -1;
						else if (arg0.getSecond() < arg1.getSecond())
							return 1;
						else
							return 0;
					}
				});

				OutputWriter labelOutput = new OutputWriter(
						new File(output.getDebugFilePath() + "." + name + "." + label), 
						null, 
						new File(output.getDataFilePath() + "." + name + "." + label), 
						null);
				
				for (Pair<TokenSpansDatum<Boolean>, Double> scoredDatum : scoredDatums) {
					TokenSpansDatum<Boolean> datum = scoredDatum.getFirst();
					TokenSpan span = datum.getTokenSpans()[0];
					String mentionStr = span.toString();
					String sentenceStr = getSpanSurroundingSentences(span);
					String idStr = String.valueOf(datum.getId());
					String spanJsonStr = span.toJSON(true).toString();
					
					StringBuilder annotationLine = new StringBuilder();
					annotationLine.append("\t");
					annotationLine.append(mentionStr);
					annotationLine.append("\t");
					annotationLine.append(sentenceStr);
					annotationLine.append("\t");
					annotationLine.append(idStr);
					annotationLine.append("\t");
					annotationLine.append(spanJsonStr);
					
					labelOutput.dataWriteln(annotationLine.toString());
				}
				
				labelOutput.close();
				
				return new Pair<String, Integer>(label, predictionCount);
			}
		});
		
		List<Pair<String, Integer>> labelCounts = threads.run(Arrays.asList(categories.getCategories()), maxThreads);
		labelCounts.sort(new Comparator<Pair<String, Integer>>() {
			@Override
			public int compare(Pair<String, Integer> arg0,
					Pair<String, Integer> arg1) {
				if (arg0.getSecond() > arg1.getSecond())
					return -1;
				else if (arg0.getSecond() < arg1.getSecond())
					return 1;
				else
					return 0;
			}
		});
		
		output.dataWriteln("\n" + name + " prediction counts");
		for (Pair<String, Integer> labelCount : labelCounts)
			output.dataWriteln(labelCount.getFirst() + "\t" + labelCount.getSecond());
	}
	
	private static DataSet<TokenSpansDatum<CategoryList>, CategoryList> nellLabelData(final DataSet<TokenSpansDatum<CategoryList>, CategoryList> data, int maxThreads, final double nellConfidenceThreshold) {
		final DataSet<TokenSpansDatum<CategoryList>, CategoryList> labeledData = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(data.getDatumTools(), null);
		final NELLUtil nell = new NELLUtil((CatDataTools)data.getDatumTools().getDataTools());
		
		labeledData.addAll(data.map(new Fn<TokenSpansDatum<CategoryList>, TokenSpansDatum<CategoryList>>(){

			@Override
			public TokenSpansDatum<CategoryList> apply(
					TokenSpansDatum<CategoryList> item) {
				List<Pair<String, Double>> weightedCategories = nell.getNounPhraseNELLWeightedCategories(item.getTokenSpans()[0].toString(), nellConfidenceThreshold);
				List<String> positiveIndicators = new ArrayList<String>(weightedCategories.size());
				Map<String, Double> labelWeights = new HashMap<String, Double>();
				for (Pair<String, Double> weightedCategory : weightedCategories) {
					positiveIndicators.add(weightedCategory.getFirst());
					labelWeights.put(weightedCategory.getFirst(), weightedCategory.getSecond());
				}
				
				CategoryList label = data.getDatumTools().getInverseLabelIndicator("UnweightedConstrained").label(labelWeights, positiveIndicators);

				return new TokenSpansDatum<CategoryList>(item, label, false);
			}
			
		}, maxThreads));
	
		return labeledData;
	}
	
	private static String getSpanSurroundingSentences(TokenSpan span) {
		int startSentenceIndex = Math.max(0, span.getSentenceIndex() - 1);
		int endSentenceIndex = Math.min(span.getDocument().getSentenceCount(), span.getSentenceIndex() + 2);
		StringBuilder str = new StringBuilder();
		DocumentNLP document = span.getDocument();
		
		for (int i = startSentenceIndex; i < endSentenceIndex; i++) {
			List<String> tokens = document.getSentenceTokenStrs(i);
			for (int j = 0; j < tokens.size(); j++) {
				if (span.containsToken(i, j)) {
					str.append("__").append(tokens.get(j)).append("__ ");
				} else {
					str.append(tokens.get(j)).append(" ");
				}
			}
		}
		
		return str.toString();
	}
	
	private static boolean parseArgs(String[] args) {
		OptionParser parser = new OptionParser();
		parser.accepts("categories").withRequiredArg()
			.describedAs("ALL_NELL_CATEGORIES, FREEBASE_NELL_CATEGORIES, or a list of categories by which to classify " 
					+ "noun-phrase mentions")
			.defaultsTo("ALL_NELL_CATEGORIES");
		parser.accepts("maxThreads").withRequiredArg()
			.describedAs("Maximum number of threads")
			.ofType(Integer.class)
			.defaultsTo(33);
		parser.accepts("randomSeed").withRequiredArg()
			.describedAs("Seed for random numbers")
			.ofType(Integer.class)
			.defaultsTo(1);
		
		parser.accepts("nellConfidenceThreshold").withRequiredArg()
			.describedAs("Confidence threshold above which NELL's beliefs are used to categorize noun-phrases")
			.ofType(Double.class)
			.defaultsTo(0.9);
		
		parser.accepts("nonPolysemousExamplesForLabel").withRequiredArg()
			.describedAs("Number of non-polysemous examples per category label")
			.ofType(Integer.class)
			.defaultsTo(2000);
		
		parser.accepts("polysemousTestExamples").withRequiredArg()
			.describedAs("Number of polysemous test examples")
			.ofType(Integer.class)
			.defaultsTo(15000);
		
		parser.accepts("lowConfidenceTestExamples").withRequiredArg()
			.describedAs("Number of low-confidence test examples")
			.ofType(Integer.class)
			.defaultsTo(50000);
		
		parser.accepts("noBeliefTestExamples").withRequiredArg()
			.describedAs("Number of no-belief test examples")
			.ofType(Integer.class)
			.defaultsTo(50000);
		
		parser.accepts("nonPolysemousDataSetName").withRequiredArg()
		.describedAs("Name of the non-polysemous dataset" 
				+ "noun-phrase mentions") 
				.defaultsTo("AllNELL_c90_e2000");
		
		parser.accepts("documentSetName").withRequiredArg()
				.describedAs("Name of the source document set") 
				.defaultsTo("HazyFacc1");
		
		parser.accepts("featuresFile").withRequiredArg()
			.describedAs("Path to file containing initialized model features")
			.ofType(File.class);
		
		parser.accepts("modelFilePathPrefix").withRequiredArg()
			.describedAs("Prefix of serialized model file paths");
		
		parser.accepts("outputFilePathPrefix").withRequiredArg()
			.describedAs("Prefix of output file paths");
		
		parser.accepts("help").forHelp();
		
		OptionSet options = parser.parse(args);
		
		if (options.has("help")) {
			try {
				parser.printHelpOn(System.out);
			} catch (IOException e) {
				return false;
			}
			return false;
		}
		
		categoriesStr = options.valueOf("categories").toString();
		maxThreads = (int)options.valueOf("maxThreads");
		randomSeed = (int)options.valueOf("randomSeed");
		nellConfidenceThreshold = (double)options.valueOf("nellConfidenceThreshold");
		nonPolysemousExamplesPerLabel = (int)options.valueOf("nonPolysemousExamplesPerLabel");
		polysemousTestExamples = (int)options.valueOf("polysemousTestExamples");
		lowConfidenceTestExamples = (int)options.valueOf("lowConfidenceTestExamples");
		noBeliefTestExamples = (int)options.valueOf("noBeliefTestExamples");
		nonPolysemousDataSetName = options.valueOf("nonPolysemousDataSetName").toString();
		documentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("documentSetName").toString());
		featuresFile = (File)options.valueOf("featuresFile");
		modelFilePathPrefix = options.valueOf("modelFilePathPrefix").toString();
		outputFilePathPrefix = options.valueOf("outputFilePathPrefix").toString();
		
		return true;
	}
}
