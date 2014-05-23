package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class CI<F, T> {
	
	private List<Source<F, T>> sources;
	private Aggregator<T> agg;
	private Selector<F, T> sel;

	public CI(Source<F, T>[] sources, Aggregator<T> agg, Selector<F, T> sel) {
		this(Arrays.asList(sources), agg, sel);
	}

	public CI(List<Source<F, T>> sources, Aggregator<T> agg, Selector<F, T> sel) {
		this.sources = sources;
		this.agg = agg;
		this.sel = sel;
	}
	
	public Estimate<T> apply(F args, Map<String, Float> budget) {
		// Create the thread pool
		ListeningExecutorService pool = MoreExecutors.listeningDecorator(new ForkJoinPool());
		
		// Create the invocation
		Invocation invocation = new Invocation(args, budget, pool);
		
		// Return the estimate
		return invocation.getEstimate();
	}
	
	/**
	 * The Invocation object represents a single invocation of a CI.
	 * It encapsulates the state of the invocation.
	 * 
	 * @author layzellm
	 *
	 */
	public class Invocation implements Runnable {
		
		// Parameters
		private F args;
		private Map<String, Float> budget;
		private ListeningExecutorService pool;
		
		// State
		private List<Source<F, T>> consulted;
		private List<Opinion<T>> opinions;
		private EstimateImpl<T> estimate = new EstimateImpl<T>(agg, null);
		
		/**
		 * Create an Invocation of the CI. This will run the invocation, and return immediately.
		 * To wait for the CI to complete, get the estimate by calling {@code getEstimate}
		 * 
		 * @param args The arguments to pass to Source functions
		 * @param budget The budget for the CI
		 */
		private Invocation(F args, Map<String, Float> budget, ListeningExecutorService pool) {
			this.args = args;
			this.budget = budget;
			this.pool = pool;
			
			consulted = new ArrayList<Source<F, T>>(sources.size());
			opinions = new ArrayList<Opinion<T>>(sources.size());
			
			// Run the invocation, ensuring that the estimate is sealed when it finishes
			Futures.addCallback(pool.submit(this), new FutureCallback<Object>() {
				@Override
				public void onSuccess(Object result) {
					estimate.seal();
				}

				@Override
				public void onFailure(Throwable t) {
					estimate.seal();
				}
			});
		}
		
		/**
		 * @return The Selector object for the CI
		 */
		public Selector<F, T> getSelector() {
			return sel;
		}

		/**
		 * @return The Aggregator object for the CI
		 */
		public Aggregator<T> getAggregator() {
			return agg;
		}

		/**
		 * @return The Sources for the CI to query
		 */
		public List<Source<F, T>> getSources() {
			return sources;
		}

		/**
		 * @return The Sources which have already been consulted in this Invocation of the CI
		 */
		public List<Source<F, T>> getConsulted() {
			return consulted;
		}
		
		/**
		 * @return The Opinions which have been solicited in this Invocation of the CI
		 */
		public List<Opinion<T>> getOpinions() {
			return opinions;
		}

		/**
		 * @return The Budget available for running Sources
		 */
		public Map<String, Float> getBudget() {
			return budget;
		}

		/**
		 * @return The arguments to the CI function
		 */
		public F getArgs() {
			return args;
		}

		/**
		 * @return The Estimate object for the result of the CI
		 */
		public Estimate<T> getEstimate() {
			return estimate;
		}
		
		/**
		 * @return The ListeningExecutorService used for parallel execution of Source functions
		 */
		public ListeningExecutorService getPool() {
			return pool;
		}


		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			Source<F, T> next;
			for (;;) {
				// Get the next source (this might block)
				next = sel.getNextSource(this);
				if (next == null)
					break;
				
				// Record that the source has been consulted
				consulted.add(next);

				// Get the value and trust, augmenting the estimate
				ListenableFuture<T> value = pool.submit(new Source.SourceCallable<F, T>(next, args));
				ListenableFuture<Double> trust = pool.submit(new Source.SourceTrustCallable<F, T>(next, value, args));
				Opinion<T> opinion = new Opinion<T>(value, trust);
				estimate.augment(opinion);
			}
			
			// Seal the estimate
			estimate.seal();
		}
	}

}
