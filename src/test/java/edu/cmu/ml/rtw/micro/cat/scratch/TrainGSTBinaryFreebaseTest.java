package edu.cmu.ml.rtw.micro.cat.scratch;





import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.model.evaluation.ValidationGSTBinary;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;

public class TrainGSTBinaryFreebaseTest {
	@Test
	public void testTrainGSTBinary() {
		int randomSeed = 2;
		CatDataTools dataTools = new CatDataTools(null);
		dataTools.setRandomSeed(randomSeed);
		TokenSpansDatum.Tools<CategoryList> datumTools = TokenSpansDatum.getCategoryListTools(dataTools);
		DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context = constructContext(datumTools);
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		pipelineStanford.initialize(AnnotationTypeNLP.COREF);
		
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> trainData = constructDataSet(pipelineStanford, context, 30, context.getMatchArray(Obj.curlyBracedValue("validLabels")));
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> devData = constructDataSet(pipelineStanford, context, 30, context.getMatchArray(Obj.curlyBracedValue("validLabels")));
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> testData = constructDataSet(pipelineStanford, context, 30, context.getMatchArray(Obj.curlyBracedValue("validLabels")));
		
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Setting up binary GST validation...");
		
		CategoryList categories = new CategoryList(context.getStringArray("validLabels").toArray(new String[0]), null, 0);
		for (String category : categories.getCategories()) {
			LabelIndicator<CategoryList> labelIndicator = new LabelIndicator<CategoryList>() {
				public String toString() {
					return category.replace('/', '_');
				}
				
				@Override
				public boolean indicator(CategoryList labelList) {
					return labelList.contains(category);
				}
				
				@Override
				public double weight(CategoryList labelList) {
					return labelList.getCategoryWeight(category);
				}
			};
			
			datumTools.addLabelIndicator(labelIndicator);
		}
		
		ValidationGSTBinary<TokenSpansDatum<Boolean>,TokenSpansDatum<CategoryList>,CategoryList> validation = 
				new ValidationGSTBinary<TokenSpansDatum<Boolean>, TokenSpansDatum<CategoryList>, CategoryList>(
						"experiment", 
						context,
						trainData,
						devData, 
						testData,
						datumTools.getInverseLabelIndicator("Weighted"));
		
		validation.runAndOutput();
	}
	
	private DataSet<TokenSpansDatum<CategoryList>, CategoryList> constructDataSet(PipelineNLPStanford pipelineStanford, DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context, int size, List<String> labels) {
		
		PipelineNLPExtendable pipelineExtendable = new PipelineNLPExtendable();
		
		pipelineExtendable.extend(new AnnotatorTokenSpan<String>() {
			public String getName() { return "Test"; }
			public boolean measuresConfidence() { return false; }
			public AnnotationType<String> produces() { return AnnotationTypeNLPCat.FREEBASE_TYPE; }
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
			public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
				List<Triple<TokenSpan, String, Double>> output = new ArrayList<Triple<TokenSpan, String, Double>>();

				for (int i = 0; i < document.getSentenceCount(); i++) {
					for (int j = 0; j < document.getSentenceTokenCount(i); j++) {
						if (context.getDataTools().getGlobalRandom().nextDouble() > .5) {
							int lIndex = context.getDataTools().getGlobalRandom().nextInt(labels.size());
							String label = labels.get(lIndex);
							if (lIndex % 2 == i % 2)
								output.add(new Triple<>(new TokenSpan(document, i, j, j+1), label, null));
						}
					}
				}
				
				return output;
			}
		});
		
		PipelineNLP pipeline = pipelineStanford.weld(pipelineExtendable);
		DataSet<TokenSpansDatum<CategoryList>, CategoryList> data = new DataSet<>(context.getDatumTools(), null);
		
		for (int i = 0; i < size; i++) {
			DocumentNLPInMemory document = new DocumentNLPInMemory(context.getDataTools(), 
					   "Test document " + i, 
					   "I baked a cake in the oven.  Barack Obama helped. I baked a cake in the oven.  Barack Obama helped.  I baked a cake in the oven.");
			document = (DocumentNLPInMemory)pipeline.run(document);
			
			List<Pair<TokenSpan, String>> types = document.getTokenSpanAnnotations(AnnotationTypeNLPCat.FREEBASE_TYPE);
			for (Pair<TokenSpan, String> pair : types) {
				
				data.add(new TokenSpansDatum<CategoryList>(context.getDataTools().getIncrementId(), pair.getFirst(), new CategoryList(new String[] {pair.getSecond()}, 0), false));
			}
		}
		
		return data;
	}
	
	private DatumContext<TokenSpansDatum<CategoryList>, CategoryList> constructContext(TokenSpansDatum.Tools<CategoryList> datumTools) {
		return DatumContext.run(datumTools, 
				"value randomSeed=SetRandomSeed(seed=\"2\");\n" +
				"value maxThreads=\"33\";\n" + 
				"value trainOnDev=\"false\";\n" +
				"value errorExampleExtractor=\"FirstTokenSpan\";\n" +
				"array validLabels=(\n" +
	/*			"\"/architecture/architect\",\"/architecture/building\",\"/architecture/lighthouse_construction_material\",\n" +
				"\"/architecture/museum\",\"/architecture/skyscraper\",\"/automotive/company\",\"/automotive/engine\",\"/automotive/model\",\n" +
				"\"/aviation/airport\",\"/base/terrorism/terrorist_organization\",\"/biology/animal\",\"/biology/protein\",\n" +
				"\"/book/author\",\"/book/book\",\"/book/magazine\",\"/book/newspaper\",\"/book/poem\",\"/broadcast/radio_station\",\n" +
				"\"/broadcast/tv_station\",\"/business/business_operation\",\"/business/industry\",\"/business/shopping_center\",\n" +
				"\"/business/trade_union\",\"/celebrities/celebrity\",\"/chemistry/chemical_compound\",\"/chemistry/chemical_element\",\n" +
				"\"/computer/programming_language\",\"/conferences/conference_series\",\"/cvg/computer_videogame\",\"/cvg/cvg_platform\",\n" +
				"\"/dining/chef\",\"/dining/restaurant\",\"/education/educational_institution\",\"/education/field_of_study\",\n" +
				"\"/education/university\",\"/fashion/garment\",\"/film/film\",\"/film/film_festival\",\"/finance/currency\",\n" +
				"\"/food/beverage\",\"/food/cheese\",\"/food/food\",\"/games/game\",\"/geography/geographical_feature\",\n" +
				"\"/geography/island\",\"/geography/lake\",\"/geography/mountain\",\"/geography/mountain_range\",\"/geography/river\",\n" +
				"\"/government/government_agency\",\"/government/government_office_or_title\",\"/government/political_party\",\n" +
				"\"/government/politician\",\"/interests/hobby\",\"/internet/blog\",\"/internet/website\",\"/language/human_language\",\n" +
				"\"/location/citytown\",\"/location/continent\",\"/location/country\",\"/location/geocode\",\"/location/location\",\n" +
				"\"/location/us_county\",\"/medicine/anatomical_structure\",\"/medicine/bone\",\"/medicine/disease\",\"/medicine/drug\",\n" +
				"\"/medicine/hospital\",\"/medicine/medical_treatment\",\"/military/military_conflict\",\"/music/album\",\"/music/artist\",\n" +
				"\"/music/composition\",\"/music/festival\",\"/music/genre\",\"/music/group_member\",\"/music/instrument\",\n" +
				"\"/music/record_label\",\"/organization/non_profit_organization\",\"/organization/organization\",\"/people/ethnicity\",\n" +
				"\"/people/person\",\"/people/profession\",\"/protected_sites/protected_site\",\"/religion/place_of_worship\",\n" +
				"\"/religion/religion\",\"/royalty/monarch\",\"/spaceflight/astronaut\",\"/sports/pro_athlete\",\"/sports/sport\",\n" +
				"\"/sports/sports_championship\",\"/sports/sports_equipment\",\"/sports/sports_facility\",\"/sports/sports_league\",\n" +
				"\"/sports/sports_team\",\"/time/day_of_week\",\"/time/event\",\"/time/month\",\"/transportation/bridge\",\"/travel/accommodation\",\n" +*/
				/*"\"/travel/tourist_attraction\",\"/travel/transportation_mode\",\"/tv/tv_network\",\"/tv/tv_program\",\n" +
				"\"/visual_art/art_period_movement\",\"/visual_art/color\",\"/visual_art/visual_art_form\",\n" +*/
				"\"/visual_art/visual_artist\",\"/wine/wine_producer\");\n" +

				"evaluation accuracy=Accuracy();\n" +
				"evaluation accuracyBase=Accuracy(computeBaseline=\"true\");\n" +
				"evaluation f.5=F(mode=\"MACRO_WEIGHTED\", filterLabel=\"true\", Beta=\"0.5\");\n" +
				"evaluation f1=F(mode=\"MACRO_WEIGHTED\", filterLabel=\"true\", Beta=\"1\");\n" +
				"evaluation prec=Precision(weighted=\"false\", filterLabel=\"true\");\n" +
				"evaluation recall=Recall(weighted=\"false\", filterLabel=\"true\");\n" +

				"ts_fn head=Head();\n" +
				"ts_fn ins1=NGramInside(n=\"1\", noHead=\"true\");\n" +
				"ts_fn ins2=NGramInside(n=\"2\", noHead=\"true\");\n" +
				"ts_fn ins3=NGramInside(n=\"3\", noHead=\"true\");\n" +
				"ts_fn ctxb1=NGramContext(n=\"1\", type=\"BEFORE\");\n" +
				"ts_fn ctxa1=NGramContext(n=\"1\", type=\"AFTER\");\n" +
				"ts_fn sent1=NGramSentence(n=\"1\", noSpan=\"true\");\n" +
				"ts_str_fn pos=PoSUniversal();\n" +
				"ts_str_fn strDef=String(cleanFn=\"CatDefaultCleanFn\");\n" +
				"ts_str_fn strStem=String(cleanFn=\"CatStemCleanFn\");\n" +
				"ts_str_fn strBoW=String(cleanFn=\"CatBagOfWordsFeatureCleanFn\");\n" +
				"str_fn pre=Affix(nMin=\"3\", nMax=\"3\", type=\"PREFIX\"); \n" +
				"str_fn suf=Affix(nMin=\"3\", nMax=\"3\", type=\"SUFFIX\"); \n" +
				
				"ts_fn insAll1=NGramInside(n=\"1\");\n" +
				"ts_fn dep=DependencyRelation();\n" +
				"ts_str_fn lemma = TokenAnnotation(annotationType=\"lemma\");\n" +
				"ts_str_fn posLemma = RelationStr(f1=${pos}, f2=${lemma}, relationSymbol=\"-\");\n" +
				"ts_fn beforeAfterS3 = NGramContext(type=\"BEFORE_AND_AFTER\", n=\"3\", allowSqueeze=\"true\");\n" +
				"ts_str_fn depLemma = TokenSpanPathStr(mode=\"ALL\", pathLength=\"1\", spanFn1=${dep}, strFn=${posLemma}, multiRelation=\"true\");\n" + 
				"ts_str_fn depRel = TokenSpanPathStr(mode=\"ONLY_RELATIONS\", pathLength=\"1\", spanFn1=${dep}, strFn=${posLemma}, multiRelation=\"true\");\n" + 
				
				"feature fpos=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=${pos});\n" +
				"feature fposb1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${pos} o ${ctxb1}));\n" +
				"feature fposa1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${pos} o ${ctxa1}));\n" +
				"feature fctxb1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${strDef} o ${ctxb1}));\n" +
				"feature fctxa1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${strDef} o ${ctxa1}));\n" +
				"feature fphrh1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${strDef} o ${head}));\n" +
				"feature fphr2=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${strDef} o ${ins2}));\n" +
				"feature fphr3=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${strDef} o ${ins3}));\n" +
				"feature ftcnt=TokenCount(maxCount=\"5\", tokenExtractor=\"AllTokenSpans\");\n" +
				"feature fform=StringForm(stringExtractor=\"FirstTokenSpan\", minFeatureOccurrence=\"2\");\n" +
				"feature fpreh=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${pre} o ${strDef} o ${head}));\n" +
				"feature fsufh=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${suf} o ${strDef} o ${head}));\n" +
				"feature fpre=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${pre} o ${strDef} o ${ins1}));\n" +
				"feature fsuf=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${suf} o ${strDef} o ${ins1}));\n" +
				"feature fsent1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${strDef} o ${sent1}));\n" +
				"feature fner=Ner(useTypes=\"true\", tokenExtractor=\"AllTokenSpans\");\n" +
				
				"feature fctx3=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${posLemma} o ${insAll1} o ${beforeAfterS3}));\n" +
				"feature fins1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=(${posLemma} o ${insAll1}));\n" +
				"feature fdep=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=${depLemma});\n" +
				"feature fdepRel=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"AllTokenSpans\", fn=${depRel});\n" +

				"model lr=Areg(l1=\"0\", l2=\"0\", convergenceEpsilon=\".00001\", maxTrainingExamples=\"1000001\", batchSize=\"200\", evaluationIterations=\"200\", maxEvaluationConstantIterations=\"500\", weightedLabels=\"false\", computeTestEvaluations=\"false\")\n" +
				"{\n" +
				"	array validLabels=${validLabels};\n" +
				"};\n" +
/*
				"gs g=GridSearch() {\n" +
				"	dimension l2=Dimension(name=\"l2\", values=(.000001,.00001,.0001,.001,.01,.1,1), trainingDimension=\"true\");\n" +
				"	dimension ct=Dimension(name=\"classificationThreshold\", values=(.5,.7,.9), trainingDimension=\"false\");\n" +
	
				"	model model=${lr};\n" +
				"	evaluation evaluation=${f1};\n" +
				"};"*/
				

				"gs g=GridSearch() {\n" +
				"	dimension l2=Dimension(name=\"l2\", values=(.000001, .00001), trainingDimension=\"true\");\n" +
				"	dimension ct=Dimension(name=\"classificationThreshold\", values=(.5, .7), trainingDimension=\"false\");\n" +
	
				"	model model=${lr};\n" +
				"	evaluation evaluation=${f1};\n" +
				"};"
		);
	}
}
