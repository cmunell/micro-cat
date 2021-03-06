package edu.cmu.ml.rtw.micro.cat.model.evaluation.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;

/**
 * SupervisedModelEvaluationPolysemy computes the fraction of 
 * noun-phrases in a data set that a model has labeled with 
 * a polysemous (mutually-exclusive) set of NELL categories.
 * 
 * Parameters:
 *  computeNELLBaseline - indicates whether the NELL baseline
 *  labeling (independent of document context) should be evaluated.
 *  
 *  NELLConfidenceThreshold - minimum confidence above which the 
 *  NELL baseline assigns a category to a noun-phrase
 * 
 * @author Bill McDowell
 *
 */
public class SupervisedModelEvaluationPolysemy extends SupervisedModelEvaluation<TokenSpansDatum<CategoryList>, CategoryList> {
	private double NELLConfidenceThreshold;
	private boolean computeNELLBaseline;
	
	private String[] parameterNames = { "computeNELLBaseline", "NELLConfidenceThreshold" };
	
	public SupervisedModelEvaluationPolysemy() {
		
	}
	
	public SupervisedModelEvaluationPolysemy(DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context) {
		this.context = context;
	}
	
	@Override
	public String getGenericName() {
		return "Polysemy";
	}

	@Override
	protected double compute(
			SupervisedModel<TokenSpansDatum<CategoryList>, CategoryList> model,
			DataFeatureMatrix<TokenSpansDatum<CategoryList>, CategoryList> data,
			Map<TokenSpansDatum<CategoryList>, CategoryList> predictions) {
		List<String> indicatorLabels = new ArrayList<String>();
		for (LabelIndicator<CategoryList> indicator : data.getData().getDatumTools().getLabelIndicators())
			indicatorLabels.add(indicator.toString());

		NELLUtil nell = new NELLUtil((CatDataTools)data.getData().getDatumTools().getDataTools());
		double polysemous = 0.0;
		for (Entry<TokenSpansDatum<CategoryList>, CategoryList> entry : predictions.entrySet()) {
			String np = entry.getKey().getTokenSpans()[0].toString();
			if (this.computeNELLBaseline) {
				polysemous += (nell.isNounPhrasePolysemous(np, this.NELLConfidenceThreshold)) ? 1.0 : 0.0;
			} else {
				List<String> labels = new ArrayList<String>();
				for (String label : entry.getValue().getCategories()) {
					if (entry.getValue().getCategoryWeight(label) >= 0.5)
						labels.add(label);
				}
				polysemous += nell.areCategoriesMutuallyExclusive(labels) ? 1.0 : 0.0;
			}
		}
		
		
		return polysemous / predictions.size();
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
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("NELLConfidenceThreshold"))
			this.NELLConfidenceThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("computeNELLBaseline"))
			this.computeNELLBaseline = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public SupervisedModelEvaluation<TokenSpansDatum<CategoryList>, CategoryList> makeInstance(DatumContext<TokenSpansDatum<CategoryList>, CategoryList> context) {
		return new SupervisedModelEvaluationPolysemy(context);
	}

}
