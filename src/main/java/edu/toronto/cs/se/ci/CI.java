package edu.toronto.cs.se.ci;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import edu.toronto.cs.se.ci.aggregators.VoteAggregator;
import edu.toronto.cs.se.ci.budget.Allowance;
import edu.toronto.cs.se.ci.budget.Budgets;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.budget.basic.Time;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.selectors.AllSelector;

/**
 * A Contributional Implementation (CI) of a function. Queries a set of sources, and aggregates
 * their opinions to get answers to otherwise unanswerable questions.
 * 
 * @author Michael Layzell
 *
 * @param <F> Input type
 * @param <T> Result type
 */
public class CI<F, T> {

	private final ImmutableSet<Source<F,T>> sources;
	private final Aggregator<T> agg;
	private final Selector<F, T> sel;
	private final Acceptor<T> acceptor;
	
	public CI(Class<? extends Contract<F, T>> contract) {
		this(Contracts.discover(contract));
	}

	public CI(Class<? extends Contract<F, T>> contract, Aggregator<T> agg, Selector<F, T> sel, Acceptor<T> acceptor) {
		this(Contracts.discover(contract), agg, sel, acceptor);
	}

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
		this.acceptor = null;
	}
	
	public CI(Collection<Source<F, T>> sources, Aggregator<T> agg, Selector<F, T> sel, Acceptor<T> acceptor) {
		this.sources = ImmutableSet.copyOf(sources);
		this.agg = agg;
		this.sel = sel;
		this.acceptor = acceptor;
	}
	
	/**
	 * Invokes the CI
	 * 
	 * @param args The arguments to pass to the CI
	 * @param budget The budget allocated to the CI
	 * @return An {@link Estimate} of the CI's response
	 */
	public Estimate<T> apply(F args, Allowance[] budget) {
		// Create the thread pool
		ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		
		// Create the invocation
		Invocation invocation = new Invocation(args, budget, pool);
		
		// Return the estimate
		return invocation.getEstimate();
	}
	
	public Result<T> applySync(F args, Allowance[] budget) throws InterruptedException, ExecutionException {
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
		private Allowance[] budget;
		private final ListeningExecutorService pool;
		
		// State
		private final Set<Source<F, T>> remaining;
		private final Set<ListenableFuture<Opinion<T>>> opinions;
		private final EstimateImpl<T> estimate = new EstimateImpl<T>(agg, acceptor);

		private long startedAt = -1;
		
		/**
		 * Create an Invocation of the CI. This will run the invocation, and return immediately.
		 * To wait for the CI to complete, get the estimate by calling {@code getEstimate}
		 * 
		 * @param args The arguments to pass to Source functions
		 * @param budget The budget for the CI
		 */
		private Invocation(F args, Allowance[] budget, ListeningExecutorService pool) {
			this.args = args;
			this.budget = budget;
			this.pool = pool;
			
			remaining = new HashSet<>(sources);
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
			
			// Once the estimate is complete & has returned a final answer, kill all of the threads
			Futures.addCallback(estimate, new FutureCallback<Result<T>>() {

				@Override
				public void onSuccess(Result<T> result) {
					Invocation.this.pool.shutdownNow();
				}

				@Override
				public void onFailure(Throwable t) {
					Invocation.this.pool.shutdownNow();
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
			return Budgets.withinBudget(budget, source.getCost(args), Optional.of(this));
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
		
		public Set<Source<F, T>> getRemaining() {
			return Collections.unmodifiableSet(remaining);
		}

		/**
		 * @return The Sources which have already been consulted in this Invocation of the CI
		 */
		public Set<Source<F, T>> getConsulted() {
			Set<Source<F, T>> set = new HashSet<>(sources);
			set.removeAll(remaining);
			return Collections.unmodifiableSet(set);
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
		public Allowance[] getBudget() {
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
			
			// Create the timeout timer
			Optional<Time> timeAllowance = Budgets.getByClass(budget, Time.class);
			if (timeAllowance.isPresent()) {
				long time = timeAllowance.get().getDuration(TimeUnit.NANOSECONDS);

				pool.submit(() -> {
					try {
						synchronized(this) {
							this.wait(time / 1000000, (int) (time % 1000000));
						}

						estimate.done(); // If the estimate isn't done yet - force it to be so.
					} catch (InterruptedException e) { }
				});
			}
			
			Source<F, T> next;
			for (;;) {
				// Get the next source (this might block)
				Optional<Source<F, T>> maybeNext = sel.getNextSource(this);
				if (maybeNext.isPresent())
					next = maybeNext.get();
				else
					break;
				
				// Record that the source has been consulted
				remaining.remove(next);
				
				// Exhaust budget
				Expenditure[] cost = next.getCost(args);
				Optional<Allowance[]> newBudget = Budgets.expend(budget, cost, Optional.of(this));

				if (! newBudget.isPresent()) {
					System.err.println("Selection function chose source out of budget");
					continue;
				}

				// Run the opinion if the estimate isn't already sealed. Stop running if it is.
				synchronized(estimate) {
					if (estimate.isSealed())
						return null;

					budget = newBudget.get();
					
					System.out.println("Calling " + next.getName()); // TODO: DEBUG
					
					// Query the source & augment the estimate
					ListenableFuture<Opinion<T>> opinion = pool.submit(new Source.SourceCallable<F, T>(next, args));
					opinions.add(opinion);
					estimate.augment(opinion);
				}
			}
			
			// Seal the estimate
			estimate.seal();

			return null;
		}
	}

}
