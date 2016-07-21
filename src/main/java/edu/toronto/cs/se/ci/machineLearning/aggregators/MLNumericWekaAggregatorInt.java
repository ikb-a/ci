package edu.toronto.cs.se.ci.machineLearning.aggregators;

/**
 * This Interface represents a Weka aggregator, which aggregates opinions which
 * are numeric values, and returns a representation of type FO of a numeric
 * value. The quality type is implicitly {@code double[]}, the class
 * distribution produced by the aggregator.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 *            The output of the sources (This information should be numeric)
 * @param <FO>
 *            The value returned by the aggregator (a representation of the
 *            numeric value returned by the Weka aggregator).
 * @param <Q>
 *            The type of the quality returned by the numeric aggregator.
 */
public interface MLNumericWekaAggregatorInt<O, FO, Q> extends MLWekaAggregator<O, FO, Q> {

	// Problem: <O> can only be: "Nominal Attribute" (enum) (in this case, bool
	// would be considered an enum);
	// String attribute (doesn't seem very useful)
	// Date attribute (yyyy-MM-dd'T'HH:mm:ss, i.e. a specially formatted string)
	// Numeric Attribute (whole number or decimal)
	// Relational Attribute (?)

}
