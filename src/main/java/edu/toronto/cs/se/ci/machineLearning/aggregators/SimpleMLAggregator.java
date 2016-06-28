package edu.toronto.cs.se.ci.machineLearning.aggregators;

/**
 * This interface represents an {@link MLAggregator} which returns the same type
 * as it aggregates. For that reason a {@code SimpleMLAggregator<FO, Q>} is
 * equivalent to {@code MLAggregator<FO, FO, Q>}.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <FO>
 *            The type that the aggregator both aggregates, and returns.
 * @param <Q>
 *            The Quality returned by the aggregator.
 */
public interface SimpleMLAggregator<FO, Q> extends MLAggregator<FO, FO, Q> {

}
