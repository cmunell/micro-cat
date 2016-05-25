package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import edu.cmu.ml.rtw.generic.data.store.StoredCollectionFileSystem;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;

public class ConstructTrainDevTestDocumentSetPartition {
	private static CatDataTools dataTools = new CatDataTools();
		
	public static void main(String[] args) {
		String inputPath = args[0];
		String outputTrainPath = args[1];
		String outputDevPath = args[2];
		String outputTestPath = args[3];
		
		List<AnnotationType<?>> annotationTypes = new ArrayList<>();
		annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
		annotationTypes.remove(AnnotationTypeNLP.ORIGINAL_TEXT);
		SerializerDocument<DocumentNLPMutable, Document> serializer = new SerializerDocumentNLPBSON(new DocumentNLPInMemory(dataTools), annotationTypes);
		
		File outputTrainDir = new File(outputTrainPath);
		if (!outputTrainDir.exists() && !outputTrainDir.mkdir()) {
			dataTools.getOutputWriter().debugWriteln("Failed to create directory " + outputTrainDir.getAbsolutePath());
			System.exit(0);
		}
		
		File outputDevDir = new File(outputDevPath);
		if (!outputDevDir.exists() && !outputDevDir.mkdir()) {
			dataTools.getOutputWriter().debugWriteln("Failed to create directory " + outputDevDir.getAbsolutePath());
			System.exit(0);
		}
		
		
		File outputTestDir = new File(outputTestPath);
		if (!outputTestDir.exists() && !outputTestDir.mkdir()) {
			dataTools.getOutputWriter().debugWriteln("Failed to create directory " + outputTestDir.getAbsolutePath());
			System.exit(0);
		}
		
		Random r = new Random(1);
		StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("Input", new File(inputPath), serializer);
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> inputDocs =  new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		List<StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>> parts = inputDocs.makePartition(new double[] { .8, .1, .1 }, r);
		
		StoredCollectionFileSystem<DocumentNLPMutable, Document> train = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("Train", outputTrainDir, serializer);
		for (DocumentNLP doc : parts.get(0))
			train.addItem((DocumentNLPMutable)doc);
		
		StoredCollectionFileSystem<DocumentNLPMutable, Document> dev = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("Dev", outputDevDir, serializer);
		for (DocumentNLP doc : parts.get(1))
			dev.addItem((DocumentNLPMutable)doc);
		
		StoredCollectionFileSystem<DocumentNLPMutable, Document> test = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("Test", outputTestDir, serializer);
		for (DocumentNLP doc : parts.get(2))
			test.addItem((DocumentNLPMutable)doc);
	}
}
