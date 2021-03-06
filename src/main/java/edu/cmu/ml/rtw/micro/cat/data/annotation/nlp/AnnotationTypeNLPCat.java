package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;

/**
 * 
 * AnnotationTypeNLPCat represents types of annotations that
 * the micro-cat project can add to NLP documents
 * 
 * @author Bill McDowell
 *
 */
public class AnnotationTypeNLPCat {
	public static final AnnotationTypeNLP<String> NELL_CATEGORY = new AnnotationTypeNLP<String>("nell-cat", String.class, Target.TOKEN_SPAN);
	public static final AnnotationTypeNLP<String> FREEBASE_TYPE = new AnnotationTypeNLP<String>("freebase-type", String.class, Target.TOKEN_SPAN);
	public static final AnnotationTypeNLP<String> FREEBASE_TOPIC = new AnnotationTypeNLP<String>("freebase-topic", String.class, Target.TOKEN_SPAN);
	public static final AnnotationTypeNLP<String> WIKI_URL = new AnnotationTypeNLP<String>("wiki-url", String.class, Target.TOKEN_SPAN);
	
	// NOTE: These use camel-cased instead of underscored names so that their backward compatible 
	// with previous versions of data
	public static final AnnotationTypeNLP<FACC1Annotation> FACC1 = new AnnotationTypeNLP<FACC1Annotation>("facc1", FACC1Annotation.class, Target.TOKEN_SPAN);
	public static final AnnotationTypeNLP<Boolean> FAILED_FACC1 = new AnnotationTypeNLP<Boolean>("failedFacc1", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> AMBIGUOUS_FACC1 = new AnnotationTypeNLP<Boolean>("ambiguousFacc1", Boolean.class, Target.DOCUMENT);
}
