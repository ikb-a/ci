package edu.toronto.cs.se.ci.machineLearning.aggregators;

public interface MLWekaNominalConverter<O> {
	/**
	 * Converts the source output of type {@code O} into a String nominal value.
	 */
	public String convert(O sourceOutput);
}
