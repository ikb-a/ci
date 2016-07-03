package edu.toronto.cs.se.ci.machineLearning.aggregators;

import weka.classifiers.Classifier;
import weka.core.Instances;

public interface MLWekaAggregator<O, FO, Q> extends MLAggregator<O, FO, Q> {
	/**
	 * Return a copy of the {@link Classifier} being used in the aggregator.
	 * 
	 * @throws Exception
	 *             if the copying of the classifier throws an exception
	 */
	public Classifier getClassifier() throws Exception;

	/**
	 * Given test data at {@code filePath} which contains classified instances
	 * compatible with the trained classifier, the classifier is tested on the
	 * test data, and the results returned.
	 * <p>
	 * From the Weka documentation: "Evaluates the classifier on a given set of
	 * instances. Note that the data must have exactly the same format (e.g.
	 * order of attributes) as the data used to train the classifier! Otherwise
	 * the results will generally be meaningless."
	 * 
	 * @param filePath
	 *            The path to the test file.
	 */
	public weka.classifiers.Evaluation testClassifier(String filePath) throws Exception;

	/**
	 * Given test {@code Instances} which should be classified instances
	 * compatible with the trained classifier, the classifier is tested on the
	 * test data, and the results returned.
	 * <p>
	 * From the Weka documentation: "Evaluates the classifier on a given set of
	 * instances. Note that the data must have exactly the same format (e.g.
	 * order of attributes) as the data used to train the classifier! Otherwise
	 * the results will generally be meaningless."
	 * 
	 * @param filePath
	 *            The path to the test file.
	 */
	public weka.classifiers.Evaluation testClassifier(Instances instances) throws Exception;
}
