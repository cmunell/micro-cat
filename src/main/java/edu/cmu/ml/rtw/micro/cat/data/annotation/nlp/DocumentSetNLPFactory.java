package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.File;

import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentSetNLP;
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
	
	public static DocumentSetNLP<DocumentNLP> getDocumentSet(SetName name, CatProperties properties, CatDataTools dataTools) {
		if (name == SetName.HazyFacc1) {
			return DocumentSet.loadFromJSONDirectory(properties.getHazyFacc1DataDirPath(), 
					new FACC1DocumentNLPInMemory(dataTools), 
					new DocumentSetNLP<DocumentNLP>("HazyFacc1"));
		} else if (name == SetName.CoNLL_YAGO_train) {
			return DocumentSetNLP.loadFromMicroPathThroughPipeline(
					"CoNLL_YAGO_train",
					new File(properties.getCoNLLYagoDataDirPath(), "train").getAbsolutePath(), 
					new DocumentNLPInMemory(dataTools));
		} else if (name == SetName.CoNLL_YAGO_testa) {
			return DocumentSetNLP.loadFromMicroPathThroughPipeline(
					"CoNLL_YAGO_testa",
					new File(properties.getCoNLLYagoDataDirPath(), "testa").getAbsolutePath(), 
					new DocumentNLPInMemory(dataTools));
		} else if (name == SetName.CoNLL_YAGO_testb) {
			return DocumentSetNLP.loadFromMicroPathThroughPipeline(
					"CoNLL_YAGO_testb",
					new File(properties.getCoNLLYagoDataDirPath(), "testb").getAbsolutePath(), 
					new DocumentNLPInMemory(dataTools));
		} else {
			return null;
		}
	}
}
