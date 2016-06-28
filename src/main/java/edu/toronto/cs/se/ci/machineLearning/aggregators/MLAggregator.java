package edu.toronto.cs.se.ci.machineLearning.aggregators;

import edu.toronto.cs.se.ci.GenericAggregator;

/**
 * This class represents a ML Aggregator. It's trust type is implicitly
 * {@code Void} as all {@link edu.toronto.cs.se.ci.machineLearning.MLSource}
 * have an implicit trust of type {@code <void>}.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O> The type of the value of the Opinions accepted by the ML Aggregator.
 * @param <FO> The type of the value of the aggregated {@link edu.toronto.cs.se.ci.data.Result}.
 * @param <Q> The type of the quality of the aggregated {@link edu.toronto.cs.se.ci.data.Result}.
 */
public interface MLAggregator<O, FO, Q> extends GenericAggregator<O, FO, Void, Q> {
	// TODO: determine if a retrain method is good. On the one hand,
	// all ML aggregators should be able to retrain. On the other,
	// none of the ML aggregators will necessarily share a format
	// for training data.

	// TODO: determine what methods for n-fold cross validation should be
	// included, so that a measure of the quality of the aggregator can
	// be programmatically created
}
