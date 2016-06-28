package edu.toronto.cs.se.ci;

/**
 * The Aggregator functions as a {@link GenericAggregator}, except it aggregates
 * Opinions with a value of type O, AND returns Results of a value of type O.
 * <p>
 * In other words, an Aggregator<O,T,Q> is equivalent to a GenericAggregator
 * <O,O,T,Q>.
 * 
 * @author Michael Layzell
 * @author Ian Berlot-Attwel
 *
 * @param <O>
 * @param <T>
 * @param <Q>
 */
public interface Aggregator<O, T, Q> extends GenericAggregator<O, O, T, Q> {

}
