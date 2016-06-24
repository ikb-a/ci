package edu.toronto.cs.se.ci.machineLearning;

// TODO: Add date
/**
 * All Weka classifiers can only accept attributes that are nominal or numberic
 * 
 * @author ikba
 *
 * @param <O>
 */
public interface MLWekaAttribute<O> extends MLAttribute<O> {
	public boolean isNominal();

	public boolean isNumeric();

	public String getNominal();

	public Double getNumeric();

	/*
	 * In order to satisfy the ML attribute interface, it is necessary to
	 * convert from String to <O>, or from Numeric to <O> (This is because a
	 * Weka Aggregator must both accept and return a MLWekaAttribute). A
	 * possible solution is to hard code the conversion, another possible
	 * solution is to ask the user for a converter. This detail should be dealt
	 * with in the implementation. On the other hand, the conversion from <O> to nominal (or
	 * from <O> to numeric) should most likely be dealt by the user
	 */
}
