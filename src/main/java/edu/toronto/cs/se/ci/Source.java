package edu.toronto.cs.se.ci;

import java.util.concurrent.Callable;

public interface Source<F, T> {
	
	/**
	 * Get the cost of querying the source
	 * 
	 * @param args The arguments which would be passed to {@code apply}
	 * @return The cost of querying the source
	 */
	public Cost getCost(F args) throws Exception;

	/**
	 * Get the source's opinion. This includes the result of the source, and
	 * the trust/confidence of the source.
	 * 
	 * @param args The arguments passed to the CI
	 * @return The source's opinion.
	 * @throws UnknownException The source wasn't avaliable, so no answer could be obtained
	 */
	public Opinion<T> getOpinion(F args) throws UnknownException;

	/**
	 * Callable wrapper for a source.
	 *
	 * @param <F> Argument Type
	 * @param <T> Return Type
	 */
	public class SourceCallable<F, T> implements Callable<Opinion<T>> {
		
		private Source<F, T> source;
		private F args;
		
		public SourceCallable(Source<F, T> source, F args) {
			this.source = source;
			this.args = args;
		}

		@Override
		public Opinion<T> call() throws Exception {
			return source.getOpinion(args);
		}

	}
	
}
