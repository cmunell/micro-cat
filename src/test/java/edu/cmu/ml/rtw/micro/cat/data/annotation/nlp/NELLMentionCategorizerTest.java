package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPMicro;
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
		
		DocumentNLPInMemory document = new DocumentNLPInMemory(new CatDataTools(), 
				   "Test document", 
				   "I baked a cake in the oven.  Barack Obama helped because I was " +
				   "the deciding vote in the next presidential election in the United States.");
		pipeline.run(document);
		
		SerializerDocumentNLPMicro serializer = new SerializerDocumentNLPMicro(new CatDataTools());
		
		List<Annotation> annotations = serializer.serialize(document).getAllAnnotations();
		for (Annotation annotation : annotations)
			System.out.println(annotation.toJsonString());
	}
	
	// FIXME @Test
	public void testDeserialization() {
		String modelStr = "model m=" +
						  "Areg(l1=\"0.0\", l2=\"1.0E-6\", convergenceEpsilon=\"0.001\", " + 
						  "maxEvaluationConstantIterations=\"500\", maxTrainingExamples=\"260001.0\", " +
						  "batchSize=\"100\", evaluationIterations=\"200\", weightedLabels=\"false\", " +
						  "classificationThreshold=\"0.9\", computeTestEvaluations=\"false\") { " +
						  "array validLabels=(\"false\", \"true\"); " +
						  "value featureVocabularySize=\"173250\"; " +
						  "value nonZeroWeights=\"107757\"; " +
						  "value bias=\"0.1221464574149594\"; " +
						  "array w_57200=(\"fner_PERSON\", \"-1.1522157009718756\", \"57200\"); " +
						  "array w_172144=(\"fsufh_ple\", \"-1.078403333184309\", \"172144\"); " +
						  "array w_85088=(\"fphrh1_people\", \"-0.8212790847351784\", \"85088\"); };";
	
		System.out.println(DatumContext.run(TokenSpansDatum.getCategoryListTools(new CatDataTools()), new StringReader(modelStr)).getModels().get(0).toParse(true).toString());
	}
}
