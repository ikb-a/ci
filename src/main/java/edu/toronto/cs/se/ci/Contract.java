package edu.toronto.cs.se.ci;

import java.util.List;

/**
 * An interface representing a source contract. All instances should use a
 * non-generic sub-interface
 * 
 * @author Michael Layzell
 *
 * @param <I>
 * @param <O>
 * @param <T>
 */
public interface Contract<I, O, T> extends Provider<I, O, T> {

	/**
	 * Generate a list of sources which fulfill the contract
	 * 
	 * @return A list of sources which fulfill the contract
	 */
	public List<Source<I, O, T>> provide();

}
