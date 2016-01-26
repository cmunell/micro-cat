package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.File;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPJSONLegacy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPMicro;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.data.store.StoredCollectionFileSystem;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

/**
 * 
 * DocumentSetNLPFactory constructs NLP document sets 
 * from files stored on disk.  This gives a systematic
 * way in which experiments (e.g. 
 * edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinary) can
 * run on various sets of documents.
 * 
 * @author Bill McDowell
 *
 */
public class DocumentSetNLPFactory {
	public enum SetName {
		HazyFacc1,
		CoNLL_YAGO_train,
		CoNLL_YAGO_testa,
		CoNLL_YAGO_testb
	}
	
	public static DocumentSet<DocumentNLP, DocumentNLPMutable> getDocumentSet(SetName name, CatProperties properties, CatDataTools dataTools) {
		if (name == SetName.HazyFacc1) {
			SerializerDocumentNLPJSONLegacy serializer = new SerializerDocumentNLPJSONLegacy(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, JSONObject> storage = new StoredCollectionFileSystem<DocumentNLPMutable, JSONObject>("HazyFacc1", new File(properties.getHazyFacc1DataDirPath()), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.CoNLL_YAGO_train) {
			SerializerDocumentNLPMicro serializer = new SerializerDocumentNLPMicro(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, DocumentAnnotation> storage = new StoredCollectionFileSystem<DocumentNLPMutable, DocumentAnnotation>("CoNLL_YAGO_train", new File(properties.getCoNLLYagoDataDirPath(), "train"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);		
		} else if (name == SetName.CoNLL_YAGO_testa) {
			SerializerDocumentNLPMicro serializer = new SerializerDocumentNLPMicro(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, DocumentAnnotation> storage = new StoredCollectionFileSystem<DocumentNLPMutable, DocumentAnnotation>("CoNLL_YAGO_testa", new File(properties.getCoNLLYagoDataDirPath(), "testa"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.CoNLL_YAGO_testb) {
			SerializerDocumentNLPMicro serializer = new SerializerDocumentNLPMicro(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, DocumentAnnotation> storage = new StoredCollectionFileSystem<DocumentNLPMutable, DocumentAnnotation>("CoNLL_YAGO_testb", new File(properties.getCoNLLYagoDataDirPath(), "testb"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else {
			return null;
		}
	}
}
