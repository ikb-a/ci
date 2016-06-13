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

import edu.toronto.cs.se.ci.budget.Allowance;
import edu.toronto.cs.se.ci.budget.Budgets;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.budget.basic.Time;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;

/**
 * A Contributional Implementation (CI) of a function. Queries a set of sources,
 * and aggregates their opinions to get answers to otherwise unanswerable
 * questions.
 * 
 * @author Michael Layzell
 *
 * @param <I>
 *            Input type
 * @param <O>
 *            Result type
 * @param <T>
 *            Trust type
 * @param <Q>
 *            Quality type
 */
public class CI<I, O, T, Q> {

	private final ImmutableSet<Source<I, O, T>> sources;
	private final Aggregator<O, T, Q> agg;
	private final Selector<I, O, T> sel;
	private final Acceptor<O, Q> acceptor;

	/**
	 * Create a CI using source discovery
	 * 
	 * @param contract
	 *            The {@link Contract} to discover sources with
	 * @param agg
	 *            The {@link Aggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 */
	public CI(Class<? extends Contract<I, O, T>> contract, Aggregator<O, T, Q> agg, Selector<I, O, T> sel) {
		this(Contracts.discover(contract), agg, sel);
	}

	/**
	 * Create a CI using source discovery
	 * 
	 * @param contract
	 *            The {@link Contract} to discover sources with
	 * @param agg
	 *            The {@link Aggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 * @param acceptor
	 *            The {@link Acceptor} to use
	 */
	public CI(Class<? extends Contract<I, O, T>> contract, Aggregator<O, T, Q> agg, Selector<I, O, T> sel,
			Acceptor<O, Q> acceptor) {
		this(Contracts.discover(contract), agg, sel, acceptor);
	}

	/**
	 * Create a CI using an explicit source set
	 * 
	 * @param sources
	 *            The {@link Source}s to select from
	 * @param agg
	 *            The {@link Aggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 */
	public CI(Collection<Source<I, O, T>> sources, Aggregator<O, T, Q> agg, Selector<I, O, T> sel) {
		this.sources = ImmutableSet.copyOf(sources);
		this.agg = agg;
		this.sel = sel;
		this.acceptor = null;
	}

	/**
	 * Create a CI using an explicit source set
	 * 
	 * @param sources
	 *            The {@link Source}s to select from
	 * @param agg
	 *            The {@link Aggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 * @param acceptor
	 *            The {@link Acceptor}
	 */
	public CI(Collection<Source<I, O, T>> sources, Aggregator<O, T, Q> agg, Selector<I, O, T> sel,
			Acceptor<O, Q> acceptor) {
		this.sources = ImmutableSet.copyOf(sources);
		this.agg = agg;
		this.sel = sel;
		this.acceptor = acceptor;
	}

	/**
	 * Invokes the CI
	 * 
	 * @param args
	 *            The arguments to pass to the CI
	 * @param budget
	 *            The budget allocated to the CI
	 * @return An {@link Estimate} of the CI's response
	 */
	public Estimate<O, Q> apply(I args, Allowance[] budget) {
		// Create the thread pool
		ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

		// Create the invocation
		Invocation invocation = new Invocation(args, budget, pool);

		// Return the estimate
		return invocation.getEstimate();
	}

	/**
	 * Invokes the CI, running on a single thread. Sources are queried
	 * sequentially
	 * 
	 * @param args
	 *            The arguments to pass to the CI
	 * @param budget
	 *            The budget allocated to the CI
	 * @return The result of the CI's execution
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public Result<O, Q> applySync(I args, Allowance[] budget) throws InterruptedException, ExecutionException {
		// sameThreadExecutor will cause this to run in sync
		ListeningExecutorService pool = MoreExecutors.sameThreadExecutor();

		// Create the invocation
		Invocation invocation = new Invocation(args, budget, pool);

		// Return the result
		return invocation.getEstimate().get();
	}

	/**
	 * The Invocation object represents a single invocation of a CI. It
	 * encapsulates the state of the invocation.
	 * 
	 * @author layzellm
	 *
	 */
	public class Invocation implements Callable<Void> {

		// Parameters
		private final I args;
		private Allowance[] budget;
		private final ListeningExecutorService pool;

		// State
		private final Set<Source<I, O, T>> remaining;
		private final Set<ListenableFuture<Opinion<O, T>>> opinions;
		/*
		 * EstimateImpl is itself a ListenableFuture, estimate can have
		 * listeners for when it is complete, or for when a new estimate is
		 * available from this Invocation.
		 */
		private final EstimateImpl<O, T, Q> estimate = new EstimateImpl<O, T, Q>(agg, acceptor);

		private long startedAt = -1;

		/**
		 * Create an Invocation of the CI. This will run the invocation, and
		 * return immediately. To wait for the CI to complete, get the estimate
		 * by calling {@code getEstimate}
		 * 
		 * @param args
		 *            The arguments to pass to Source functions
		 * @param budget
		 *            The budget for the CI
		 */
		private Invocation(I args, Allowance[] budget, ListeningExecutorService pool) {
			this.args = args;
			this.budget = budget;
			this.pool = pool;

			// deep copy
			remaining = new HashSet<>(sources);
			opinions = new HashSet<ListenableFuture<Opinion<O, T>>>();

			// Run the invocation, ensuring that the estimate is sealed when it
			// finishes
			/*
			 * This invocation (which is a Callable object that returns Void) is
			 * run on a new thread. When the execution of this invocation on the
			 * thread is complete, the FutureCallback will be executed on this
			 * thread (the default for addCallback is for FutureCallback objects
			 * to be executed using the directExecutor). Regardless of whether
			 * the invocation was successful, the estimate is marked as sealed
			 * (which will eventually cause the estimate isDone() method to
			 * become true, triggering any FutureCallbacks on estimate).
			 */
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

			// Once the estimate is complete & has returned a final answer, kill
			// all of the threads
			Futures.addCallback(estimate, new FutureCallback<Result<O, Q>>() {

				@Override
				public void onSuccess(Result<O, Q> result) {
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
		 * @param source
		 *            The given source
		 * @return Whether the source fits within the CI's remaining budget
		 * @throws Exception
		 *             If the Source's getCost function throws an exception
		 */
		public boolean withinBudget(Source<I, O, T> source) throws Exception {
			return Budgets.withinBudget(budget, source.getCost(args), Optional.of(this));
		}

		/**
		 * @param unit
		 *            The unit to return time in
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
		public Selector<I, O, T> getSelector() {
			return sel;
		}

		/**
		 * @return The Aggregator object for the CI
		 */
		public Aggregator<O, T, Q> getAggregator() {
			return agg;
		}

		/**
		 * @return The Sources for the CI to query
		 */
		public ImmutableSet<Source<I, O, T>> getSources() {
			return sources;
		}

		/**
		 * @return The Sources which haven't been queried yet
		 */
		public Set<Source<I, O, T>> getRemaining() {
			return Collections.unmodifiableSet(remaining);
		}

		/**
		 * @return The Sources which have already been consulted in this
		 *         Invocation of the CI
		 */
		public Set<Source<I, O, T>> getConsulted() {
			Set<Source<I, O, T>> set = new HashSet<>(sources);
			set.removeAll(remaining);
			return Collections.unmodifiableSet(set);
		}

		/**
		 * @return The Opinions which have been solicited in this Invocation of
		 *         the CI
		 */
		public Set<ListenableFuture<Opinion<O, T>>> getOpinions() {
			return Collections.unmodifiableSet(opinions);
		}

		/**
		 * Gets the budget for running the remaining sources. As sources are
		 * run, the budget is depleted, however the Budget object is immutable,
		 * so any references to a given budget object will not change.
		 * 
		 * @return The Budget still available for running Sources
		 */
		public Allowance[] getBudget() {
			return budget;
		}

		/**
		 * @return The arguments to the CI function
		 */
		public I getArgs() {
			return args;
		}

		/**
		 * @return The Estimate object for the result of the CI
		 */
		public Estimate<O, Q> getEstimate() {
			return estimate;
		}

		/**
		 * @return The ListeningExecutorService used for parallel execution of
		 *         Source functions
		 */
		public ListeningExecutorService getPool() {
			return pool;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 *
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

				// A Lambda expression is used to create a Runnable that will
				// act as a timeout timer
				pool.submit(() -> {
					try {
						synchronized (this) {
							this.wait(time / 1000000, (int) (time % 1000000));
						}

						estimate.done(); // If the estimate isn't done yet -
											// force it to be so.
					} catch (InterruptedException e) {
					}
				});
			}

			Source<I, O, T> next;
			for (;;) {
				// Get the next source (this might block)
				Optional<Source<I, O, T>> maybeNext = sel.getNextSource(this);
				if (maybeNext.isPresent())
					next = maybeNext.get();
				else
					break; // absent optional means no more sources from
							// selector

				// Record that the source has been consulted
				remaining.remove(next);

				// Exhaust budget
				Expenditure[] cost = next.getCost(args);
				Optional<Allowance[]> newBudget = Budgets.expend(budget, cost, Optional.of(this));

				if (!newBudget.isPresent()) {
					System.err.println("Selection function chose source out of budget");
					continue;
				}

				// Run the opinion if the estimate isn't already sealed. Stop
				// running if it is.
				synchronized (estimate) {
					if (estimate.isSealed())
						return null;

					budget = newBudget.get();

					System.out.println("Calling " + next.getName()); // TODO:
																		// DEBUG

					// Query the source & augment the estimate
					ListenableFuture<Opinion<O, T>> opinion = pool
							.submit(new Source.SourceCallable<I, O, T>(next, args));
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
