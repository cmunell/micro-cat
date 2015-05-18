package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.ValidationGSTBinary;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.DocumentSetNLPFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLDataSetFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;


public class TrainGSTBinaryNELLNormalized {
	private static String experimentName;
	private static int randomSeed;
	private static double nellConfidenceThreshold;
	private static int nonPolysemousExamplesPerLabel;
	private static int polysemousTestExamples;
	private static int lowConfidenceTestExamples;
	private static int noBeliefTestExamples;
	private static String nonPolysemousDataSetName;
	private static DocumentSetNLPFactory.SetName documentSetName;

	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		CatProperties properties = new CatProperties();
		String experimentInputPath = new File(properties.getContextInputDirPath(), "/GSTBinaryNELLNormalized/" + experimentName + ".ctx").getAbsolutePath();
		
		CatDataTools dataTools = new CatDataTools(new OutputWriter(), properties);
		dataTools.setRandomSeed(randomSeed);
		
		TokenSpansDatum.Tools<CategoryList> datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
		Context<TokenSpansDatum<CategoryList>, CategoryList> context = Context.deserialize(datumTools, FileUtil.getFileReader(experimentInputPath));
		if (context == null) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to deserialize context.");
			return;
		}
		
		CategoryList categories = new CategoryList(context.getStringArray("validLabels").toArray(new String[0]), null, 0);
		
		NELLDataSetFactory.MentionDataSetCollection dataSets = 
				new NELLDataSetFactory.MentionDataSetCollection(datumTools, 
																categories,
																documentSetName, 
																nellConfidenceThreshold, 
																lowConfidenceTestExamples, 
																noBeliefTestExamples, 
																polysemousTestExamples, 
																nonPolysemousDataSetName, 
																nonPolysemousExamplesPerLabel);
		
		String experimentOutputPath = new File(properties.getExperimentOutputDirPath(), documentSetName.toString() + "_" + nonPolysemousDataSetName + "/GSTBinaryNELLNormalized/" + experimentName).getAbsolutePath(); 
		
		dataTools.getOutputWriter().setDebugFile(new File(experimentOutputPath + ".debug.out"), false);
		dataTools.getOutputWriter().setDataFile(new File(experimentOutputPath + ".data.out"), false);
		dataTools.getOutputWriter().setModelFile(new File(experimentOutputPath + ".model.out"), false);
		dataTools.getOutputWriter().setResultsFile(new File(experimentOutputPath + ".results.out"), false);
		
		Map<String, DataSet<TokenSpansDatum<CategoryList>, CategoryList>> compositeTestDataSets = new HashMap<String, DataSet<TokenSpansDatum<CategoryList>, CategoryList>>();
		compositeTestDataSets.put("No-belief", dataSets.getNoBeliefTestData());
		compositeTestDataSets.put("Low-confidence", dataSets.getLowConfidenceTestData());
		compositeTestDataSets.put("Polysemous", dataSets.getPolysemousTestData());
		compositeTestDataSets.put("Non-polysemous", dataSets.getNonPolysemousTestData());
		
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Setting up binary GST validation...");
		
		ValidationGSTBinary<TokenSpansDatum<Boolean>,TokenSpansDatum<CategoryList>,CategoryList> validation = 
				new ValidationGSTBinary<TokenSpansDatum<Boolean>, TokenSpansDatum<CategoryList>, CategoryList>(
						experimentName, 
						context,
						dataSets.getNonPolysemousTrainData(), 
						dataSets.getDevData(), 
						dataSets.getTestData(),
						datumTools.getInverseLabelIndicator("UnweightedConstrained"),
						compositeTestDataSets);
		
		if (!validation.runAndOutput())
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to run validation.");
	}
	
	private static boolean parseArgs(String[] args) {
		OptionParser parser = new OptionParser();
		
		parser.accepts("experimentName").withRequiredArg()
			.describedAs("Name of the training experiment")
			.ofType(String.class);
		
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
		randomSeed = (int)options.valueOf("randomSeed");
		nellConfidenceThreshold = (double)options.valueOf("nellConfidenceThreshold");
		nonPolysemousExamplesPerLabel = (int)options.valueOf("nonPolysemousExamplesPerLabel");
		polysemousTestExamples = (int)options.valueOf("polysemousTestExamples");
		lowConfidenceTestExamples = (int)options.valueOf("lowConfidenceTestExamples");
		noBeliefTestExamples = (int)options.valueOf("noBeliefTestExamples");
		nonPolysemousDataSetName = options.valueOf("nonPolysemousDataSetName").toString();
		documentSetName = DocumentSetNLPFactory.SetName.valueOf(options.valueOf("documentSetName").toString());
		
		return true;
	}
}
