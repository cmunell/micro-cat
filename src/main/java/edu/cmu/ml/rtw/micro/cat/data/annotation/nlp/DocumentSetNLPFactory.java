package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.File;

import org.bson.Document;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPJSONLegacy;
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
		HazyFacc1Bson,
		HazyFacc1BsonTrain,
		HazyFacc1BsonDev,
		HazyFacc1BsonTest,
		CoNLL_YAGO_train,
		CoNLL_YAGO_testa,
		CoNLL_YAGO_testb
	}
	
	public static DocumentSet<DocumentNLP, DocumentNLPMutable> getDocumentSet(SetName name, CatProperties properties, CatDataTools dataTools) {
		if (name == SetName.HazyFacc1) {
			SerializerDocumentNLPJSONLegacy serializer = new SerializerDocumentNLPJSONLegacy(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, JSONObject> storage = new StoredCollectionFileSystem<DocumentNLPMutable, JSONObject>("HazyFacc1", new File(properties.getHazyFacc1DataDirPath()), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.HazyFacc1Bson) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("HazyFacc1Bson", new File(properties.getHazyFACC1BSONDirPath(), "temp"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.HazyFacc1BsonTrain) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("HazyFacc1BsonTrain", new File(properties.getHazyFACC1BSONDirPath(), "train"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.HazyFacc1BsonDev) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("HazyFacc1BsonDev", new File(properties.getHazyFACC1BSONDirPath(), "dev"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.HazyFacc1BsonTest) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("HazyFacc1BsonTest", new File(properties.getHazyFACC1BSONDirPath(), "test"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.CoNLL_YAGO_train) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("CoNLL_YAGO_train", new File(properties.getCoNLLYagoDataDirPath(), "train"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);		
		} else if (name == SetName.CoNLL_YAGO_testa) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("CoNLL_YAGO_testa", new File(properties.getCoNLLYagoDataDirPath(), "testa"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else if (name == SetName.CoNLL_YAGO_testb) {
			SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(dataTools);
			StoredCollectionFileSystem<DocumentNLPMutable, Document> storage = new StoredCollectionFileSystem<DocumentNLPMutable, Document>("CoNLL_YAGO_testb", new File(properties.getCoNLLYagoDataDirPath(), "testb"), serializer);
			return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(storage);
		} else {
			return null;
		}
	}
}
