package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;

public class NELLDataSetFactory {
	public static class MentionDataSetCollection {
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> lowConfidenceTestData;
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> noBeliefTestData; 
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> polysemousTestData;
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> nonPolysemousTrainData;
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> nonPolysemousTestData;
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> devData;
		private DataSet<TokenSpansDatum<CategoryList>, CategoryList> testData;
		private TokenSpansDatum.Tools<CategoryList> datumTools;
		private CategoryList categories;
		
		public MentionDataSetCollection(
				TokenSpansDatum.Tools<CategoryList> datumTools,
				CategoryList categories,
				DocumentSetNLPFactory.SetName documentSetName, 
				double nellConfidenceThreshold,
				int lowConfidenceTestExamples,
				int noBeliefTestExamples,
				int polysemousTestExamples,
				String nonPolysemousDataSetName,
				int nonPolysemousExamplesPerLabel) {	
			this.datumTools = datumTools;
			this.categories = categories;
		
			initialize(documentSetName, 
					   nellConfidenceThreshold,
					   lowConfidenceTestExamples,
					   noBeliefTestExamples,
					   polysemousTestExamples,
					   nonPolysemousDataSetName,
					   nonPolysemousExamplesPerLabel);
		}
		
		
		public MentionDataSetCollection(int randomSeed,
				String categoriesStr,
				DocumentSetNLPFactory.SetName documentSetName, 
				double nellConfidenceThreshold,
				int lowConfidenceTestExamples,
				int noBeliefTestExamples,
				int polysemousTestExamples,
				String nonPolysemousDataSetName,
				int nonPolysemousExamplesPerLabel) {
			
			CatProperties properties = new CatProperties();
			OutputWriter output = new OutputWriter();
			CatDataTools dataTools = new CatDataTools(output, properties);
			dataTools.setRandomSeed(randomSeed);
			
			this.categories = CategoryList.fromString(categoriesStr, dataTools);
			this.datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
			
			initialize(documentSetName, 
					   nellConfidenceThreshold,
					   lowConfidenceTestExamples,
					   noBeliefTestExamples,
					   polysemousTestExamples,
					   nonPolysemousDataSetName,
					   nonPolysemousExamplesPerLabel);
		}
		
		public MentionDataSetCollection(int randomSeed,
										CategoryList categories,
										DocumentSetNLPFactory.SetName documentSetName, 
										double nellConfidenceThreshold,
										int lowConfidenceTestExamples,
										int noBeliefTestExamples,
										int polysemousTestExamples,
										String nonPolysemousDataSetName,
										int nonPolysemousExamplesPerLabel) {	
			CatProperties properties = new CatProperties();
			OutputWriter output = new OutputWriter();
			CatDataTools dataTools = new CatDataTools(output, properties);
			dataTools.setRandomSeed(randomSeed);
			
			this.categories = categories;
			this.datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
			
			initialize(documentSetName, 
					   nellConfidenceThreshold,
					   lowConfidenceTestExamples,
					   noBeliefTestExamples,
					   polysemousTestExamples,
					   nonPolysemousDataSetName,
					   nonPolysemousExamplesPerLabel);
		}
		
		private void initialize(DocumentSetNLPFactory.SetName documentSetName, 
								double nellConfidenceThreshold,
								int lowConfidenceTestExamples,
								int noBeliefTestExamples,
								int polysemousTestExamples,
								String nonPolysemousDataSetName,
								int nonPolysemousExamplesPerLabel) {
			CatDataTools dataTools = (CatDataTools)this.datumTools.getDataTools();
			
			NELLDataSetFactory dataFactory = new NELLDataSetFactory(dataTools, 
					DocumentSetNLPFactory.getDocumentSet(documentSetName, 
														dataTools.getProperties(), 
														dataTools));
			
			Datum.Tools.Clusterer<TokenSpansDatum<CategoryList>, CategoryList, String> documentClusterer = 
					new Datum.Tools.Clusterer<TokenSpansDatum<CategoryList>, CategoryList, String>() {
						public String getCluster(TokenSpansDatum<CategoryList> datum) {
							return datum.getTokenSpans()[0].getDocument().getName();
						}
					};
			
			// Random fraction of data for dev and test
			// Dev-test documents are collected and ignored in loading training data (below)
			// The dev and test data are loaded as a random fraction so that the label frequencies in the sample match the frequencies in the population
			// (but this is not true of the training data)
			DataSet<TokenSpansDatum<CategoryList>, CategoryList> devTestData = dataFactory.loadSupervisedDataSet(dataTools.getProperties().getNELLDataFileDirPath(), .01, nellConfidenceThreshold, NELLDataSetFactory.PolysemyMode.NON_POLYSEMOUS, datumTools.getInverseLabelIndicator("UnweightedGeneralized"));
			Set<String> devTestDocuments = new HashSet<String>();
			for (TokenSpansDatum<CategoryList> datum : devTestData)
				devTestDocuments.add(datum.getTokenSpans()[0].getDocument().getName());
			List<DataSet<TokenSpansDatum<CategoryList>, CategoryList>> devTestDataParts = devTestData.makePartition(new double[] { .5,  .5 }, documentClusterer, dataTools.getGlobalRandom());
			this.devData = devTestDataParts.get(0);
			this.testData = devTestDataParts.get(1);
			this.lowConfidenceTestData = dataFactory.loadLowConfidenceDataSet(dataTools.getProperties().getNELLDataFileDirPath(), lowConfidenceTestExamples, nellConfidenceThreshold);
			this.noBeliefTestData = dataFactory.loadNoBeliefDataSet(dataTools.getProperties().getNELLDataFileDirPath(), noBeliefTestExamples, nellConfidenceThreshold);
			this.polysemousTestData = dataFactory.loadPolysemousDataSet(dataTools.getProperties().getNELLDataFileDirPath(), polysemousTestExamples, nellConfidenceThreshold,  datumTools.getInverseLabelIndicator("Unweighted"));
			
			DataSet<TokenSpansDatum<CategoryList>, CategoryList> nonPolysemousData = dataFactory.loadSupervisedDataSet(dataTools.getProperties().getNELLDataFileDirPath(), nonPolysemousDataSetName, this.categories, nonPolysemousExamplesPerLabel, nellConfidenceThreshold,  datumTools.getInverseLabelIndicator("UnweightedGeneralized"), devTestDocuments);
			List<DataSet<TokenSpansDatum<CategoryList>, CategoryList>> nonPolysemousDataParts = nonPolysemousData.makePartition(new double[] { .9,  .1 }, documentClusterer, dataTools.getGlobalRandom());
			this.nonPolysemousTrainData = nonPolysemousDataParts.get(0);
			this.nonPolysemousTestData = nonPolysemousDataParts.get(1);
			
			for (final String category : categories.getCategories()) {
				LabelIndicator<CategoryList> labelIndicator = new LabelIndicator<CategoryList>() {
					public String toString() {
						return category;
					}
					
					@Override
					public boolean indicator(CategoryList labelList) {
						return labelList.contains(category);
					}
					
					@Override
					public double weight(CategoryList labelList) {
						return labelList.getCategoryWeight(category);
					}
				};
				
				this.lowConfidenceTestData.getDatumTools().addLabelIndicator(labelIndicator);
				this.noBeliefTestData.getDatumTools().addLabelIndicator(labelIndicator);
				this.polysemousTestData.getDatumTools().addLabelIndicator(labelIndicator);
				this.nonPolysemousTrainData.getDatumTools().addLabelIndicator(labelIndicator);
				this.nonPolysemousTestData.getDatumTools().addLabelIndicator(labelIndicator);
				this.datumTools.addLabelIndicator(labelIndicator);
			}
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getLowConfidenceTestData() {
			return this.lowConfidenceTestData;
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getNoBeliefTestData() {
			return this.noBeliefTestData;
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getPolysemousTestData() {
			return this.polysemousTestData;
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getNonPolysemousTrainData() {
			return this.nonPolysemousTrainData;
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getNonPolysemousTestData() {
			return this.nonPolysemousTestData;
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getDevData() {
			return this.devData;
		}
		
		public DataSet<TokenSpansDatum<CategoryList>, CategoryList> getTestData() {
			return this.testData;
		}
		
		public TokenSpansDatum.Tools<CategoryList> getDatumTools() {
			return this.datumTools;
		}
		
		public CategoryList getCategories() {
			return this.categories;
		}
	}
	
	public enum PolysemyMode {
		NON_POLYSEMOUS,
		UNLABELED_POLYSEMOUS,
		LABELED_POLYSEMOUS
	}
	
	private interface DataSetConstructor {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet();
	}
	
	private CatDataTools dataTools;
	private NELLUtil nell;
	private DocumentSet<DocumentNLP> documentSet;
	private int id;
	
	public NELLDataSetFactory(CatDataTools dataTools) {
		this(dataTools, null);
	}
	
	public NELLDataSetFactory(CatDataTools dataTools, DocumentSet<DocumentNLP> documentSet) {
		this.dataTools = dataTools;
		this.nell = new NELLUtil(this.dataTools);
		this.documentSet = documentSet;
		
		this.dataTools.setDocumentSet(this.documentSet);
		this.id = 0;
	}
	
	/**
	 * 
	 * @param dataFileDirPath
	 * @param nellConfidenceThreshold
	 * @param dataFraction
	 * @param polysemyMode
	 * @param inverseLabelIndicator
	 * @return a labeled data set with labels determined by NELL with confidence greater than nellConfidenceThreshold.
	 * Note that both "loadSupervisedDataSet" and "loadUnsupervisedDataSet" both return labeled data, but "loadUnsupervisedDataSet"
	 * always returns all labels suggested by NELL, without any threshold, for use in the unsupervised setting.
	 */
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadSupervisedDataSet(String dataFileDirPath,
																	double dataFraction, 
																	double nellConfidenceThreshold, 
																	PolysemyMode polysemyMode,
																	Datum.Tools.InverseLabelIndicator<CategoryList> inverseLabelIndicator) {
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = loadDataSet(dataFileDirPath, dataFraction);
		if (data == null)
			return null;
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> retData = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
		for (TokenSpansDatum<CategoryList> datum : data) {
			CategoryList fullLabel = datum.getLabel();
			Map<String, Double> weights = new HashMap<String, Double>();
			List<String> positiveIndicators = new ArrayList<String>();
			
			for (String label : fullLabel.getCategories()) {
				weights.put(label, fullLabel.getCategoryWeight(label));
				
				double weight = fullLabel.getCategoryWeight(label);
				if (weight >= nellConfidenceThreshold) {
					positiveIndicators.add(label);
				}
			}
			
			CategoryList filteredLabel = inverseLabelIndicator.label(weights, positiveIndicators);
			positiveIndicators = new ArrayList<String>();
			for (String label : filteredLabel.getCategories()) {
				if (filteredLabel.getCategoryWeight(label) >= nellConfidenceThreshold)
					positiveIndicators.add(label);
			}
			
			boolean polysemous = this.nell.areCategoriesMutuallyExclusive(positiveIndicators);
			
			if (!polysemous || polysemyMode == PolysemyMode.LABELED_POLYSEMOUS) {
				retData.add(new TokenSpansDatum<CategoryList>(datum.getId(), datum.getTokenSpans()[0], filteredLabel, polysemous));
			} else if (polysemyMode == PolysemyMode.UNLABELED_POLYSEMOUS) {
				retData.add(new TokenSpansDatum<CategoryList>(datum.getId(), datum.getTokenSpans()[0], null, polysemous));
			}
		}
		
		return retData;
	}
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadUnsupervisedDataSet(String dataFileDirPath,
			double dataFraction, 
			boolean includeLabels,
			boolean includeLabelWeights) {

		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = loadDataSet(dataFileDirPath, dataFraction);
		if (includeLabelWeights && includeLabels)
			return data;
		if (data == null)
			return null;
	
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> retData = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
		for (TokenSpansDatum<CategoryList> datum : data) {
			if (!includeLabels) {
				datum.setLabel(null);
			} else if (!includeLabelWeights) {
				CategoryList weightedLabel = datum.getLabel();
				double[] ones = new double[weightedLabel.getCategories().length];
				Arrays.fill(ones, 1.0);
				CategoryList unweightedLabel = new CategoryList(weightedLabel.getCategories(), ones, 0);
				datum.setLabel(unweightedLabel);
			}
			retData.add(datum);
		}

		return retData;
	}	
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadSupervisedDataSet(String dataFileDirPath, final String name, final CategoryList labels, final int minExamplesPerType, final double nellConfidenceThreshold, final Datum.Tools.InverseLabelIndicator<CategoryList> inverseLabelIndicator, final Set<String> ignoreDocuments) {
		File file = new File(dataFileDirPath, "NELLData_" + this.documentSet.getName() + "_" + name);
		
		final NELLDataSetFactory that = this;
		return loadDataSet(file, new DataSetConstructor() { 
			public DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet() { 
				return that.constructSupervisedDataSet(name, labels, minExamplesPerType, nellConfidenceThreshold, inverseLabelIndicator, ignoreDocuments);	
			}
		});
	}
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadLowConfidenceDataSet(String dataFileDirPath, final int exampleCount, final double nellConfidenceThreshold) {
		File file = new File(dataFileDirPath, "NELLData_LowConfidence_" + this.documentSet.getName() + "_e" + exampleCount + "_c" + (int)(nellConfidenceThreshold * 100));
		
		final NELLDataSetFactory that = this;
		return loadDataSet(file, new DataSetConstructor() { 
			public DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet() { 
				return that.constructLowConfidenceDataSet(exampleCount, nellConfidenceThreshold);	
			}
		});
	}
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadNoBeliefDataSet(String dataFileDirPath, final int exampleCount, final double nellConfidenceThreshold) {
		File file = new File(dataFileDirPath, "NELLData_NoBelief_" + this.documentSet.getName() + "_e" + exampleCount + "_c" + (int)(nellConfidenceThreshold * 100));
		
		final NELLDataSetFactory that = this;
		return loadDataSet(file, new DataSetConstructor() { 
			public DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet() { 
				return that.constructNoBeliefDataSet(exampleCount, nellConfidenceThreshold);	
			}
		});
	}
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadPolysemousDataSet(String dataFileDirPath, final int exampleCount, final double nellConfidenceThreshold, final Datum.Tools.InverseLabelIndicator<CategoryList> inverseLabelIndicator) {
		File file = new File(dataFileDirPath, "NELLData_Polysemous_" + this.documentSet.getName() + "_e" + exampleCount + "_c" + (int)(nellConfidenceThreshold * 100));
		
		final NELLDataSetFactory that = this;
		return loadDataSet(file, new DataSetConstructor() { 
			public DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet() { 
				return that.constructPolysemousDataSet(exampleCount, nellConfidenceThreshold, inverseLabelIndicator);	
			}
		});
	}
	
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadDataSet(String dataFileDirPath, final double dataFraction) {
		File file = new File(dataFileDirPath, "NELLData_" + this.documentSet.getName() + "_f" + (int)(dataFraction * 100));
		
		final NELLDataSetFactory that = this;
		return loadDataSet(file, new DataSetConstructor() { 
			public DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet() { 
				return that.constructDataSet(dataFraction);	
			}
		});
	}
	
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> loadDataSet(File file, DataSetConstructor constructor) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = null;
		OutputWriter output = this.dataTools.getOutputWriter();
		if (file.exists()) {
			output.debugWriteln("Loading data set...");
			data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
			try {
				if (!data.deserialize(FileUtil.getFileReader(file.getAbsolutePath()), this.id))
					return null;
				this.id += data.size();
			} catch (Exception e) {
				return null;
			}
			output.debugWriteln("Finished loading data set.");
		} else {
			output.debugWriteln("Constructing data set...");
			data = constructor.constructDataSet(); 
			try {
				if (!data.serialize(new FileWriter(file)))
					return null;
			} catch (IOException e) {
				return null;
			}
			output.debugWriteln("Finished constructing data set.");
		}
		
		return data;
	}
	
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet(double dataFraction) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
		
		Random r = this.dataTools.getGlobalRandom();
		for (String documentName : this.documentSet.getDocumentNames()) {
			DocumentNLP document = this.documentSet.getDocumentByName(documentName);
			if (r.nextDouble() >= dataFraction)
				continue;
			
			List<TokenSpan> nps = this.nell.extractNounPhrases(document);
			for (TokenSpan np : nps) {
				String npStr = np.toString();
				List<Pair<String, Double>> categories = this.nell.getNounPhraseNELLWeightedCategories(npStr, 0.0);
				CategoryList labels = new CategoryList(categories);
				TokenSpansDatum<CategoryList> datum = new TokenSpansDatum<CategoryList>(this.id, np, labels, this.nell.areCategoriesMutuallyExclusive(Arrays.asList(labels.getCategories())));
				data.add(datum);
				this.id++;
			}
		}
		
		return data;
	}
	
	// Constructs a data set with minExamplesPerType positive examples per label
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructSupervisedDataSet(String name, CategoryList labels, int minExamplesPerType, double nellConfidenceThreshold, Datum.Tools.InverseLabelIndicator<CategoryList> inverseLabelIndicator, Set<String> ignoreDocuments) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
		Map<String, Integer> labelCounts = new HashMap<String, Integer>();

		Set<String> unsaturatedLabels = new HashSet<String>();
		for (String label : labels.getCategories()) {
			labelCounts.put(label, 0);
			unsaturatedLabels.add(label);
		}
		
		for (String documentName : this.documentSet.getDocumentNames()) {
			DocumentNLP document = this.documentSet.getDocumentByName(documentName);
			if (ignoreDocuments.contains(document.getName()))
				continue;
			
			List<TokenSpan> nps = this.nell.extractNounPhrases(document);
			for (TokenSpan np : nps) {
				String npStr = np.toString();
				
				CategoryList npLabels = getLabelForNounPhrase(npStr, nellConfidenceThreshold, inverseLabelIndicator);
				if (npLabels == null)
					continue;
				
				if (this.nell.areCategoriesMutuallyExclusive(Arrays.asList(npLabels.getCategories())))
					continue;
				
				boolean hasUnsaturatedLabel = false;
				for (String label : npLabels.getCategories()) {
					if (unsaturatedLabels.contains(label)) {
						hasUnsaturatedLabel = true;
						break;
					}
				}
				
				if (!hasUnsaturatedLabel)
					continue;
				
				for (String label : npLabels.getCategories()) {
					if (labelCounts.containsKey(label)) {
						labelCounts.put(label, labelCounts.get(label) + 1);
						if (labelCounts.get(label) >= minExamplesPerType) {
							unsaturatedLabels.remove(label);
						}
					}
				}
				
				TokenSpansDatum<CategoryList> datum = new TokenSpansDatum<CategoryList>(this.id, np, npLabels, false);
				data.add(datum);
				this.id++;
			}
			
			if (unsaturatedLabels.isEmpty()) {
				break;
			}
		}
		
		return data;
	}
	
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructNoBeliefDataSet(int exampleCount, double nellConfidenceThreshold) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);

		for (String documentName : this.documentSet.getDocumentNames()) {
			DocumentNLP document = this.documentSet.getDocumentByName(documentName);
			List<TokenSpan> nps = this.nell.extractNounPhrases(document);
			for (TokenSpan np : nps) {
				String npStr = np.toString();
				List<Pair<String, Double>> categories = this.nell.getNounPhraseNELLWeightedCategories(npStr, 0.0);

				if (categories.size() != 0)
					continue;
				
				TokenSpansDatum<CategoryList> datum = new TokenSpansDatum<CategoryList>(this.id, np, null, false);
				data.add(datum);
				this.id++;
			
				if (data.size() >= exampleCount)
					break;
			}
			
			if (data.size() >= exampleCount)
				break;
		}
		
		return data;
	}
	
	// minExamplesPerType low confidence examples (no label above confidence)
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructLowConfidenceDataSet(int exampleCount, double nellConfidenceThreshold) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
		for (String documentName : this.documentSet.getDocumentNames()) {
			DocumentNLP document = this.documentSet.getDocumentByName(documentName);
			List<TokenSpan> nps = this.nell.extractNounPhrases(document);
			for (TokenSpan np : nps) {
				String npStr = np.toString();
				List<Pair<String, Double>> categories = this.nell.getNounPhraseNELLWeightedCategories(npStr, 0.0);

				if (categories.size() == 0)
					continue;
				
				boolean lowConfidence = true;
				for (Pair<String, Double> category : categories) {
					if (category.getSecond() >= nellConfidenceThreshold) {
						lowConfidence = false;
						break;
					}
				}
				if (!lowConfidence)
					continue;
				
				TokenSpansDatum<CategoryList> datum = new TokenSpansDatum<CategoryList>(this.id, np, null, false);
				data.add(datum);
				this.id++;
			
				if (data.size() >= exampleCount)
					break;
			}
			
			if (data.size() >= exampleCount)
				break;
		}
		
		return data;
	}
	
	// minExamplesPerType polysemous examples
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructPolysemousDataSet(int exampleCount, double nellConfidenceThreshold, Datum.Tools.InverseLabelIndicator<CategoryList> inverseLabelIndicator) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(TokenSpansDatum.getCategoryListTools(this.dataTools), null);
		
		for (String documentName : this.documentSet.getDocumentNames()) {
			DocumentNLP document = this.documentSet.getDocumentByName(documentName);
			List<TokenSpan> nps = this.nell.extractNounPhrases(document);
			for (TokenSpan np : nps) {
				String npStr = np.toString();
				
				CategoryList labels = getLabelForNounPhrase(npStr, nellConfidenceThreshold, inverseLabelIndicator);
				if (labels == null)
					continue;
				
				if (!this.nell.areCategoriesMutuallyExclusive(Arrays.asList(labels.getCategories())))
					continue;
				
				TokenSpansDatum<CategoryList> datum = new TokenSpansDatum<CategoryList>(this.id, np, labels, true);
				data.add(datum);
				this.id++;
				
				if (data.size() >= exampleCount)
					break;
			}
			
			if (data.size() >= exampleCount)
				break;
		}
			
		return data;
	}
	
	private CategoryList getLabelForNounPhrase(String np, double nellConfidenceThreshold, Datum.Tools.InverseLabelIndicator<CategoryList> inverseLabelIndicator) {
		List<Pair<String, Double>> categories = this.nell.getNounPhraseNELLWeightedCategories(np, nellConfidenceThreshold);
		if (categories.size() == 0)
			return null;
		
		Map<String, Double> categoryWeights = new HashMap<String, Double>();
		List<String> positiveIndicators = new ArrayList<String>();
		for (Pair<String, Double> category : categories) {
			positiveIndicators.add(category.getFirst());
			categoryWeights.put(category.getFirst(), category.getSecond());
		}

		return inverseLabelIndicator.label(categoryWeights, positiveIndicators);
	}
}
