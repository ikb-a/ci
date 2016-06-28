package edu.toronto.cs.se.ci.machineLearning.aggregators;

/**
 * This is a single method interface, allowing from the conversion of a value of
 * type {@code O}, to a numeric double value. For example an
 * {@code MLWekaConverver<Boolean>} may convert from the Java boolean values
 * true and false, to the numeric values "1.0" and "0.0".
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 *            The type that the converter turns into a numeric value.
 */
public interface MLWekaNumericConverter<O> {
	/**
	 * Converts the source output of type {@code O} into a double numeric value.
	 */
	public Double convert(O sourceOutput);
}
