package edu.toronto.cs.se.ci.machineLearning;

public interface MLAttribute<O> {
	/**
	 * This method returns the unique name of this attribute, as was used in the
	 * training data for the classifier.
	 */
	public String getAttributeName();

	/**
	 * This method returns the value of this attribute.
	 */
	public O getAttributeValue();
}
