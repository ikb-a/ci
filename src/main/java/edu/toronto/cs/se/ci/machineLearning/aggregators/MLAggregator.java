package edu.toronto.cs.se.ci.machineLearning.aggregators;

import edu.toronto.cs.se.ci.GenericAggregator;

public interface MLAggregator<O,FO, Q> extends GenericAggregator<O,FO, Void, Q>{
	//TODO: determine if a retrain method is good. On the one hand, 
	//all ML aggregators should be able to retrain. On the other,
	//none of the ML aggregators will necessarily share a format
	//for training data.
}
