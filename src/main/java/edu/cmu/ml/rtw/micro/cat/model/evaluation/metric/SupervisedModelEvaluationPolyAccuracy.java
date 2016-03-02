package edu.cmu.ml.rtw.micro.cat.model.evaluation.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationAccuracy;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum;

/**
 * SupervisedModelEvaluationPolyAccuracy computes the same measure
 * as edu.cmu.ml.rtw.micro.generic.model.evaluation.model.SupervisedModelEvaluationAccuracy 
 * but restricted to polysemous noun-phrase datums.
 * 
 * @author Bill McDowell
 *
 * @param <L>
 */
public class SupervisedModelEvaluationPolyAccuracy<L> extends SupervisedModelEvaluationAccuracy<TokenSpansDatum<L>, L> {
	public SupervisedModelEvaluationPolyAccuracy() {
		super();
	}
	
	public SupervisedModelEvaluationPolyAccuracy(DatumContext<TokenSpansDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected double compute(SupervisedModel<TokenSpansDatum<L>, L> model, DataFeatureMatrix<TokenSpansDatum<L>, L> data, Map<TokenSpansDatum<L>, L> predictions) {
		Map<TokenSpansDatum<L>, L> polysemousPredictions = new HashMap<TokenSpansDatum<L>, L>();
		
		for (Entry<TokenSpansDatum<L>, L> prediction : predictions.entrySet())
			if (prediction.getKey().isPolysemous())
				polysemousPredictions.put(prediction.getKey(), prediction.getValue());
			
		return super.compute(model, data, polysemousPredictions);
	}

	@Override
	public String getGenericName() {
		return "PolyAccuracy";
	}

	@Override
	public SupervisedModelEvaluation<TokenSpansDatum<L>, L> makeInstance(DatumContext<TokenSpansDatum<L>, L> context) {
		return new SupervisedModelEvaluationPolyAccuracy<L>(context);
	}
}
