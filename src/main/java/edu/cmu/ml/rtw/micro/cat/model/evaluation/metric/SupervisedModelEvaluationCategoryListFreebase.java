package edu.cmu.ml.rtw.micro.cat.model.evaluation.metric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.InverseLabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.FACC1Annotation;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.FACC1DocumentNLPInMemory;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;

/**
 * SupervisedModelEvaluationCategoryListFreebase evaluates an assignment of noun-phrases to
 * NELL categories against the gold-standard Freebase types (according to the FACC1 data)
 * using the mapping NELL categories to Freebase types.
 * 
 * Parameters:
 *  computeNELLBaseline - indicates whether the baseline assignment of NELL categories to
 *  noun-phrases (independent of document context) should be evaluated
 *  
 *  NELLConfidenceThreshold - the confidence threshold above which a NELL category within
 *  a given CategoryList is considered to be assigned to a noun-phrase
 *  
 *  evaluation - determines the evaluation metric to compute (F1, precision,
 *  recall, or accuracy)
 * 
 * @author Bill McDowell
 *
 */
public class SupervisedModelEvaluationCategoryListFreebase extends SupervisedModelEvaluation<TokenSpansDatum<CategoryList>, CategoryList> {
	public enum EvaluationType {
		F1,
		Precision,
		Recall,
		Accuracy
	}
	
	private double NELLConfidenceThreshold;
	private boolean computeNELLBaseline;
	private EvaluationType evaluationType = EvaluationType.F1;
	
	private String[] parameterNames = { "computeNELLBaseline", "NELLConfidenceThreshold", "evaluationType" };
	
	public SupervisedModelEvaluationCategoryListFreebase() {
		
	}
	
	public SupervisedModelEvaluationCategoryListFreebase(DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context) {
		this.context = context;
	}
	
	@Override
	public String getGenericName() {
		return "CategoryListFreebase";
	}

	@Override
	protected double compute(
			SupervisedModel<TokenSpansDatum<CategoryList>, CategoryList> model,
			DataFeatureMatrix<TokenSpansDatum<CategoryList>, CategoryList> data,
			Map<TokenSpansDatum<CategoryList>, CategoryList> predictions) {
		
		double tp = 0.0;
		double fp = 0.0;
		double tn = 0.0;
		double fn = 0.0;
		

		NELLUtil nell = new NELLUtil((CatDataTools)data.getData().getDatumTools().getDataTools());
		List<String> freebaseCategories = nell.getFreebaseCategories();
		List<String> indicatorLabels = new ArrayList<String>();
		for (LabelIndicator<CategoryList> indicator : data.getData().getDatumTools().getLabelIndicators())
			if (freebaseCategories.contains(indicator.toString()))
				indicatorLabels.add(indicator.toString());
	
		Gazetteer freebaseNELLCategoryGazetteer = data.getData().getDatumTools().getDataTools().getGazetteer("FreebaseNELLCategory");
		InverseLabelIndicator<CategoryList> inverseLabelIndicator = data.getData().getDatumTools().getInverseLabelIndicator("UnweightedConstrained");
		for (Entry<TokenSpansDatum<CategoryList>, CategoryList> entry : predictions.entrySet()) {
			TokenSpan[] datumTokenSpans = entry.getKey().getTokenSpans();
			for (int i = 0; i < datumTokenSpans.length; i++) {
				FACC1DocumentNLPInMemory document = (FACC1DocumentNLPInMemory)datumTokenSpans[i].getDocument();
				if (!document.hasAnnotationType(AnnotationTypeNLPCat.FACC1))
					continue;
				
				List<Pair<TokenSpan, FACC1Annotation>> facc1Annotations = document.getTokenSpanAnnotations(AnnotationTypeNLPCat.FACC1);
				for (Pair<TokenSpan, FACC1Annotation> facc1Annotation : facc1Annotations) {
					if (facc1Annotation.getFirst().equals(datumTokenSpans[i])) {
						String[] freebaseTypes = facc1Annotation.getSecond().getFreebaseTypes();
						Set<String> actualFreebaseNELLCategories = new HashSet<String>();
						for (String freebaseType : freebaseTypes) {
							if (freebaseNELLCategoryGazetteer.contains(freebaseType))
								actualFreebaseNELLCategories.addAll(freebaseNELLCategoryGazetteer.getIds(freebaseType));
						}
						
						for (String nellCategory : indicatorLabels) {
							boolean predictedTrue = false;
							if (this.computeNELLBaseline) {
								List<Pair<String, Double>> nellCategoryWeights = nell.getNounPhraseNELLWeightedCategories(datumTokenSpans[i].toString(), this.NELLConfidenceThreshold);
								List<String> nellCategories = new ArrayList<String>();
								Map<String, Double> weights = new HashMap<String, Double>();
								for (Pair<String, Double> nellCategoryWeight : nellCategoryWeights) {
									weights.put(nellCategoryWeight.getFirst(), nellCategoryWeight.getSecond());
									nellCategories.add(nellCategoryWeight.getFirst());
								}
								CategoryList nellLabel = inverseLabelIndicator.label(weights, nellCategories);
								predictedTrue = nellLabel.contains(nellCategory);
							} else {
								predictedTrue = entry.getValue().contains(nellCategory) && entry.getValue().getCategoryWeight(nellCategory) >= 0.5;
							}
							
							boolean actualTrue = actualFreebaseNELLCategories.contains(nellCategory);
							if (predictedTrue && actualTrue)
								tp++;
							else if (predictedTrue && !actualTrue)
								fp++;
							else if (!predictedTrue && actualTrue)
								fn++;
							else if (!predictedTrue && !actualTrue)
								tn++;
						}
					}
				}
			}
		}
		
		if (this.evaluationType == EvaluationType.F1) {
			return 2*tp/(2*tp+fn+fp);
		} else if (this.evaluationType == EvaluationType.Precision) {
			return tp/(tp+fp);
		} else if (this.evaluationType == EvaluationType.Recall) {
			return tp/(tp+fn);
		} else if (this.evaluationType == EvaluationType.Accuracy) {
			return (tp+tn)/(tp+tn+fp+fn);
		} else {
			return 0;
		}
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("NELLConfidenceThreshold"))
			return Obj.stringValue(String.valueOf(this.NELLConfidenceThreshold));
		else if (parameter.equals("computeNELLBaseline"))
			return Obj.stringValue(String.valueOf(this.computeNELLBaseline));
		else if (parameter.equals("evaluationType"))
			return Obj.stringValue(this.evaluationType.toString());
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("NELLConfidenceThreshold"))
			this.NELLConfidenceThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("computeNELLBaseline"))
			this.computeNELLBaseline = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("evaluationType"))
			this.evaluationType = EvaluationType.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public SupervisedModelEvaluation<TokenSpansDatum<CategoryList>, CategoryList> makeInstance(DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context) {
		return new SupervisedModelEvaluationCategoryListFreebase(context);
	}
}
