package edu.toronto.cs.se.ci.machineLearning;

import java.util.List;

import edu.toronto.cs.se.ci.Source;

/**
 * An interface representing a source contract that will be used with an ML
 * aggregator. All instances should use a non-generic sub-interface. The class
 * {@link MLToCIContract} converts an MLContract into a
 * {@link edu.toronto.cs.se.ci.Contract} of type <I, O, Void>, which can be used
 * with a CI.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <I>
 *            The input type of all sources that implement this contract
 * @param <O>
 *            The output type of all sources implementing this contract
 */
public interface MLContract<I, O> {

	/**
	 * Generate a list of sources which fulfill the contract
	 * 
	 * @return A list of sources which fulfill the contract
	 */
	public List<Source<I, O, ?>> provide();

}
