package edu.toronto.cs.se.ci;

import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;

public interface Source<F, T> extends Function<F, T> {
	
	/**
	 * Get the cost of querying the source
	 * 
	 * @param args The arguments which would be passed to {@code apply}
	 * @return The cost of querying the source
	 */
	public Map<String, Double> getCost(F args);

	/**
	 * Get the trustworthiness of the source's response
	 * 
	 * @param response A future which will resolve to the result of calling the source
	 * @param args The arguments passed to {@code apply}
	 * @return The trustworthiness of the source's response
	 */
	public Double getTrust(ListenableFuture<T> response, F args);

	/**
	 * Callable wrapper for a source.
	 *
	 * @param <F> Argument Type
	 * @param <T> Return Type
	 */
	public class SourceCallable<F, T> implements Callable<T> {
		
		private Source<F, T> source;
		private F args;
		
		public SourceCallable(Source<F, T> source, F args) {
			this.source = source;
			this.args = args;
		}

		@Override
		public T call() throws Exception {
			return source.apply(args);
		}

	}
	
	/**
	 * Callable wrapper for a source's getTrust function
	 *
	 * @param <F> Argument Type
	 * @param <T> Return Type
	 */
	public class SourceTrustCallable<F, T> implements Callable<Double> {
		
		private Source<F, T> source;
		private ListenableFuture<T> result;
		private F args;
		
		public SourceTrustCallable(Source<F, T> source, ListenableFuture<T> result, F args) {
			this.source = source;
			this.result = result;
			this.args = args;
		}

		@Override
		public Double call() throws Exception {
			return source.getTrust(result, args);
		}

	}
}
