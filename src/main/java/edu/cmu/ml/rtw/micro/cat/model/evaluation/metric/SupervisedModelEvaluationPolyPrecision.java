package edu.cmu.ml.rtw.micro.cat.model.evaluation.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationPrecision;

public class SupervisedModelEvaluationPolyPrecision<L> extends SupervisedModelEvaluationPrecision<TokenSpansDatum<L>, L> {
	public SupervisedModelEvaluationPolyPrecision() {
		super();
	}
	
	public SupervisedModelEvaluationPolyPrecision(Context<TokenSpansDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected double compute(SupervisedModel<TokenSpansDatum<L>, L> model, FeaturizedDataSet<TokenSpansDatum<L>, L> data, Map<TokenSpansDatum<L>, L> predictions) {
		Map<TokenSpansDatum<L>, L> polysemousPredictions = new HashMap<TokenSpansDatum<L>, L>();
		
		for (Entry<TokenSpansDatum<L>, L> prediction : predictions.entrySet())
			if (prediction.getKey().isPolysemous())
				polysemousPredictions.put(prediction.getKey(), prediction.getValue());
			
		return super.compute(model, data, polysemousPredictions);
	}
	
	@Override
	public String getGenericName() {
		return "PolyPrecision";
	}


	@Override
	public SupervisedModelEvaluation<TokenSpansDatum<L>, L> makeInstance(Context<TokenSpansDatum<L>, L> context) {
		return new SupervisedModelEvaluationPolyPrecision<L>(context);
	}
}
