package edu.toronto.cs.se.ci.machineLearning.aggregators;

import edu.toronto.cs.se.ci.data.Opinion;

/**
 * This is a single method interface, allowing from the conversion of a value of
 * type {@code O}, to it's value as an attribute ({@code <AT>}).
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 *            The type that the converter turns into a numeric value.
 * @param <AT>
 *            The type of the attribute.
 */
public interface MLWekaGenericConverter<O, AT> {
	/**
	 * Converts the source output of type {@code O} into a {@code AT} value for
	 * use in Weka.
	 */
	public AT convert(Opinion<O, Void> sourceOutput);
}
