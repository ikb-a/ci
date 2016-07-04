package edu.toronto.cs.se.ci.machineLearning.aggregators;

import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.filters.Filter;

//TODO: See what can be refactored/moved into MLAggregator
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

	/**
	 * Adds a single filter to the classifier and retrains it. If the classifier
	 * already has a filter, this filter will be applied after the existing
	 * filter(s).
	 * 
	 * @param filter
	 *            The Weka filter must have the desired options set BEFORE it is
	 *            passed to this method.
	 * @throws Exception
	 *             Thrown if Weka throws an exception training the
	 *             FilteredClassifier containing the filters given.
	 */
	// TODO: Consider returning a boolean rather than throwing an exception?
	// (Then the cause of the failure is lost)
	public void addFilter(Filter filter) throws Exception;

	/**
	 * Adds multiple filters to the classifier and retrains it. If the
	 * classifier already has a filter(s), these filters will be applied in the
	 * same order as the list after the existing filter(s).
	 * 
	 * @param filter
	 *            The Weka filter must have the desired options set BEFORE it is
	 *            passed to this method.
	 * @throws Exception
	 *             Thrown if Weka throws an exception training the
	 *             FilteredClassifier containing the filters given.
	 */
	public void addFilters(List<Filter> filters) throws Exception;

	/**
	 * Performs {@code n}-fold cross validation on the classifier using the
	 * training data.
	 * 
	 * Note that the Classifier being used in the Aggregator must abide to the
	 * Weka convention that a classifier must be re-initialized every time the
	 * buildClassifier method is called
	 * (https://weka.wikispaces.com/Use+Weka+in+your+Java+code#Classification-
	 * Evaluating).
	 * 
	 * @param n
	 *            The number of folds for the validation (usually n = 10)
	 * @return An Evaluation object created by cross validating the classifier
	 *         being used.
	 * @throws Exception
	 */
	public Evaluation nFoldCrossValidate(int n) throws Exception;

	/**
	 * Retrains the classifier on the data in the file at
	 * {@code filePathToTrainingData}. Note that this method works only if the
	 * Classifier provided at construction obeys the Weka convention of
	 * re-initializing when the buildClassifier method is called. Any applied
	 * filters will apply during retraining.
	 * 
	 * @param filePathToTrainingData
	 */
	public void retrain(String filePathToTrainingData) throws Exception;

	/**
	 * Retrains the classifier on {@code trainingData}. Note that this method
	 * works only if the Classifier provided at construction obeys the Weka
	 * convention of re-initializing when the buildClassifier method is called.
	 * Any applied filters will apply during retraining.
	 * 
	 * @param filePathToTrainingData
	 */
	public void retrain(Instances trainingData) throws Exception;
}
