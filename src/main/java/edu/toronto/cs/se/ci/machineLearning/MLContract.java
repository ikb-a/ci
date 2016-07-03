package edu.toronto.cs.se.ci.machineLearning;

import java.util.List;

import edu.toronto.cs.se.ci.Source;

/**
 * An interface representing a source contract that will be used with an ML
 * aggregator. All instances should use a non-generic sub-interface. The class
 * {@link MLToCIContract} converts an MLContract into a
 * {@link edu.toronto.cs.se.ci.Contract} of type {@code <I, O, Void>}, which can
 * be used with a CI, along with an
 * {@link edu.toronto.cs.se.ci.GenericAggregator} of type {@code <O,?,Void,?>}.
 * <p>
 * For example, a subinterface could be
 * {@code ChecksIntegersAreOdd extends MContract<Integer, Boolean>}, then any
 * existing source which accepts an Integer and returns a Boolean, and checks
 * whether the integer is odd, could be said to implement this MLContract
 * regardless of trust type. These sources would then be registered in
 * {@link edu.toronto.cs.se.ci.Contracts}. Then an {@link MLToCIContract} given
 * the {@code CheckIntegersAreOdd} class, would act as a
 * {@link edu.toronto.cs.se.ci.Contract}{@code <Integer, Boolean, Void>}
 * returning sources which check whether an integer is odd, which are
 * {@link edu.toronto.cs.se.ci.Source}{@code <Integer,
 * Boolean, Void>}.
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
	 * @return A list of sources which fulfill the contract. These sources
	 *         return whatever the contract states the must return, and are of
	 *         type {@link edu.toronto.cs.se.ci.Source}{@code <I,O,?>}.
	 */
	public List<Source<I, O, ?>> provideML();

}
