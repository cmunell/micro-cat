package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeatureSet;
import edu.cmu.ml.rtw.generic.data.feature.SerializerDataFeatureMatrixBSONString;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.DocumentSetNLPFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

public class OutputFeatureMatrix {
	private static String experimentName;
	private static int randomSeed;
	private static DocumentSetNLPFactory.SetName initDocumentSetName;
	private static DocumentSetNLPFactory.SetName documentSetName;
	private static AnnotationTypeNLP<String> categoryType;
	
	private static DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context;
	private static TokenSpansDatum.Tools<CategoryList> datumTools;
	private static CatProperties properties;
	private static CatDataTools dataTools;
	private static int datumId;
	private static String outputFilePath;
	
	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = constructDataSet(DocumentSetNLPFactory.getDocumentSet(documentSetName, properties, dataTools));
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> initData = null;
		if (initDocumentSetName == documentSetName)
			initData = data;
		else
			initData = constructDataSet(DocumentSetNLPFactory.getDocumentSet(initDocumentSetName, properties, dataTools));
		
		System.out.println("Initializing features...");
		List<Feature<TokenSpansDatum<CategoryList>, CategoryList>> featureList = context.getFeatures();
		for (Feature<TokenSpansDatum<CategoryList>, CategoryList> feature : featureList) {
			if (!feature.init(initData)) {
				System.out.println("Failed to init feature " + feature.getReferenceName());
			}
		}
		
		FeatureSet<TokenSpansDatum<CategoryList>, CategoryList> features = new FeatureSet<TokenSpansDatum<CategoryList>, CategoryList>(context, featureList);

		DataFeatureMatrix<TokenSpansDatum<CategoryList>, CategoryList> mat = 
				new DataFeatureMatrix<>(context, "", data, features);
		SerializerDataFeatureMatrixBSONString ser = new SerializerDataFeatureMatrixBSONString(dataTools);
	
		FileUtil.writeFile(outputFilePath, ser.serializeToString(mat));
	}
	
	private static DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet(DocumentSet<DocumentNLP, DocumentNLPMutable> documentSet) {
		System.out.println("Constructing data from " + documentSet.getName() + " documents...");
		
		Set<String> documentNames = documentSet.getDocumentNames();
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> dataSet = new DataSet<TokenSpansDatum<CategoryList>, CategoryList>(datumTools, null);
		for (String documentName : documentNames) {
			DocumentNLP document = documentSet.getDocumentByName(documentName);
			List<Pair<TokenSpan, String>> annotations = document.getTokenSpanAnnotations(categoryType);
			Map<TokenSpan, Set<String>> spanCategories = new HashMap<TokenSpan, Set<String>>();
		
			for (Pair<TokenSpan, String> annotation : annotations) {
				if (!spanCategories.containsKey(annotation.getFirst()))
					spanCategories.put(annotation.getFirst(), new HashSet<String>());
				spanCategories.get(annotation.getFirst()).add(annotation.getSecond());
			}
		
			for (Entry<TokenSpan, Set<String>> entry : spanCategories.entrySet()) {
				String[] categories = entry.getValue().toArray(new String[0]);
				if (categories.length == 1 && categories[0].equals(""))
					continue;
				
				dataSet.add(
					new TokenSpansDatum<CategoryList>(datumId, 
							new TokenSpan[] { entry.getKey() }, 
							new CategoryList(categories, 0), 
							false)
				);
				datumId++;
			}
		}
	
		return dataSet;
	}
	
	@SuppressWarnings("unchecked")
	private static boolean parseArgs(String[] args) {
		OptionParser parser = new OptionParser();
		
		parser.accepts("experimentName").withRequiredArg()
			.describedAs("Name of the training experiment")
			.ofType(String.class);
		
		parser.accepts("randomSeed").withRequiredArg()
			.describedAs("Seed for random numbers")
			.ofType(Integer.class)
			.defaultsTo(1);
		
		parser.accepts("documentSetName").withRequiredArg()
			.ofType(String.class)
			.describedAs("Name of the document set");
		
		parser.accepts("initDocumentSetName").withRequiredArg()
		.ofType(String.class)
		.describedAs("Name of the feature initialization document set");
		
		parser.accepts("categoryType").withRequiredArg()
			.ofType(String.class)
			.describedAs("Category annotation type")
			.defaultsTo("freebase-type");
		
		parser.accepts("outputFilePath").withRequiredArg()
		.ofType(String.class)
		.describedAs("Path to output file");
		
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
		

		experimentName = options.valueOf("experimentName").toString();
		randomSeed = (Integer)options.valueOf("randomSeed");
		documentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("documentSetName").toString());
		initDocumentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("initDocumentSetName").toString());
		
		outputFilePath = options.valueOf("outputFilePath").toString();
		
		properties = new CatProperties();
		String experimentInputPath = new File(properties.getContextInputDirPath(), "/GSTBinary/" + experimentName + ".ctx").getAbsolutePath();
		
		dataTools = new CatDataTools(new OutputWriter(), properties);
		
		dataTools.setRandomSeed(randomSeed);
		datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
		
		context = DatumContext.run(datumTools, FileUtil.getFileReader(experimentInputPath));
		if (context == null) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to deserialize context.");
			return false;
		}
	
		datumId = 0;
		
		categoryType = (AnnotationTypeNLP<String>)dataTools.getAnnotationTypeNLP(options.valueOf("categoryType").toString());
		
		return true;
	}
}
