package edu.toronto.cs.se.ci.machineLearning.aggregators;

public interface MLWekaNumericConverter<O> {
	/**
	 * Converts the source output of type {@code O} into a double numeric value.
	 */
	public Double convert(O sourceOutput);
}
