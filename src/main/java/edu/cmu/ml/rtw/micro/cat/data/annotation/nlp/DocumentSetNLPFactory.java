package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentSetNLP;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

public class DocumentSetNLPFactory {
	public enum SetName {
		HazyFacc1
	}
	
	public static DocumentSetNLP<DocumentNLP> getDocumentSet(SetName name, CatProperties properties, CatDataTools dataTools) {
		if (name == SetName.HazyFacc1) {
			return DocumentSet.loadFromJSONDirectory(properties.getHazyFacc1DataDirPath(), 
					new FACC1DocumentNLPInMemory(dataTools), 
					new DocumentSetNLP<DocumentNLP>("HazyFacc1"));
		} else {
			return null;
		}
	}
}
