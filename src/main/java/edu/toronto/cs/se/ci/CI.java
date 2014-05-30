package edu.toronto.cs.se.ci;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import edu.toronto.cs.se.ci.aggregators.VoteAggregator;
import edu.toronto.cs.se.ci.selectors.AllSelector;

public class CI<F, T> {
	
	private final ImmutableSet<Source<F,T>> sources;
	private final Aggregator<T> agg;
	private final Selector<F, T> sel;

	/**
	 * Create a CI using the {@link VoteAggregator} aggregator and
	 * {@link AllSelector} selector.
	 * 
	 * @param sources The list of sources for the CI to query
	 */
	public CI(Collection<Source<F, T>> sources) {
		this(sources, new VoteAggregator<T>(), new AllSelector<F, T>());
	}

	/**
	 * Create a CI using the provided aggregator and selector.
	 * 
	 * @param sources The list of sources for the CI to query
	 * @param agg The opinion aggregator
	 * @param sel The source selector
	 */
	public CI(Collection<Source<F, T>> sources, Aggregator<T> agg, Selector<F, T> sel) {
		this.sources = ImmutableSet.copyOf(sources);
		this.agg = agg;
		this.sel = sel;
	}
	
	public CI(Collection<Source<F, T>> sources, Aggregator<T> agg, Selector<F, T> sel, Acceptor<F, T> acc) {
		this.sources = ImmutableSet.copyOf(sources);
		this.agg = agg;
		this.sel = sel;
	}
	
	/**
	 * Invokes the CI
	 * 
	 * @param args The arguments to pass to the CI
	 * @param budget The budget allocated to the CI
	 * @return An {@link Estimate} of the CI's response
	 */
	public Estimate<T> apply(F args, Budget budget) {
		// Create the thread pool
		ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		
		// Create the invocation
		Invocation invocation = new Invocation(args, budget, pool);
		
		// Return the estimate
		return invocation.getEstimate();
	}
	
	public Result<T> applySync(F args, Budget budget) throws InterruptedException, ExecutionException {
		// sameThreadExecutor will cause this to run in sync
		ListeningExecutorService pool = MoreExecutors.sameThreadExecutor();
		
		// Create the invocation
		Invocation invocation = new Invocation(args, budget, pool);
		
		// Return the result
		return invocation.getEstimate().get();
	}
	
	/**
	 * The Invocation object represents a single invocation of a CI.
	 * It encapsulates the state of the invocation.
	 * 
	 * @author layzellm
	 *
	 */
	public class Invocation implements Callable<Void> {
		
		// Parameters
		private final F args;
		private Budget budget;
		private final ListeningExecutorService pool;
		
		// State
		private final Set<Source<F, T>> consulted;
		private final Set<ListenableFuture<Opinion<T>>> opinions;
		private final EstimateImpl<T> estimate = new EstimateImpl<T>(agg, null); // TODO: Add satisfaction function

		private long startedAt = -1;
		
		/**
		 * Create an Invocation of the CI. This will run the invocation, and return immediately.
		 * To wait for the CI to complete, get the estimate by calling {@code getEstimate}
		 * 
		 * @param args The arguments to pass to Source functions
		 * @param budget The budget for the CI
		 */
		private Invocation(F args, Budget budget, ListeningExecutorService pool) {
			this.args = args;
			this.budget = budget;
			this.pool = pool;
			
			consulted = new HashSet<Source<F, T>>();
			opinions = new HashSet<ListenableFuture<Opinion<T>>>();
			
			// Run the invocation, ensuring that the estimate is sealed when it finishes
			Futures.addCallback(pool.submit(this), new FutureCallback<Object>() {
				@Override
				public void onSuccess(Object result) {
					estimate.seal();
					Invocation.this.pool.shutdown();
				}

				@Override
				public void onFailure(Throwable t) {
					estimate.seal();
					Invocation.this.pool.shutdown();
				}
			});
			
		}
		
		/**
		 * Checks whether the given source fits within the CI's remaining budget
		 * 
		 * @param source The given source
		 * @return Whether the source fits within the CI's remaining budget
		 * @throws Exception If the Source's getCost function throws an exception
		 */
		public boolean withinBudget(Source<F, T> source) throws Exception {
			return source.getCost(args).withinBudget(budget, getElapsedTime(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
		}
		
		/**
		 * @param unit The unit to return time in
		 * @return Time elapsed since the CI was invoked
		 */
		public long getElapsedTime(TimeUnit unit) {
			if (startedAt == -1)
				throw new Error("Invocation hasn't started yet");
			
			return unit.convert(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
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
		public ImmutableSet<Source<F, T>> getSources() {
			return sources;
		}

		/**
		 * @return The Sources which have already been consulted in this Invocation of the CI
		 */
		public Set<Source<F, T>> getConsulted() {
			return Collections.unmodifiableSet(consulted);
		}
		
		/**
		 * @return The Opinions which have been solicited in this Invocation of the CI
		 */
		public Set<ListenableFuture<Opinion<T>>> getOpinions() {
			return Collections.unmodifiableSet(opinions);
		}

		/**
		 * Gets the budget for running the remaining sources. As sources are run,
		 * the budget is depleted, however the Budget object is immutable, so any
		 * references to a given budget object will not change.
		 * 
		 * @return The Budget still available for running Sources
		 */
		public Budget getBudget() {
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
		public Void call() throws Exception {
			if (startedAt > 0)
				throw new Error("An invocation can only be called once");

			startedAt = System.nanoTime();
			
			Source<F, T> next;
			for (;;) {
				// Get the next source (this might block)
				next = sel.getNextSource(this);
				if (next == null)
					break;
				
				// Record that the source has been consulted
				consulted.add(next);
				
				// Exhaust budget
				Cost cost = next.getCost(args);
				if (! cost.withinBudget(budget, System.nanoTime() - startedAt, TimeUnit.NANOSECONDS)) {
					System.err.println("Selection function chose source out of budget");
					continue;
				}
				budget = cost.spend(budget);
				
				System.out.println("Calling " + next.getClass().getName()); // TODO: DEBUG
				
				// Query the source & augment the estimate
				ListenableFuture<Opinion<T>> opinion = pool.submit(new Source.SourceCallable<F, T>(next, args));
				opinions.add(opinion);
				estimate.augment(opinion);
			}
			
			// Seal the estimate
			estimate.seal();

			return null;
		}
	}

}
