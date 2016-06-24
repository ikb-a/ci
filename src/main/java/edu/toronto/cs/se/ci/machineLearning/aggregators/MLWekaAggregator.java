package edu.toronto.cs.se.ci.machineLearning.aggregators;

public interface MLWekaAggregator<O,FO> extends MLAggregator<O,FO, double[]> {
	
	// Problem: <O> can only be: "Nominal Attribute" (enum) (in this case, bool would be considered an enum);
	//String attribute (doesn't seem very useful)
	//Date attribute (yyyy-MM-dd'T'HH:mm:ss, i.e. a specially formatted string)
	//Numeric Attribute (whole number or decimal)
	//Relational Attribute (?)

}
