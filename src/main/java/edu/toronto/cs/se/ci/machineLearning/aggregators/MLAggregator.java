package edu.toronto.cs.se.ci.machineLearning.aggregators;

import edu.toronto.cs.se.ci.GenericAggregator;

/**
 * This class represents a ML Aggregator. It's trust type is implicitly
 * {@code Void} as all {@link edu.toronto.cs.se.ci.machineLearning.MLSource}
 * have an implicit trust of type {@code <void>}.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 *            The type of the value of the Opinions accepted by the ML
 *            Aggregator.
 * @param <FO>
 *            The type of the value of the aggregated
 *            {@link edu.toronto.cs.se.ci.data.Result}.
 * @param <Q>
 *            The type of the quality of the aggregated
 *            {@link edu.toronto.cs.se.ci.data.Result}.
 */
public interface MLAggregator<O, FO, Q> extends GenericAggregator<O, FO, Void, Q> {

}
