package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.util.List;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;

public class NELLMentionCategorizerTest {
	@Test
	public void testNELLMentionCategorizer() {
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		PipelineNLPExtendable pipelineExtendable = new PipelineNLPExtendable();
		pipelineExtendable.extend(new NELLMentionCategorizer());
		PipelineNLP pipeline = pipelineStanford.weld(pipelineExtendable);
		
		DocumentNLP document = new DocumentNLPInMemory(new CatDataTools(), 
				   "Test document", 
				   "I baked a cake in the oven.  Sam helped.",
				   Language.English, pipeline);

		List<Annotation> annotations = document.toMicroAnnotation().getAllAnnotations();
		for (Annotation annotation : annotations)
			System.out.println(annotation.toJsonString());
	}
}
