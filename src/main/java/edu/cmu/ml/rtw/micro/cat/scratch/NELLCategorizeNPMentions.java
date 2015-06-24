package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Timer;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

/**
 * NELLCategorizeNPMentions is a command-line version 
 * of the noun-phrase mention categorization micro-reader 
 * (edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer)
 * 
 * @author Bill McDowell
 *
 */
public class NELLCategorizeNPMentions {
	public enum InputType {
		PLAIN_TEXT,
		JSON,
		MICRO
	}
	
	public enum OutputType {
		MICRO,
		JSON,
		TSV
	}
	
	public static final int DEFAULT_MAX_ANNOTATION_SENTENCE_LENGTH = 30;
	
	private static CatDataTools dataTools; 
	private static Datum.Tools<TokenSpansDatum<CategoryList>, CategoryList> datumTools;
	
	private static InputType inputType;
	private static OutputType outputType;
	private static int maxThreads;
	private static File outputDataLocation;
	private static File outputDocumentDir;
	private static List<File> inputFiles;
	private static NELLMentionCategorizer categorizer;
	private static PipelineNLPStanford nlpAnnotator;
	private static boolean appendOutput;
	private static long quittingTime;
	private static BufferedWriter dataWriter;
	private static DateTime annotationTime = DateTime.now();
	
	public static void main(String[] args) {
		Timer timer = new Timer();
		timer.startClock("");
		
		if (!parseArgs(args))
			return;
		
		dataTools.getOutputWriter().debugWriteln("Running annotation and models...");
		
		if (!initializeNlpPipeline())
			return;
		
		if (!initializeDataWriter())
			return;
		
		final String outputFileExtension = (outputType == OutputType.TSV) ? "tsv" : "json"; 
		ThreadMapper<File, Boolean> threads = new ThreadMapper<File, Boolean>(new ThreadMapper.Fn<File, Boolean>() {
			public Boolean apply(File file) {
				File outputDataFile = null; 
				if (outputDataLocation.isDirectory())
					outputDataFile = new File(outputDataLocation, file.getName() + ".data." + outputFileExtension);
				else
					outputDataFile = outputDataLocation;
				
				File outputDocumentFile = null;
				if (outputDocumentDir != null)
					outputDocumentFile = new File(outputDocumentDir, file.getName() + ".json");
					
				if (quittingTime > 0 && quittingTime <= timer.getClockRunTimeInMillis("")/1000.0) {
					dataTools.getOutputWriter().debugWriteln("Skipping file " + file.getName() + ".  Time limit reached. ");
					return true;
				}
				
				if (appendOutput && 
						(outputDataLocation.isDirectory() && outputDataFile.exists() 
							|| (outputDocumentFile != null && outputDocumentFile.exists())
								)) {
					dataTools.getOutputWriter().debugWriteln("Skipping file " + file.getName() + ".  Output already exists. ");
					return true;
				}
				
				dataTools.getOutputWriter().debugWriteln("Processing file " + file.getName() + "...");
				List<DocumentNLPInMemory> documents = new ArrayList<DocumentNLPInMemory>();
				if (inputType == InputType.PLAIN_TEXT) {
					DocumentNLPInMemory document = constructAnnotatedDocument(file);
					
					if (document == null) {
						dataTools.getOutputWriter().debugWriteln("ERROR: Failed to annotate document " + file.getName() + ". ");
						return false;
					}
					
					if (outputDocumentDir != null) {
						if (outputType == OutputType.MICRO) {
							document.toMicroAnnotation().writeToFile(outputDocumentFile.getAbsolutePath());
						} else if (!document.saveToJSONFile(outputDocumentFile.getAbsolutePath())) {
							dataTools.getOutputWriter().debugWriteln("ERROR: Failed to save annotated " + file.getName() + ". ");
							return false;
						}
					}
					
					documents.add(document);
				} else if (inputType == InputType.MICRO) {
					List<DocumentAnnotation> annotations = DocumentAnnotation.fromFile(file.getAbsolutePath());
					for (DocumentAnnotation annotation : annotations) {
						documents.add(new DocumentNLPInMemory(dataTools, annotation));
					}
				} else {
					documents.add(new DocumentNLPInMemory(dataTools, FileUtil.readJSONFile(file)));
				}
				
				DataSet<TokenSpansDatum<CategoryList>, CategoryList> labeledData = categorizer.categorizeNounPhraseMentions(documents.get(0));

				if (labeledData == null) {
					dataTools.getOutputWriter().debugWriteln("ERROR: Failed to run categorizer on " + file.getName() + ". ");
					return false;
				}
				
				for (int i = 1; i < documents.size(); i++) {
					DataSet<TokenSpansDatum<CategoryList>, CategoryList> moreLabeledData = categorizer.categorizeNounPhraseMentions(documents.get(i));
					
					if (moreLabeledData == null) {
						dataTools.getOutputWriter().debugWriteln("ERROR: Failed to run categorizer on " + file.getName() + ". ");
						return false;
					}
					
					labeledData.addAll(moreLabeledData);
				}
				

				List<JSONObject> jsonLabeledData = new ArrayList<JSONObject>();
				try {
					for (TokenSpansDatum<CategoryList> datum : labeledData) {
						jsonLabeledData.add(datumTools.datumToJSON(datum));
						TokenSpan span = datum.getTokenSpans()[0];
						DocumentNLP document = span.getDocument();
						jsonLabeledData.get(jsonLabeledData.size() - 1).put("startCharIndex", document.getToken(span.getSentenceIndex(), span.getStartTokenIndex()).getCharSpanStart());
						jsonLabeledData.get(jsonLabeledData.size() - 1).put("endCharIndex", document.getToken(span.getSentenceIndex(), span.getEndTokenIndex() - 1).getCharSpanEnd());
					}
				} catch (JSONException e) {
					return false;
				}
				
				
				if ((!outputDataLocation.isDirectory() && !outputData(jsonLabeledData))
						|| !outputData(jsonLabeledData, outputDataFile)) {
					dataTools.getOutputWriter().debugWriteln("ERROR: Failed to output data for " + file.getName() + ". ");
					return false;
				}
				
				return true;
			}
		});
		
		threads.run(inputFiles, maxThreads);
		
		if (!finalizeDataWriter())
			return;
		
		dataTools.getOutputWriter().debugWriteln("Finished running annotation and models.");
	}
	
	private static boolean initializeDataWriter() {
		if (!outputDataLocation.isDirectory()) {
			try {
				dataWriter = new BufferedWriter(new FileWriter(outputDataLocation, appendOutput));
			} catch (IOException e) {
				dataTools.getOutputWriter().debugWriteln("ERROR: Failed to open data writer.");
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean finalizeDataWriter() {
		if (dataWriter != null) {
			try {
				dataWriter.close();
			} catch (IOException e) {
				dataTools.getOutputWriter().debugWriteln("ERROR: Failed to close data writer.");
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean outputData(List<JSONObject> data) {
		OutputWriter output = dataTools.getOutputWriter();
		synchronized (dataWriter) {
			output.debugWriteln("Outputting data...");
			
			if (dataWriter == null) {
				dataTools.getOutputWriter().debugWriteln("ERROR: Failed to output data... data writer doesn't exist.");
				return false;
			}
				
			try {	
				String outputData = constructOutput(data);
				if (outputData == null) {
					dataTools.getOutputWriter().debugWriteln("ERROR: Output construction failed.");
					dataWriter.close();
					return false;
				}
				
				dataWriter.write(outputData);
			} catch (IOException e) {
				dataTools.getOutputWriter().debugWriteln("ERROR: Failed to output data. (" + e.getMessage() + ")");
				e.printStackTrace();
				return false;
			}
			
			output.debugWriteln("Finished outputting data.");
		}
		
		return true;
	}
	
	private static boolean outputData(List<JSONObject> data, File outputFile) {
		OutputWriter output = dataTools.getOutputWriter();
		output.debugWriteln("Outputting data to " + outputFile.getName() + "...");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			String outputData = constructOutput(data);
			if (outputData == null) {
				dataTools.getOutputWriter().debugWriteln("ERROR: Output construction failed for " + outputFile.getName() + ".");
				writer.close();
				return false;
			}
			
			writer.write(outputData);
			writer.close();
		} catch (IOException e) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to output data to " + outputFile.getName() + ". (" + e.getMessage() + ")");
			e.printStackTrace();
			return false;
		}
		output.debugWriteln("Finished outputting data to " + outputFile.getName() + ".");
		return true;
	}
	
	private static String constructOutput(List<JSONObject> outputData) {
		StringBuilder str = new StringBuilder();
		if (outputType == OutputType.TSV) {
			try {
				str.append("id\tdoc\tsen\tstok\tetok\tstr");
				CategoryList categories = categorizer.getValidCategories();
				for (String category : categories.getCategories())
					str.append("\t").append(category);
				str.append("\n");
				
				CategoryList allLabels = categorizer.getValidCategories();
				for (JSONObject outputDatum : outputData) {
					JSONObject tokenSpanObj = outputDatum.getJSONArray("tokenSpans").getJSONObject(0);
					str.append(outputDatum.getInt("id")).append("\t");
					str.append(tokenSpanObj.getString("document")).append("\t");
					str.append(tokenSpanObj.getInt("sentenceIndex")).append("\t");
					str.append(tokenSpanObj.getInt("startTokenIndex")).append("\t");
					str.append(tokenSpanObj.getInt("endTokenIndex")).append("\t");
					str.append(outputDatum.getString("str"));
					
					if (outputDatum.has("label")) {
						CategoryList datumLabels = CategoryList.fromString(outputDatum.getString("label"), dataTools);
						for (String label : allLabels.getCategories())
							str.append("\t").append(datumLabels.getCategoryWeight(label));
					}
					str.append("\n");
				}
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		} else if (outputType == OutputType.JSON) {
			for (JSONObject outputDatum : outputData)
				str.append(outputDatum.toString()).append("\n");		
		} else {
			try {
				for (JSONObject outputDatum : outputData) {
					CategoryList categories = CategoryList.fromString(outputDatum.getString("label"), dataTools); // FIXME This is dumb
					for (String category : categories.getCategories()) {
						str.append((new Annotation(outputDatum.getInt("charSpanStart"), 
												  outputDatum.getInt("charSpanEnd"), 
												  AnnotationTypeNLPCat.NELL_CATEGORY.getType(), 
												  categorizer.getName(), 
												  outputDatum.getString("document"), 
												  category, 
												  null, 
												  categories.getCategoryWeight(category), 
												  annotationTime, 
												  null)).toJsonString()).append("\n");
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return str.toString();
	}
	
	private static DocumentNLPInMemory constructAnnotatedDocument(File file) {
		String fileText = FileUtil.readFile(file);
		PipelineNLPStanford threadNlpAnnotator = new PipelineNLPStanford(nlpAnnotator);
		return new DocumentNLPInMemory(dataTools, file.getName(), fileText, Language.English, threadNlpAnnotator);
	}
	
	private static boolean initializeNlpPipeline() {
		if (!nlpAnnotator.initialize()) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to initialze nlp pipeline.");
			return false;
		}

		return true;
	}
	
	private static boolean parseArgs(String[] args) {
		OutputWriter output = new OutputWriter();
		OptionParser parser = new OptionParser();
		parser.accepts("inputType").withRequiredArg()
			.describedAs("PLAIN_TEXT, JSON, MICRO determines whether input file(s) are text,have NLP annotations in JSON, or have NLP annotations in micro-reading format")
			.defaultsTo("PLAIN_TEXT");
		parser.accepts("outputType").withRequiredArg()
			.describedAs("JSON, TSV, MICRO determines whether output data is stored as json objects, tab-separated table, or micro-reading format")
			.defaultsTo("JSON");
		parser.accepts("maxThreads").withRequiredArg()
			.describedAs("Maximum number of concurrent threads to use when annotating files")
			.ofType(Integer.class)
			.defaultsTo(1);
		parser.accepts("mentionModelThreshold").withRequiredArg()
			.describedAs("Confidence threshold above which NELL's beliefs are used to categorize noun-phrases without reference "
					 + "to the mention-trained models")
			.ofType(Double.class)
			.defaultsTo(NELLMentionCategorizer.DEFAULT_MENTION_MODEL_THRESHOLD);
		parser.accepts("featuresFile").withRequiredArg()
			.describedAs("Path to initialized feature file")
			.ofType(File.class)
			.defaultsTo(NELLMentionCategorizer.DEFAULT_FEATURES_FILE);
		parser.accepts("input").withRequiredArg()
			.describedAs("Path to input file or directory on which to run the noun-phrase categorization")
			.ofType(File.class);
		parser.accepts("modelFilePathPrefix").withRequiredArg()
			.describedAs("Prefix of paths to model files. Each model file path should start with this prefix and end with the NELL " +
					" category for which the model was trained")
			.defaultsTo(NELLMentionCategorizer.DEFAULT_MODEL_FILE_PATH_PREFIX);
		parser.accepts("validCategories").withRequiredArg()
			.describedAs("ALL_NELL_CATEGORIES, FREEBASE_NELL_CATEGORIES, or a list of categories by which to classify " 
					+ "noun-phrase mentions")
			.defaultsTo(NELLMentionCategorizer.DEFAULT_VALID_CATEGORIES.toString());
		parser.accepts("outputDataLocation").withRequiredArg()
			.describedAs("Path to noun-phrase mention categorization data output directory or file")
			.ofType(File.class);
		parser.accepts("outputDocumentDir").withRequiredArg()
			.describedAs("Optional path to NLP document annotation output directory")
			.ofType(File.class);
		parser.accepts("maxAnnotationSentenceLength").withRequiredArg()
			.describedAs("Maximum length of sentences that are considered when parsing the document")
			.ofType(Integer.class)
			.defaultsTo(DEFAULT_MAX_ANNOTATION_SENTENCE_LENGTH);
		parser.accepts("outputDebugFile").withRequiredArg()
			.describedAs("Optional path to debug output file")
			.ofType(File.class);
		parser.accepts("labelType").withRequiredArg()
			.describedAs("WEIGHTED, UNWEIGHTED, WEIGHTED_CONSTRAINED, or UNWEIGHTED_CONSTRAINED " +
						 " determines whether labels are constrained to be non-polysemous and/or weighted")
			.defaultsTo(NELLMentionCategorizer.DEFAULT_LABEL_TYPE.toString());
		parser.accepts("appendOutput").withRequiredArg()
			.describedAs("Indicates whether to append to existing output files or overwrite them.")
			.ofType(Boolean.class)
			.defaultsTo(false);
		parser.accepts("quittingTime").withRequiredArg()
			.describedAs("If non-zero, this is the number of seconds after which no more documents will be annotated.")
			.ofType(Long.class)
			.defaultsTo(0L);
		
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
		
		output.debugWriteln("Loading data tools (gazetteers etc)...");
		dataTools = new CatDataTools(output, new CatProperties());
		datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
		output.debugWriteln("Finished loading data tools.");
		
		inputType = InputType.valueOf(options.valueOf("inputType").toString());
		outputType = OutputType.valueOf(options.valueOf("outputType").toString());
		maxThreads = (int)options.valueOf("maxThreads");
		appendOutput = (boolean)options.valueOf("appendOutput");
		quittingTime = (long)options.valueOf("quittingTime");
		
		if (options.has("input")) {
			File input = (File)options.valueOf("input");
			inputFiles = new ArrayList<File>();
			if (input.isDirectory()) {
				inputFiles.addAll(Arrays.asList(input.listFiles()));
			} else {
				inputFiles.add(input);
			}
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'input' argument.");
			return false;
		}
		
		if (options.has("outputDataLocation")) {
			outputDataLocation = (File)options.valueOf("outputDataLocation");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'outputDataLocation' argument.");
			return false;
		}
		
		if (options.has("outputDocumentDir")) {
			outputDocumentDir = (File)options.valueOf("outputDocumentDir");
		}
		
		nlpAnnotator = new PipelineNLPStanford((Integer)options.valueOf("maxAnnotationSentenceLength"));
		
		if (options.has("outputDebugFile")) {
			dataTools.getOutputWriter().setDebugFile((File)options.valueOf("outputDebugFile"), appendOutput);
		}
		
		categorizer = new NELLMentionCategorizer(datumTools, 
												 CategoryList.fromString(options.valueOf("validCategories").toString(), dataTools),
												 (double)options.valueOf("mentionModelThreshold"),
												 NELLMentionCategorizer.LabelType.valueOf(options.valueOf("labelType").toString()),
												 (File)options.valueOf("featuresFile"),
												 options.valueOf("modelFilePathPrefix").toString(),
												 maxThreads);
	
		return true;
	}
}
