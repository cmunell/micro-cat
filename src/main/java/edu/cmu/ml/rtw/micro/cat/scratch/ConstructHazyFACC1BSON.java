package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.SerializerDocument;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPJSONLegacy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.store.StoredCollectionFileSystem;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford.JSONTokenizer;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Singleton;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.DocumentSetNLPFactory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.FACC1Annotation;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

/**
 * ConstructHazyFACC1BSON takes the original HazyFACC1 data set and runs it through
 * the newest version of the stanford pipeline, outputting to the BSON format given by
 * edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON
 * 
 * @author Bill McDowell
 *
 */
public class ConstructHazyFACC1BSON {
	private static CatProperties properties = new CatProperties();
	private static CatDataTools dataTools = new CatDataTools();
	private static SerializerDocumentNLPJSONLegacy jsonSerializer = new SerializerDocumentNLPJSONLegacy(dataTools);
	
	public static void main(String[] args) {
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> oldFacc1Docs = (DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>)DocumentSetNLPFactory.getDocumentSet(DocumentSetNLPFactory.SetName.HazyFacc1, properties, dataTools);
		
		List<AnnotationType<?>> annotationTypes = new ArrayList<>();
		annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
		annotationTypes.remove(AnnotationTypeNLP.ORIGINAL_TEXT);
		SerializerDocument<DocumentNLPMutable, Document> serializer = new SerializerDocumentNLPBSON(null, annotationTypes);
		
		File tempOutputDirectory = new File(dataTools.getProperties().getHazyFACC1BSONDirPath(), "temp");
		if (!tempOutputDirectory.exists() && !tempOutputDirectory.mkdir()) {
			dataTools.getOutputWriter().debugWriteln("Failed to create directory " + tempOutputDirectory.getAbsolutePath());
			System.exit(0);
		}
		
		StoredCollectionFileSystem<DocumentNLPMutable, Document> documents = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("temp", tempOutputDirectory, serializer);
		
		Random r = new Random(1);
		Set<String> oldDocNames = oldFacc1Docs.getDocumentNames();
		List<StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>> parts = oldFacc1Docs.makePartition(30, r);

		Singleton<Integer> count = new Singleton<Integer>(0);
		
		ThreadMapper<StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>, Boolean> mapper = new ThreadMapper<StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>, Boolean>(new ThreadMapper.Fn<StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>, Boolean>() {
			@Override
			public Boolean apply(StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> docs) {
				PipelineNLPStanford stanfordPipeline = new PipelineNLPStanford();
				stanfordPipeline.initialize(AnnotationTypeNLP.COREF, new JSONTokenizer());
				
				for (DocumentNLP doc : docs) {
					String docName = doc.getName();
					
					synchronized (count) {
						count.set(count.get() + 1);
					}
					
					System.out.println("Updating " + docName + "... (" + count.get() + "/" + oldDocNames.size() + ")");
					DocumentNLP oldDoc = oldFacc1Docs.getDocumentByName(docName, false);
					DocumentNLPMutable newDoc = constructUpdatedDocument(oldDoc, stanfordPipeline);
					if (newDoc == null) {
						System.out.println("Skipped " + docName + " (no facc1)");
						return true;
					} else {
						System.out.println("Finished updating " + docName + "... (" + count.get() + "/" + oldDocNames.size() + ")");
					}
					
					if (!documents.addItem(newDoc)) {
						dataTools.getOutputWriter().debugWriteln("Failed to store document " + newDoc.getName());
						System.exit(0);
					}
				}
				
				return true;
			} 
		});
		
		mapper.run(parts, 30);
		/*System.out.println("Finished updating documents (" + newDocs.size() + ").");
	
		
		List<DocumentNLPMutable> newDocsPerm = MathUtil.randomPermutation(r, newDocs);
		int trainEndIndex = (int)Math.floor(newDocsPerm.size() * .8);
		int devEndIndex = (int)Math.floor(newDocsPerm.size() * .9);
		
		if (!outputDocuments(newDocsPerm, 0, trainEndIndex, "train")) {
			System.out.println("Failed to output train docs.");
			System.exit(0);
		}
		
		if (!outputDocuments(newDocsPerm, trainEndIndex, devEndIndex, "dev")) {
			System.out.println("Failed to output dev docs.");
			System.exit(0);
		}
		
		
		if (!outputDocuments(newDocsPerm, devEndIndex, newDocsPerm.size(), "test")) {
			System.out.println("Failed to output test docs.");
			System.exit(0);
		}*/
	}
	
	private static DocumentNLPMutable constructUpdatedDocument(DocumentNLP oldDocument, PipelineNLPStanford stanfordPipeline) {
		if (/*oldDocument.getDocumentAnnotation(AnnotationTypeNLPCat.AMBIGUOUS_FACC1)
				||*/ oldDocument.getDocumentAnnotation(AnnotationTypeNLPCat.FAILED_FACC1))
			return null;
		
		List<Pair<TokenSpan, FACC1Annotation>> facc1 = oldDocument.getTokenSpanAnnotations(AnnotationTypeNLPCat.FACC1);

		PipelineNLPExtendable facc1Pipeline = new PipelineNLPExtendable();
		
		facc1Pipeline.extend(new AnnotatorTokenSpan<String>() {
			public String getName() { return "FACC1"; }
			public boolean measuresConfidence() { return false; }
			public AnnotationType<String> produces() { return AnnotationTypeNLPCat.FREEBASE_TOPIC; }
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
			public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
				List<Triple<TokenSpan, String, Double>> output = new ArrayList<Triple<TokenSpan, String, Double>>();
				for (Pair<TokenSpan, FACC1Annotation> annotation : facc1) {
					output.add(new Triple<TokenSpan,String,Double>(
						new TokenSpan(document, 
									  annotation.getFirst().getSentenceIndex(),
									  annotation.getFirst().getStartTokenIndex(),
									  annotation.getFirst().getEndTokenIndex()),
						annotation.getSecond().getFreebaseTopic(),
						null));
				}
				
				return output;
			}
		});
		
		facc1Pipeline.extend(new AnnotatorTokenSpan<String>() {
			public String getName() { return "FACC1"; }
			public boolean measuresConfidence() { return false; }
			public AnnotationType<String> produces() { return AnnotationTypeNLPCat.FREEBASE_TYPE; }
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
			public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
				List<Triple<TokenSpan, String, Double>> output = new ArrayList<Triple<TokenSpan, String, Double>>();
				for (Pair<TokenSpan, FACC1Annotation> annotation : facc1) {
					for (String freebaseType : annotation.getSecond().getFreebaseTypes()) {
						output.add(new Triple<TokenSpan,String,Double>(
							new TokenSpan(document, 
										  annotation.getFirst().getSentenceIndex(),
										  annotation.getFirst().getStartTokenIndex(),
										  annotation.getFirst().getEndTokenIndex()),
							freebaseType,
							null));
					}
				}
				return output;
			}
		});
		
		PipelineNLP pipeline = stanfordPipeline.weld(facc1Pipeline);
		DocumentNLPInMemory document = new DocumentNLPInMemory(dataTools, oldDocument.getName(), jsonSerializer.serializeToString((DocumentNLPMutable)oldDocument));
		pipeline.run(document);
		return document;
	}
	
}
