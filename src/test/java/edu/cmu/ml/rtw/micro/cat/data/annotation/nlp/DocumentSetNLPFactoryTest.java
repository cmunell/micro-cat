package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentSetNLP;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

public class DocumentSetNLPFactoryTest {
	@Test
	public void testDocumentSetNLP() {
		CatDataTools dataTools = new CatDataTools(new OutputWriter(), new CatProperties());
		DocumentSetNLP<DocumentNLP> documents = DocumentSetNLPFactory.getDocumentSet(DocumentSetNLPFactory.SetName.HazyFacc1,
											 dataTools.getProperties(),
											 dataTools);
		for (String documentName : documents.getDocumentNames()) {
			DocumentNLP document = documents.getDocumentByName(documentName);
			Assert.assertNotEquals(null, document);
			System.out.println(documentName + documents.getDocumentByName(documentName).toJSON().toString());
		}									 
	}
}
