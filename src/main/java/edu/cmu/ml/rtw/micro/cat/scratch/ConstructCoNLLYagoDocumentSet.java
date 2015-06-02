package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentSetNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford.JSONTokenizer;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;

public class ConstructCoNLLYagoDocumentSet {
	private static CatDataTools dataTools = new CatDataTools();
	private static PipelineNLPStanford stanfordPipeline;
	
	public static void main(String[] args) {
		File yagoInputFile = new File(args[0]);
		
		stanfordPipeline = new PipelineNLPStanford();
		stanfordPipeline.initialize(null, new JSONTokenizer());
		
		System.out.println("Parsing yago files into documents...");
		
		Map<String, List<List<String>>>[] documentSets = splitYagoDataIntoDocumentSets(yagoInputFile);
		Map<String, List<List<String>>> trainDocuments = documentSets[0];
		Map<String, List<List<String>>> testaDocuments = documentSets[1];
		Map<String, List<List<String>>> testbDocuments = documentSets[2];
		
		if (!constructAnnotatedDocumentSetFromYagoTsvLines("train", trainDocuments))
			dataTools.getOutputWriter().debugWriteln("Failed to construct train data.");
		if (!constructAnnotatedDocumentSetFromYagoTsvLines("testa", testaDocuments))
			dataTools.getOutputWriter().debugWriteln("Failed to construct testa data.");
		if (!constructAnnotatedDocumentSetFromYagoTsvLines("testb", testbDocuments))
			dataTools.getOutputWriter().debugWriteln("Failed to construct testb data.");
	}
	
	/*
	 * Takes text of form:
	 * -DOCSTART- (1 EU)
	 * EU      B       EU      --NME--
	 * rejects
	 * German  B       German  Germany http://en.wikipedia.org/wiki/Germany    11867   /m/0345h
	 * call
	 * to
	 * boycott
	 * British B       British United_Kingdom  http://en.wikipedia.org/wiki/United_Kingdom     31717   /m/07ssc
	 * lamb
	 * .
	 *
	 * Peter   B       Peter Blackburn --NME--
	 * Blackburn       I       Peter Blackburn --NME--
	 * [...]
	 */
	private static Map<String, List<List<String>>>[] splitYagoDataIntoDocumentSets(File yagoInputFile) {
		Map<String, List<List<String>>> trainData = new HashMap<String, List<List<String>>>();
		Map<String, List<List<String>>> testaData = new HashMap<String, List<List<String>>>();
		Map<String, List<List<String>>> testbData = new HashMap<String, List<List<String>>>();
		
		BufferedReader reader = FileUtil.getFileReader(yagoInputFile.getAbsolutePath());
		try {
			String documentName = null;
			String documentSet = null;
			List<List<String>> documentLines = null;
			List<String> sentenceLines = null;
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("-DOCSTART-")) {
					if (documentSet != null) {
						if (documentSet.equals("train"))
							trainData.put(documentName, documentLines);
						else if (documentSet.equals("testa"))
							testaData.put(documentName, documentLines);
						else if (documentSet.equals("testb"))
							testbData.put(documentName, documentLines);
					}
					
					String docId = line.substring("-DOCSTART- (".length(), line.length() - 1);
					String[] docIdParts = docId.split(" ");
					documentName = docIdParts[0] + "_" + docIdParts[1];
					
					if (docIdParts[0].endsWith("testa"))
						documentSet = "testa";
					else if (docIdParts[0].endsWith("testb"))
						documentSet = "testb";
					else
						documentSet = "train";
					
					documentLines = new ArrayList<List<String>>();
					sentenceLines = new ArrayList<String>();
				} else if (line.trim().length() == 0) {
					documentLines.add(sentenceLines);
					sentenceLines = new ArrayList<String>();
				} else {
					sentenceLines.add(line);
				}
			}
			
			if (documentSet.equals("train"))
				trainData.put(documentName, documentLines);
			else if (documentSet.equals("testa"))
				testaData.put(documentName, documentLines);
			else if (documentSet.equals("testb"))
				testbData.put(documentName, documentLines);
		} catch (IOException e) {
			return null;
		}

		@SuppressWarnings("unchecked")
		Map<String, List<List<String>>>[] returnData = new HashMap[3];
		returnData[0] = trainData;
		returnData[1] = testaData;
		returnData[2] = testbData;
		
		return returnData;
	}
	
	private static boolean constructAnnotatedDocumentSetFromYagoTsvLines(String name, Map<String, List<List<String>>> documentLines) {
		System.out.println("Constructing document set " + name + "...");
		
		DocumentSetNLP<DocumentNLP> documentSet = new DocumentSetNLP<DocumentNLP>(name);
		
		for (Entry<String, List<List<String>>> entry : documentLines.entrySet()) {
			System.out.println("Constructing document " + entry.getKey() + " for " + name + "...");
			DocumentNLP document = constructAnnotatedDocumentFromYagoTsvLines(entry.getKey(), entry.getValue());
			documentSet.add(document);
		}
		
		File outputDirectory = new File(dataTools.getProperties().getCoNLLYagoDataDirPath(), name);
		if (!outputDirectory.mkdir())
			return false;
		
		System.out.println("Outputting document set " + name + "...");
		
		return documentSet.saveToMicroDirectory(outputDirectory.getAbsolutePath(), dataTools.getAnnotationTypesNLP());
	}
	
	private static DocumentNLP constructAnnotatedDocumentFromYagoTsvLines(String documentName, List<List<String>> lines) {
		Pair<JSONObject, Map<AnnotationTypeNLP<String>, List<Pair<Triple<Integer, Integer, Integer>, String>>>> jsonAndAnnotations = constructJSONObjectAndAnnotationsFromYagoTsvLines(documentName, lines);
		
		PipelineNLPExtendable yagoPipeline = new PipelineNLPExtendable();
		yagoPipeline.extend(new AnnotatorTokenSpan<String>() {
			public String getName() { return "CoNLL-YAGO"; }
			public boolean measuresConfidence() { return false; }
			public AnnotationType<String> produces() { return AnnotationTypeNLPCat.WIKI_URL; }
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
			public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
				List<Pair<Triple<Integer, Integer, Integer>, String>> annotations = jsonAndAnnotations.getSecond().get(AnnotationTypeNLPCat.WIKI_URL);
				List<Triple<TokenSpan, String, Double>> output = new ArrayList<Triple<TokenSpan, String, Double>>();
				
				for (Pair<Triple<Integer, Integer, Integer>, String> annotation : annotations) {
					output.add(new Triple<TokenSpan, String, Double>(
								new TokenSpan(document, 
											  annotation.getFirst().getFirst(),
											  annotation.getFirst().getSecond(),
											  annotation.getFirst().getThird()),
								annotation.getSecond(),
								null));
				}
				
				return output;
			}
		});
		
		yagoPipeline.extend(new AnnotatorTokenSpan<String>() {
			public String getName() { return "CoNLL-YAGO"; }
			public boolean measuresConfidence() { return false; }
			public AnnotationType<String> produces() { return AnnotationTypeNLPCat.FREEBASE_TOPIC; }
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
			public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
				List<Pair<Triple<Integer, Integer, Integer>, String>> annotations = jsonAndAnnotations.getSecond().get(AnnotationTypeNLPCat.FREEBASE_TOPIC);
				List<Triple<TokenSpan, String, Double>> output = new ArrayList<Triple<TokenSpan, String, Double>>();
				
				for (Pair<Triple<Integer, Integer, Integer>, String> annotation : annotations) {
					output.add(new Triple<TokenSpan, String, Double>(
								new TokenSpan(document, 
											  annotation.getFirst().getFirst(),
											  annotation.getFirst().getSecond(),
											  annotation.getFirst().getThird()),
								annotation.getSecond(),
								null));
				}
				
				return output;
			}
		});
		
		yagoPipeline.extend(new AnnotatorTokenSpan<String>() {
			public String getName() { return "CoNLL-YAGO"; }
			public boolean measuresConfidence() { return false; }
			public AnnotationType<String> produces() { return AnnotationTypeNLPCat.FREEBASE_TYPE; }
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
			public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
				List<Pair<Triple<Integer, Integer, Integer>, String>> annotations = jsonAndAnnotations.getSecond().get(AnnotationTypeNLPCat.FREEBASE_TYPE);
				List<Triple<TokenSpan, String, Double>> output = new ArrayList<Triple<TokenSpan, String, Double>>();

				for (Pair<Triple<Integer, Integer, Integer>, String> annotation : annotations) {
					output.add(new Triple<TokenSpan, String, Double>(
								new TokenSpan(document, 
											  annotation.getFirst().getFirst(),
											  annotation.getFirst().getSecond(),
											  annotation.getFirst().getThird()),
								annotation.getSecond(),
								null));
				}
				
				return output;
			}
		});
		
		PipelineNLP pipeline = stanfordPipeline.weld(yagoPipeline);
		
		return new DocumentNLPInMemory(dataTools, documentName, jsonAndAnnotations.getFirst().toString(), Language.English, pipeline);
	}
	
	
	private static Pair<JSONObject, Map<AnnotationTypeNLP<String>, List<Pair<Triple<Integer, Integer, Integer>, String>>>> constructJSONObjectAndAnnotationsFromYagoTsvLines(String documentName, List<List<String>> lines) {
		Gazetteer freebaseGazetteer = dataTools.getGazetteer("FreebaseTypeTopic"); // FIXME Note this only includes types for which there are NELL categories
		JSONObject json = new JSONObject();
		int annotatedSpanStart = -1;
		int annotatedSpanEnd = -1;
		String wikiUrl = "";
		String freebaseTopic = "";
		List<String> freebaseTypes = null;
		
		Map<AnnotationTypeNLP<String>, List<Pair<Triple<Integer, Integer, Integer>, String>>> annotations = new HashMap<AnnotationTypeNLP<String>, List<Pair<Triple<Integer, Integer, Integer>, String>>>();
		annotations.put(AnnotationTypeNLPCat.FREEBASE_TOPIC, new ArrayList<Pair<Triple<Integer, Integer, Integer>, String>>());
		annotations.put(AnnotationTypeNLPCat.FREEBASE_TYPE, new ArrayList<Pair<Triple<Integer, Integer, Integer>, String>>());
		annotations.put(AnnotationTypeNLPCat.WIKI_URL, new ArrayList<Pair<Triple<Integer, Integer, Integer>, String>>());
		
		try {
			json.put("annotators", new JSONObject());
			json.getJSONObject("annotators").put("token", "CoNLL-YAGO");
			json.put("sentences", new JSONArray());
			int charIndex = 0;
			for (int sentenceIndex = 0; sentenceIndex < lines.size(); sentenceIndex++) {
				List<String> sentenceLines = lines.get(sentenceIndex);
				JSONObject sentenceJson = new JSONObject();
				JSONArray tokensJson = new JSONArray();
				sentenceJson.put("tokens", tokensJson);
				
				for (int j = 0; j < sentenceLines.size(); j++) {
					String[] lineParts = sentenceLines.get(j).split("\t");
					String token = lineParts[0];
					
					if (lineParts.length == 1) {
						addAnnotation(annotations, sentenceIndex, annotatedSpanStart, annotatedSpanEnd, wikiUrl, freebaseTopic, freebaseTypes);
						annotatedSpanStart = -1;
						annotatedSpanEnd = -1;
					} else if (lineParts[1].equals("B")) {
						addAnnotation(annotations, sentenceIndex, annotatedSpanStart, annotatedSpanEnd, wikiUrl, freebaseTopic, freebaseTypes);
						annotatedSpanStart = j;
						annotatedSpanEnd = j+1;
					} else {
						annotatedSpanEnd = j+1;
					}
					
					wikiUrl = "";
					freebaseTopic = "";
					freebaseTypes = null;
					
					if (lineParts.length > 4)
						wikiUrl = lineParts[4];
					if (lineParts.length > 6) {
						freebaseTopic = lineParts[6];
						freebaseTypes = freebaseGazetteer.getIds(freebaseTopic);
					}

					sentenceJson.getJSONArray("tokens").put(
							(new Token(null, token, charIndex, charIndex + token.length())).toJSON());
					charIndex += token.length() + 1;
				}
				
				addAnnotation(annotations, sentenceIndex, annotatedSpanStart, annotatedSpanEnd, wikiUrl, freebaseTopic, freebaseTypes);
				annotatedSpanStart = -1;
				annotatedSpanEnd = -1;
				json.getJSONArray("sentences").put(sentenceJson);
			}
		} catch (JSONException e) {
			return null;
		}
		
		return new Pair<JSONObject, Map<AnnotationTypeNLP<String>, List<Pair<Triple<Integer, Integer, Integer>, String>>>>(json, annotations);
	}
	
	private static void addAnnotation(Map<AnnotationTypeNLP<String>, List<Pair<Triple<Integer, Integer, Integer>, String>>> annotations, int sentenceIndex, int annotatedSpanStart, int annotatedSpanEnd, String wikiUrl, String freebaseTopic, List<String> freebaseTypes) {
		if (annotatedSpanStart < 0)
			return;
		
		Triple<Integer, Integer, Integer> spanTriple = new Triple<Integer, Integer, Integer>(sentenceIndex, annotatedSpanStart, annotatedSpanEnd);
		
		annotations.get(AnnotationTypeNLPCat.FREEBASE_TOPIC).add(new Pair<Triple<Integer, Integer, Integer>, String>(
				spanTriple, freebaseTopic));
		annotations.get(AnnotationTypeNLPCat.WIKI_URL).add(new Pair<Triple<Integer, Integer, Integer>, String>(
				spanTriple, wikiUrl));
		
		if (freebaseTypes == null)
			freebaseTypes = new ArrayList<String>();
		if (freebaseTypes.size() == 0)
			freebaseTypes.add("");

		for (String freebaseType : freebaseTypes)
			annotations.get(AnnotationTypeNLPCat.FREEBASE_TYPE).add(new Pair<Triple<Integer, Integer, Integer>, String>(
					spanTriple, freebaseType));
		return;
	}
}
