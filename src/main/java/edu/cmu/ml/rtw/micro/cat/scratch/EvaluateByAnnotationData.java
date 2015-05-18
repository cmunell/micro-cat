package edu.cmu.ml.rtw.micro.cat.scratch;
/*
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLDataSetFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;*/

public class EvaluateByAnnotationData {
/*	public static void main(String[] args) {
		double nellConfidenceThreshold = Double.valueOf(args[0]);
		int maxThreads = Integer.valueOf(args[1]);
		File featuresFile = new File(args[2]);
		String modelFilePathPrefix = args[3];
		File inputFileDir= new File(args[4]);
		
		final CatProperties properties = new CatProperties();
		OutputWriter output = new OutputWriter();
		
		CatDataTools dataTools = new CatDataTools(output, properties);
		
		TokenSpansDatum.Tools<CategoryList> datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
		TokenSpansDatum.Tools<Boolean> binaryTools = TokenSpansDatum.getBooleanTools(dataTools);
		NELLDataSetFactory dataFactory = new NELLDataSetFactory(dataTools, properties.getHazyFacc1DataDirPath(), 1000000);

		NELLMentionCategorizer categorizer = new NELLMentionCategorizer(datumTools, "ALL_NELL_CATEGORIES", Double.MAX_VALUE, NELLMentionCategorizer.LabelType.WEIGHTED_CONSTRAINED, featuresFile, modelFilePathPrefix, dataFactory);
		
		File[] inputFiles = inputFileDir.listFiles();
		Map<String, Map<String, Pair<Evaluation, Evaluation>>> categoryToNameToPerformance = new TreeMap<String, Map<String, Pair<Evaluation, Evaluation>>>();
		Set<String> names = new TreeSet<String>();
		for (File inputFile : inputFiles) {
			Pair<String, DataSet<TokenSpansDatum<Boolean>, Boolean>> annotatedData = loadAnnotatedData(inputFile, binaryTools, dataTools.getDocumentCache());		
			System.out.println("Evaluating " + annotatedData.getFirst() + "...");
			String[] nameAndCategory = annotatedData.getFirst().split("\\.");
			String name = nameAndCategory[0];
			String category = nameAndCategory[1];
			
			Pair<Evaluation, Evaluation> evaluation = evaluateByAnnotatedData(maxThreads, categorizer, nellConfidenceThreshold, annotatedData.getSecond(), category);
			
			if (!categoryToNameToPerformance.containsKey(category))
				categoryToNameToPerformance.put(category, new HashMap<String, Pair<Evaluation, Evaluation>>());
			categoryToNameToPerformance.get(category).put(name, evaluation);
			names.add(name);
		}
		
		names.add("nb/lc");
		names.add("~nonpoly");
		
		for (Entry<String, Map<String, Pair<Evaluation, Evaluation>>> entry : categoryToNameToPerformance.entrySet()) {
			Pair<Evaluation, Evaluation> evaluationNb = entry.getValue().get("nb");
			Pair<Evaluation, Evaluation> evaluationLc = entry.getValue().get("lc");
			Pair<Evaluation, Evaluation> evaluationPoly = entry.getValue().get("hc_poly");
		
			Evaluation nbLc1 = evaluationNb.getFirst().plus(evaluationLc.getFirst());
			Evaluation nbLc2 = evaluationNb.getSecond().plus(evaluationLc.getSecond());
			Evaluation notNonpoly1 = nbLc1.plus(evaluationPoly.getFirst());
			Evaluation notNonpoly2 = nbLc2.plus(evaluationPoly.getSecond());
			
			entry.getValue().put("nb/lc", new Pair<Evaluation, Evaluation>(nbLc1, nbLc2));
			entry.getValue().put("~nonpoly", new Pair<Evaluation, Evaluation>(notNonpoly1, notNonpoly2));
		}
		
		outputEvaluations(names, categoryToNameToPerformance, Evaluation.Measure.Accuracy);
		outputEvaluations(names, categoryToNameToPerformance, Evaluation.Measure.F1);
		outputEvaluations(names, categoryToNameToPerformance, Evaluation.Measure.Precision);
		outputEvaluations(names, categoryToNameToPerformance, Evaluation.Measure.Recall);
	}
	
	private static Pair<String, DataSet<TokenSpansDatum<Boolean>, Boolean>> loadAnnotatedData(File file, TokenSpansDatum.Tools<Boolean> tools, DocumentCache documents) {
		DataSet<TokenSpansDatum<Boolean>, Boolean> data = new DataSet<TokenSpansDatum<Boolean>, Boolean>(tools, null);
		String fileName = file.getName();
		String nameAndCategory = fileName.substring(fileName.lastIndexOf('.', fileName.lastIndexOf('.') - 1) + 1);
		
		BufferedReader r = FileUtil.getFileReader(file.getAbsolutePath());
		try {
			String line = null;
			int id = 0;
			while ((line = r.readLine()) != null) {
				boolean label = line.substring(0, line.indexOf('\t')).trim().equals("1");
				TokenSpan tokenSpan = TokenSpan.fromJSON(new JSONObject(line.substring(line.lastIndexOf('\t'))), documents);
				TokenSpansDatum<Boolean> datum = new TokenSpansDatum<Boolean>(id, tokenSpan, label, false);
				data.add(datum);
				id++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new Pair<String, DataSet<TokenSpansDatum<Boolean>, Boolean>>(nameAndCategory, data);
	}
	
	private static Pair<Evaluation, Evaluation> evaluateByAnnotatedData(int maxThreads, NELLMentionCategorizer categorizer, double nellConfidenceThreshold, DataSet<TokenSpansDatum<Boolean>, Boolean> data, String label) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> unlabeledData = makeUnlabeledData(data);
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> mentionLabeledData = categorizer.categorizeNounPhraseMentions(unlabeledData, maxThreads, true);
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> nellLabeledData = nellLabelData(unlabeledData, maxThreads, nellConfidenceThreshold);
		
		Evaluation mentionEvaluation = new Evaluation();
		Evaluation baselineEvaluation = new Evaluation();
		for (TokenSpansDatum<Boolean> datum : data) {
			boolean mentionLabel = mentionLabeledData.getDatumById(datum.getId()).getLabel().contains(label);
			boolean baselineLabel = nellLabeledData.getDatumById(datum.getId()).getLabel().contains(label);
			
			if (mentionLabel == baselineLabel) {
				System.out.println("ERROR: Equal labels on " + label + 
						" " + datum.getId() + 
						" " + datum.getTokenSpans()[0].toString() + 
						" " + datum.getTokenSpans()[0].toJSON(true) + 
						" " + mentionLabeledData.getDatumById(datum.getId()).getLabel());
			}
				
			if (datum.getLabel()) {
				if (mentionLabel)
					mentionEvaluation.incrementTP();
				else
					mentionEvaluation.incrementFN();
				
				if (baselineLabel)
					baselineEvaluation.incrementTP();
				else
					baselineEvaluation.incrementFN();
			} else {
				if (!mentionLabel)
					mentionEvaluation.incrementTN();
				else
					mentionEvaluation.incrementFP();
				
				if (!baselineLabel)
					baselineEvaluation.incrementTN();
				else
					baselineEvaluation.incrementFP();
			}
		}
		
		return new Pair<Evaluation, Evaluation>(mentionEvaluation, baselineEvaluation);
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
	
	private static DataSet<TokenSpansDatum<CategoryList>, CategoryList> makeUnlabeledData(DataSet<TokenSpansDatum<Boolean>, Boolean> binaryData) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(binaryData.getDatumTools().getDataTools()), null);
		for (TokenSpansDatum<Boolean> datum : binaryData)
			data.add(new TokenSpansDatum<CategoryList>(datum.getId(), datum.getTokenSpans()[0], null, false));
		return data;
	}
	
	private static void outputEvaluations(Set<String> dataSetNames, Map<String, Map<String, Pair<Evaluation, Evaluation>>> evaluations, Evaluation.Measure measure) {
		StringBuilder outputStr = new StringBuilder();
		Map<String, Pair<Evaluation, Evaluation>> aggregateEvaluation = new HashMap<String, Pair<Evaluation, Evaluation>>();
		
		outputStr.append(measure + "\t");
		for (String name : dataSetNames) {
			outputStr.append(name).append("\t");
			outputStr.append(name).append("-(base)\t");
			aggregateEvaluation.put(name, new Pair<Evaluation, Evaluation>(new Evaluation(), new Evaluation()));
		}
		outputStr.append("\n");
		
		for (Entry<String, Map<String, Pair<Evaluation, Evaluation>>> categoryEntry : evaluations.entrySet()) {
			String category = categoryEntry.getKey();
			outputStr.append(category).append("\t");
			
			for (String name : dataSetNames) {
				Pair<Evaluation, Evaluation> evaluation = categoryEntry.getValue().get(name);
				outputStr.append(evaluation.getFirst().compute(measure)).append("\t");
				outputStr.append(evaluation.getSecond().compute(measure)).append("\t");
			
				aggregateEvaluation.get(name).setFirst(aggregateEvaluation.get(name).getFirst().plus(evaluation.getFirst()));
				aggregateEvaluation.get(name).setSecond(aggregateEvaluation.get(name).getSecond().plus(evaluation.getSecond()));
			}
		
			outputStr.append("\n");
		}
		
		outputStr.append("Micro-average\t");
		for (String name : dataSetNames) {
			outputStr.append(aggregateEvaluation.get(name).getFirst().compute(measure)).append("\t");
			outputStr.append(aggregateEvaluation.get(name).getSecond().compute(measure)).append("\t");
		}
		outputStr.append("\n\n");
		
		System.out.println(outputStr.toString());
	}
	
	private static class Evaluation {
		public enum Measure {
			Accuracy,
			F1,
			Precision,
			Recall
		}
		
		private double tp;
		private double tn;
		private double fp;
		private double fn;
		
		public Evaluation(double tp, double tn, double fp, double fn) {
			this.tp = tp;
			this.tn = tn;
			this.fp = fp;
			this.fn = fn;
		}
		
		public Evaluation() {
			this(0.0, 0.0, 0.0, 0.0);
		}
		
		public double incrementTP() {
			this.tp++;
			return this.tp;
		}

		public double incrementTN() {
			this.tn++;
			return this.tn;
		}
		
		public double incrementFP() {
			this.fp++;
			return this.fp;
		}
		
		public double incrementFN() {
			this.fn++;
			return this.fn;
		}
		
		public Evaluation plus(Evaluation evaluation) {
			return new Evaluation(evaluation.tp + this.tp, evaluation.tn + this.tn, evaluation.fp + this.fp, evaluation.fn + this.fn);
		}
		
		public double compute(Measure measure) {
			if (measure == Measure.Accuracy)
				return computeAccuracy();
			else if (measure == Measure.F1)
				return computeF1();
			else if (measure == Measure.Precision)
				return computePrecision();
			else if (measure == Measure.Recall)
				return computeRecall();
			else
				return 0.0;
		}
		
		public double computeAccuracy() {
			return (this.tp + this.tn)/(this.tp + this.tn + this.fn + this.fp);
		}
		
		public double computeF1() {
			return 2.0*this.tp/(2.0*this.tp + this.fn + this.fp);
		}
		
		public double computePrecision() {
			if (this.tp == 0 && this.fp == 0)
				return 1.0;
			
			return this.tp/(this.tp + this.fp);
		}
		
		public double computeRecall() {
			if (this.tp == 0 && this.fn == 0)
				return 1.0;
			
			return this.tp/(this.tp + this.fn);
		}
	}*/
}
