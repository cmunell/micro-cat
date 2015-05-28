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
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentSetNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.evaluation.ValidationGSTBinary;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.DocumentSetNLPFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

public class TrainGSTBinary {
	private static String experimentName;
	private static int randomSeed;
	private static DocumentSetNLPFactory.SetName trainDocumentSetName;
	private static DocumentSetNLPFactory.SetName devDocumentSetName;
	private static DocumentSetNLPFactory.SetName testDocumentSetName;
	private static AnnotationTypeNLP<String> categoryType;
	
	private static Context<TokenSpansDatum<CategoryList>, CategoryList> context;
	private static TokenSpansDatum.Tools<CategoryList> datumTools;
	private static CatProperties properties;
	private static CatDataTools dataTools;
	private static int datumId;
	
	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> trainData = constructDataSet(DocumentSetNLPFactory.getDocumentSet(trainDocumentSetName, properties, dataTools));
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> devData = constructDataSet(DocumentSetNLPFactory.getDocumentSet(devDocumentSetName, properties, dataTools));
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> testData = constructDataSet(DocumentSetNLPFactory.getDocumentSet(testDocumentSetName, properties, dataTools));
		
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Setting up binary GST validation...");
		
		CategoryList categories = new CategoryList(context.getStringArray("validLabels").toArray(new String[0]), null, 0);
		for (String category : categories.getCategories()) {
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
			
			datumTools.addLabelIndicator(labelIndicator);
		}
		
		ValidationGSTBinary<TokenSpansDatum<Boolean>,TokenSpansDatum<CategoryList>,CategoryList> validation = 
				new ValidationGSTBinary<TokenSpansDatum<Boolean>, TokenSpansDatum<CategoryList>, CategoryList>(
						experimentName, 
						context,
						trainData,
						devData, 
						testData,
						datumTools.getInverseLabelIndicator("Weighted"));
		
		if (!validation.runAndOutput())
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to run validation.");
	}
	
	private static DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet(DocumentSetNLP<DocumentNLP> documentSet) {
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
				dataSet.add(
					new TokenSpansDatum<CategoryList>(datumId, 
							new TokenSpan[] { entry.getKey() }, 
							new CategoryList(entry.getValue().toArray(new String[0]), 0), 
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
		
		parser.accepts("trainDocumentSetName").withRequiredArg()
			.ofType(String.class)
			.describedAs("Name of the train document set");
		
		parser.accepts("devDocumentSetName").withRequiredArg()
			.ofType(String.class)
			.describedAs("Name of the dev document set");
		
		parser.accepts("testDocumentSetName").withRequiredArg()
			.ofType(String.class)
			.describedAs("Name of the test document set");
		
		parser.accepts("categoryType").withRequiredArg()
			.ofType(String.class)
			.describedAs("Category annotation type")
			.defaultsTo("freebase-type");
		
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
		trainDocumentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("trainDocumentSetName").toString());
		devDocumentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("devDocumentSetName").toString());
		testDocumentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("testDocumentSetName").toString());
		
		CatProperties properties = new CatProperties();
		String experimentInputPath = new File(properties.getContextInputDirPath(), "/GSTBinaryString" + experimentName + ".ctx").getAbsolutePath();
		String experimentOutputPath = new File(properties.getExperimentOutputDirPath(), trainDocumentSetName.toString() + "/GSTBinaryString/" + experimentName).getAbsolutePath(); 
		
		CatDataTools dataTools = new CatDataTools(new OutputWriter(
				new File(experimentOutputPath + ".debug.out"),
				new File(experimentOutputPath + ".data.out"),
				new File(experimentOutputPath + ".model.out"),
				new File(experimentOutputPath + ".results.out")
				), properties);
		
		dataTools.setRandomSeed(randomSeed);
		datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
		
		context = Context.deserialize(datumTools, FileUtil.getFileReader(experimentInputPath));
		if (context == null) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to deserialize context.");
			return false;
		}
	
		datumId = 0;
		
		categoryType = (AnnotationTypeNLP<String>)dataTools.getAnnotationTypeNLP(options.valueOf("categoryType").toString());
		
		return true;
	}
}