package edu.toronto.cs.se.ci.machineLearning.aggregators;

import weka.classifiers.Classifier;

public interface MLWekaAggregator<O, FO, Q> extends MLAggregator<O, FO, Q> {
	/**
	 * Return a copy of the {@link Classifier} being used in the aggregator.
	 * 
	 * @throws Exception
	 *             if the copying of the classifier throws an exception
	 */
	public Classifier getClassifier() throws Exception;
}
