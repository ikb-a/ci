package edu.toronto.cs.se.ci.machineLearning.aggregators;

/**
 * This is a single method interface, allowing from the conversion of a value of
 * type {@code O}, to a nominal String value. For example an
 * {@code MLWekaConverver<Boolean>} may convert from the Java boolean values
 * true and false, to the nominal String values "True" and "False".
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 *            The type that the converter turns into a nominal value.
 */
public interface MLWekaNominalConverter<O> {
	/**
	 * Converts the source output of type {@code O} into a String nominal value.
	 */
	public String convert(O sourceOutput);
}
