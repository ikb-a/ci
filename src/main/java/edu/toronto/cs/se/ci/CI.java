package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class CI<F, T> implements Function<F, Estimate<T>> {
	
	private List<Source<F, T>> sources;
	private Aggregator<T> agg;
	private Selector<F, T> sel;

	public CI(List<Source<F, T>> sources, Aggregator<T> agg, Selector<F, T> sel) {
		this.sources = sources;
		this.agg = agg;
		this.sel = sel;
	}

	@Override
	public Estimate<T> apply(F args) {
		// TODO: Should this implement Function<>?
		return apply(args, new HashMap<String, Float>());
	}
	
	public Estimate<T> apply(F args, Map<String, Float> budget) {
		Invocation invocation = new Invocation(this, args, budget);
		return invocation.getEstimate();
	}
	
	public class Invocation implements Runnable {
		
		private CI<F, T> ci;
		private F args;
		private Map<String, Float> budget;
		
		private List<Source<F, T>> consulted;
		private List<Opinion<T>> opinions;
		private ListeningExecutorService pool = MoreExecutors.listeningDecorator(new ForkJoinPool());
		private EstimateImpl<T> estimate = new EstimateImpl<T>(agg, null);
		
		private ListenableFuture<?> internalFuture;

		private Invocation(CI<F, T> ci, F args, Map<String, Float> budget) {
			this.ci = ci;
			this.args = args;
			this.budget = budget;
			
			consulted = new ArrayList<Source<F, T>>(ci.sources.size());
			opinions = new ArrayList<Opinion<T>>(ci.sources.size());
			
			internalFuture = pool.submit(this);
		}

		public Estimate<T> getEstimate() {
			return estimate;
		}
		
		public ListeningExecutorService getPool() {
			return pool;
		}

		@Override
		public void run() {
			Source<F, T> next;
			for (;;) {
				// Get the next source (this might block)
				next = ci.sel.getNextSource(consulted, opinions, ci.sources, budget, args);
				if (next == null)
					break;

				// Get the value and trust, augmenting the estimate
				ListenableFuture<T> value = pool.submit(new Source.SourceCallable<F, T>(next, args));
				ListenableFuture<Float> trust = pool.submit(new Source.SourceTrustCallable<F, T>(next, value, args));
				Opinion<T> opinion = new Opinion<T>(value, trust);
				estimate.augment(opinion);
			}
			
			// Seal the estimate
			estimate.seal();
		}
	}

}
