package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.InverseLabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.SupervisedModelCompositeBinary;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;

public class NELLMentionCategorizer implements AnnotatorTokenSpan<String> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.TOKEN,
		AnnotationTypeNLP.SENTENCE,
		AnnotationTypeNLP.POS,
		AnnotationTypeNLP.DEPENDENCY_PARSE,
		AnnotationTypeNLP.NER
	};
	
	public enum LabelType {
		UNWEIGHTED,
		WEIGHTED,
		UNWEIGHTED_CONSTRAINED,
		WEIGHTED_CONSTRAINED
	}
	
	public static final CategoryList.Type DEFAULT_VALID_CATEGORIES = CategoryList.Type.ALL_NELL_CATEGORIES;
	public static final double DEFAULT_MENTION_MODEL_THRESHOLD =  Double.MAX_VALUE;
	public static final LabelType DEFAULT_LABEL_TYPE= LabelType.WEIGHTED_CONSTRAINED;
	public static final File DEFAULT_FEATURES_FILE = new File("models/GSTBinaryNELLNormalized/HazyFacc1_AllNELL_c90_e2000/LRBasel2.model.out");
	public static final String DEFAULT_MODEL_FILE_PATH_PREFIX = "models/GSTBinaryNELLNormalized/HazyFacc1_AllNELL_c90_e2000/LRBasel2.model.out.";
	
	private int datumId;
	private Context<TokenSpansDatum<CategoryList>, CategoryList> context;
	private NELLUtil nell;
	
	private CategoryList validCategories;
	private double mentionModelThreshold;
	private InverseLabelIndicator<CategoryList> inverseLabelIndicator;
	private List<Feature<TokenSpansDatum<CategoryList>, CategoryList>> features;
	private SupervisedModel<TokenSpansDatum<CategoryList>, CategoryList> model;
	private int maxThreads;
	
	public NELLMentionCategorizer() {
		this(TokenSpansDatum.getCategoryListTools(new CatDataTools()),
			 DEFAULT_VALID_CATEGORIES, 
			 DEFAULT_MENTION_MODEL_THRESHOLD, 
			 DEFAULT_LABEL_TYPE, 
			 DEFAULT_FEATURES_FILE, 
			 DEFAULT_MODEL_FILE_PATH_PREFIX,
			 1);
	}
	
	public NELLMentionCategorizer(CategoryList validCategories, double mentionModelThreshold, LabelType labelType, File featuresFile, String modelFilePathPrefix, int maxThreads) {
		this(TokenSpansDatum.getCategoryListTools(new CatDataTools()),
			 validCategories,
			 mentionModelThreshold,
			 labelType,
			 featuresFile,
			 modelFilePathPrefix,
			 1);
	}
	
	public NELLMentionCategorizer(CategoryList validCategories, double mentionModelThreshold, LabelType labelType, int maxThreads) {
		this(TokenSpansDatum.getCategoryListTools(new CatDataTools()),
			 validCategories,
			 mentionModelThreshold,
			 labelType,
			 DEFAULT_FEATURES_FILE,
			 DEFAULT_MODEL_FILE_PATH_PREFIX,
			 maxThreads);
	}

	public NELLMentionCategorizer(Datum.Tools<TokenSpansDatum<CategoryList>, CategoryList> datumTools, CategoryList.Type validCategoriesType, double mentionModelThreshold, LabelType labelType, File featuresFile, String modelFilePathPrefix, int maxThreads) {
		this(datumTools,
			new CategoryList(validCategoriesType, new CatDataTools()),
			mentionModelThreshold,
			labelType,
			featuresFile, 
			modelFilePathPrefix,
			maxThreads);
	}

	
	public NELLMentionCategorizer(Datum.Tools<TokenSpansDatum<CategoryList>, CategoryList> datumTools, CategoryList validCategories, double mentionModelThreshold, LabelType labelType, File featuresFile, String modelFilePathPrefix, int maxThreads) {
		this.datumId = 0;
		this.context = new Context<TokenSpansDatum<CategoryList>, CategoryList>(datumTools);
		
		this.validCategories = validCategories;
		this.mentionModelThreshold = mentionModelThreshold;
		this.maxThreads = maxThreads;
		
		if (labelType == LabelType.UNWEIGHTED) {
			this.inverseLabelIndicator = this.context.getDatumTools().getInverseLabelIndicator("Unweighted");
		} else if (labelType == LabelType.WEIGHTED) {
			this.inverseLabelIndicator = this.context.getDatumTools().getInverseLabelIndicator("Weighted");
		} else if (labelType == LabelType.UNWEIGHTED_CONSTRAINED) {
			this.inverseLabelIndicator = this.context.getDatumTools().getInverseLabelIndicator("UnweightedConstrained");
		} else {
			this.inverseLabelIndicator = this.context.getDatumTools().getInverseLabelIndicator("WeightedConstrained");
		}
		
		this.nell = new NELLUtil((CatDataTools)this.context.getDatumTools().getDataTools());
		
		if (!deserialize(featuresFile, modelFilePathPrefix))
			throw new IllegalArgumentException();
	}
	
	public CategoryList getValidCategories() {
		return this.validCategories;
	}
	
	public boolean deserialize(File featuresFile, String modelFilePathPrefix) {
		CatDataTools dataTools = (CatDataTools)this.context.getDatumTools().getDataTools();
		
		if (this.mentionModelThreshold < 0 || this.validCategories.size() == 0) {
			dataTools.getOutputWriter().debugWriteln("Skipping model and feature deserialization due to negative mention (negative mention model threshold and/or no valid labels).");
			return true;
		}
		
		List<SupervisedModel<TokenSpansDatum<Boolean>, Boolean>> binaryModels = new ArrayList<SupervisedModel<TokenSpansDatum<Boolean>, Boolean>>();
		this.features = new ArrayList<Feature<TokenSpansDatum<CategoryList>, CategoryList>>();
		List<LabelIndicator<CategoryList>> labelIndicators = new ArrayList<LabelIndicator<CategoryList>>();
		
		try {
			dataTools.getOutputWriter().debugWriteln("Deserializing features...");
			
			BufferedReader featureReader = FileUtil.getFileReader(featuresFile.getPath());
			Context<TokenSpansDatum<CategoryList>, CategoryList> featureContext = Context.deserialize(this.context.getDatumTools(), featureReader);
			featureReader.close();
			this.features = featureContext.getFeatures();

			Context<TokenSpansDatum<Boolean>, Boolean> binaryContext = featureContext.makeBinary(TokenSpansDatum.getBooleanTools(this.context.getDatumTools().getDataTools()), null);
			
			dataTools.getOutputWriter().debugWriteln("Finished deserializing " + this.features.size() + " features.");
			
			for (final String category : this.validCategories.getCategories()) {
				File modelFile = new File(modelFilePathPrefix + category);
				if (FileUtil.fileExists(modelFile.getPath())) {
					dataTools.getOutputWriter().debugWriteln("Deserializing " + category + " model at " + modelFile.getPath());
					BufferedReader modelReader = FileUtil.getFileReader(modelFile.getPath());
					
					Context<TokenSpansDatum<Boolean>, Boolean> modelContext = Context.deserialize(binaryContext.getDatumTools(), modelReader);
					modelReader.close();
					
					if (modelContext == null || modelContext.getModels().size() == 0) {
						dataTools.getOutputWriter().debugWriteln("WARNING: Failed to deserialize " + category + " model.  Maybe empty?");	
						continue;
					}
					
					binaryModels.add(modelContext.getModels().get(0));
					
					LabelIndicator<CategoryList> labelIndicator = new LabelIndicator<CategoryList>() {
						@Override
						public String toString() {
							return category;
						}
						
						@Override
						public boolean indicator(CategoryList categories) {
							if (categories == null)
								return true;
							return categories.contains(category);
						}
	
						@Override
						public double weight(CategoryList categories) {
							return categories.getCategoryWeight(category);
						}	
					};
					
					this.context.getDatumTools().addLabelIndicator(labelIndicator);
					labelIndicators.add(labelIndicator);
				}
			
			}
			
			this.model = new SupervisedModelCompositeBinary<TokenSpansDatum<Boolean>, TokenSpansDatum<CategoryList>, CategoryList>(binaryModels, labelIndicators, binaryContext, inverseLabelIndicator);
			dataTools.getOutputWriter().debugWriteln("Finished deserializing models.");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> categorizeNounPhraseMentions(DataSet<TokenSpansDatum<CategoryList>, CategoryList> data) {
		return categorizeNounPhraseMentions(data, false);
	}
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> categorizeNounPhraseMentions(DataSet<TokenSpansDatum<CategoryList>, CategoryList> data, boolean outputUnlabeled) {
		if (this.validCategories.size() == 0 || (this.mentionModelThreshold >= 0
				&& (this.features == null || this.model == null)))
			return null;
		
		FeaturizedDataSet<TokenSpansDatum<CategoryList>, CategoryList> featurizedData = 
			new FeaturizedDataSet<TokenSpansDatum<CategoryList>, CategoryList>("", 
																	this.features, 
																	this.maxThreads, 
																	this.context.getDatumTools(),
																	null);
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> labeledData = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(this.context.getDatumTools(), null);
		
		for (TokenSpansDatum<CategoryList> datum : data) {
			CategoryList categories = datum.getLabel();
			List<String> aboveThreshold = (categories != null) ? categories.getCategoriesAboveWeight(this.mentionModelThreshold) : null;
			if (this.mentionModelThreshold >= 0 && (categories == null || aboveThreshold.size() == 0 || datum.isPolysemous())) {
				featurizedData.add(datum);
			} else {
				CategoryList category = filterToValidCategories(this.inverseLabelIndicator.label(categories.getWeightMap(), aboveThreshold));
				labeledData.add(new TokenSpansDatum<CategoryList>(datum, category, isLabelPolysemous(category)));
			}
		}
		
		if (this.mentionModelThreshold >= 0) {
			if (!featurizedData.precomputeFeatures())
				return null;
			
			Map<TokenSpansDatum<CategoryList>, CategoryList> dataLabels = this.model.classify(featurizedData);
	
			for (Entry<TokenSpansDatum<CategoryList>, CategoryList> entry : dataLabels.entrySet()) {
				CategoryList label = filterToValidCategories(entry.getValue());
				if (!outputUnlabeled && label.size() == 0)
					continue;
				
				labeledData.add(new TokenSpansDatum<CategoryList>(entry.getKey(), label, isLabelPolysemous(label)));
			}
		}
		
		return labeledData;
	}
	
	private boolean isLabelPolysemous(CategoryList categories) {
		List<String> positiveCategories = new ArrayList<String>();
		for (String category : categories.getCategories())
			if (categories.getCategoryWeight(category) >= 0.5)
				positiveCategories.add(category);
		
		return this.nell.areCategoriesMutuallyExclusive(positiveCategories);
	}
	
	private CategoryList filterToValidCategories(CategoryList categories) {
		String[] categoryArray = categories.getCategories();
		List<Pair<String, Double>> filteredLabels = new ArrayList<Pair<String, Double>>(categoryArray.length);
		for (String category : categoryArray)
			if (this.validCategories.contains(category))
				filteredLabels.add(new Pair<String, Double>(category, categories.getCategoryWeight(category)));
		return new CategoryList(filteredLabels);
	}
	
	public DataSet<TokenSpansDatum<CategoryList>, CategoryList> categorizeNounPhraseMentions(DocumentNLP document) {
		if (this.mentionModelThreshold >= 0 && this.validCategories.size() > 0 && (this.features == null || this.model == null))
			return null;
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> documentData = constructDataSet(document, 
																							 this.validCategories.size() > 0 && (this.mentionModelThreshold <= 1.0), 
																							 this.mentionModelThreshold, 
																							 this.context.getDatumTools().getInverseLabelIndicator("WeightedGeneralized"));
		
		if (this.validCategories.size() == 0)
			return documentData;
		
		return categorizeNounPhraseMentions(documentData, false);
	}
	
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet(DocumentNLP document, boolean labeled, double nellConfidenceThreshold, InverseLabelIndicator<CategoryList> inverseLabelIndicator) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(this.context.getDatumTools(), null);

		List<TokenSpan> nps = this.nell.extractNounPhrases(document);
		for (TokenSpan np : nps) {
			TokenSpansDatum<CategoryList> datum = null;
			String npStr = np.toString();
			
			if (labeled) {
				List<Pair<String, Double>> categories = this.nell.getNounPhraseNELLWeightedCategories(npStr, nellConfidenceThreshold);
				Map<String, Double> weights = new TreeMap<String, Double>();
				List<String> indicators = new ArrayList<String>(categories.size());
				for (Pair<String, Double> category : categories) {
					weights.put(category.getFirst(), category.getSecond());
					indicators.add(category.getFirst());
				}
				CategoryList labels = inverseLabelIndicator.label(weights, indicators);
				
				datum = new TokenSpansDatum<CategoryList>(this.datumId, np, labels, this.nell.areCategoriesMutuallyExclusive(Arrays.asList(labels.getCategories())));
			} else {
				datum = new TokenSpansDatum<CategoryList>(this.datumId, np, null, false);
			}
			
			data.add(datum);
			
			synchronized (this) {
				this.datumId++;
			}
		}
		
		return data;
	}

	@Override
	public String getName() {
		return "micro-cat";
	}

	@Override
	public AnnotationType<String> produces() {
		return AnnotationTypeNLPCat.NELL_CATEGORY;
	}

	@Override
	public AnnotationType<?>[] requires() {
		return REQUIRED_ANNOTATIONS;
	}

	@Override
	public boolean measuresConfidence() {
		return true;
	}

	@Override
	public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> categorizedData =  categorizeNounPhraseMentions(document);
		List<Triple<TokenSpan, String, Double>> categorized = new ArrayList<Triple<TokenSpan, String, Double>>();
		
		for (TokenSpansDatum<CategoryList> datum : categorizedData) {
			TokenSpan span = datum.getTokenSpans()[0];
			Map<String, Double> categories = datum.getLabel().getWeightMap();
			
			for (Entry<String, Double> entry : categories.entrySet()) {
				categorized.add(new Triple<TokenSpan, String, Double>(span, entry.getKey(), entry.getValue()));	
			}
		}
		
		System.out.println("Categorized " + categorized.size());
		
		return categorized;
	}
}
