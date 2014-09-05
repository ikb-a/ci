package edu.toronto.cs.se.ci;

import com.google.common.base.Optional;

/**
 * The Selector is a single-method interface. It is used to determine the next 
 * source to consult for its opinion.
 * 
 * @author Michael Layzell
 *
 * @param <I>
 * @param <O>
 * @param <T>
 */
public interface Selector<I, O, T> {
	
	/**
	 * Get the next source to be consulted by the CI. This function will be called
	 * repeatedly until it returns {@code null}, at which point, the CI will stop
	 * calling functions. It may block to wait for sources to be consulted.
	 * 
	 * @param invocation The current invocation of the CI
	 * @return The next source to consult, or {@code null}
	 */
	public Optional<Source<I, O, T>> getNextSource(CI<I, O, T, ?>.Invocation invocation);

}
