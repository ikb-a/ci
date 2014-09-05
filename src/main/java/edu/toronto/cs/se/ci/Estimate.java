package edu.toronto.cs.se.ci;

import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import edu.toronto.cs.se.ci.data.Result;

/**
 * An estimate is a representation of the CI's current estimate of a result. Like normal
 * Futures, the final answer can be retrieved from it (though it may block until the CI
 * is done computing), however, it is also possible at any given point in time to obtain
 * the CI's current best estimate by calling {@code getCurrent}.
 * 
 * @author Michael Layzell
 *
 * @param <O>
 * @param <Q>
 */
public interface Estimate<O, Q> extends ListenableFuture<Result<O, Q>> {
	
	/**
	 * Gets the current estimate. Will not block.
	 * @return Current best estimate
	 */
	public Optional<Result<O, Q>> getCurrent();
	

	/**
	 * Adds a partial listener.
	 * 
	 * @param listener the listener to run when a new partial estimate is avaliable
	 * @param executor the executor to run the listener in
	 * @throws NullPointerException if the executor or listener was null
	 * @throws RejectedExecutionException if we tried to execute the listener
	 *         immediately but the executor rejected it.
	 */
	public void addPartialListener(Runnable listener, Executor executor);

}
