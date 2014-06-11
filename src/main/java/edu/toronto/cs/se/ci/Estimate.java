package edu.toronto.cs.se.ci;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An estimate is a representation of the CI's current estimate of a result. Like normal
 * Futures, the final answer can be retrieved from it (though it may block until the CI
 * is done computing), however, it is also possible at any given point in time to obtain
 * the CI's current best estimate by calling {@code getCurrent}.
 * 
 * @author Michael Layzell
 *
 * @param <T>
 */
public interface Estimate<T> extends ListenableFuture<Result<T>> {
	
	/**
	 * Gets the current estimate. Will not block.
	 * @return Current best estimate
	 */
	public Result<T> getCurrent();

}
