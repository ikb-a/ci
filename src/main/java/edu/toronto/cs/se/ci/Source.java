package edu.toronto.cs.se.ci;

import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;

/**
 * A source queried by a CI for its opinion.
 * 
 * @author Michael Layzell
 *
 * @param <I>
 * @param <O>
 * @param <T>
 */
public abstract class Source<I, O, T> {
	
	/**
	 * Provides this source. Means that contracts don't have to be implemented
	 * manually for every source.
	 * 
	 * @return A single element list containing only this source
	 */
	public List<Source<I, O, T>> provide() {
		return ImmutableList.of(this);
	}
	
	/**
	 * @return A unique name for the Source - used for debugging purposes
	 */
	public String getName() {
		return this.getClass().getName();
	}
	
	/**
	 * Get the cost of querying the source
	 * 
	 * @param args The arguments which would be passed to {@code apply}
	 * @return The cost of querying the source
	 */
	public abstract Expenditure[] getCost(I args) throws Exception;

	/**
	 * Get the source's opinion. This includes the result of the source, and
	 * the trust/confidence of the source.
	 * 
	 * @param args The arguments passed to the CI
	 * @return The source's opinion.
	 * @throws UnknownException The source wasn't avaliable, so no answer could be obtained
	 */
	public abstract Opinion<O, T> getOpinion(I args) throws UnknownException;

	/**
	 * Callable wrapper for a source.
	 *
	 * @param <I> Argument Type
	 * @param <O> Return Type
	 */
	public static class SourceCallable<I, O, T> implements Callable<Opinion<O, T>> {
		
		private Source<I, O, T> source;
		private I args;
		
		public SourceCallable(Source<I, O, T> source, I args) {
			this.source = source;
			this.args = args;
		}

		@Override
		public Opinion<O, T> call() throws Exception {
			return source.getOpinion(args);
		}

	}

	/**
	 * Gets the trust in the source. This can vary based on the response which
	 * the source has provided.
	 * 
	 * @param value The response which the source provided (the value returned by getResponse)
	 * @param args The arguments passed to the source
	 * @return A double representing the trust in the response
	 */
	public abstract T getTrust(I args, Optional<O> value);
	
}
