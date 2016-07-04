package edu.toronto.cs.se.ci.machineLearning.aggregators;

/**
 * This Interface represents a Weka aggregator, which aggregates opinions which
 * are Nominal values, and returns a representation of type FO of a nominal
 * value. The quality type is implicitly {@code double[]}, the class
 * distribution produced by the aggregator.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 *            The output of the sources (This information should be nominal)
 * @param <FO>
 *            The value returned by the aggregator (a representation of the
 *            nominal value returned by the Weka aggregator).
 */
public interface MLNominalWekaAggregatorInt<O, FO> extends MLWekaAggregator<O, FO, double[]> {

	// Problem: <O> can only be: "Nominal Attribute" (enum) (in this case, bool
	// would be considered an enum);
	// String attribute (doesn't seem very useful)
	// Date attribute (yyyy-MM-dd'T'HH:mm:ss, i.e. a specially formatted string)
	// Numeric Attribute (whole number or decimal)
	// Relational Attribute (?)

}
