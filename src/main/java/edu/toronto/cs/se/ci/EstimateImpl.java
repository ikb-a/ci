package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;

/**
 * Concrete implementation of {@link Estimate}. Used internally by CI.
 * 
 * @author Michael Layzell
 *
 * @param <O>
 */
public class EstimateImpl<O, T, Q> extends AbstractFuture<Result<O, Q>> implements Estimate<O, Q> {
	
	private Set<Opinion<O, T>> opinions = new HashSet<Opinion<O, T>>();
	private int incomplete = 0;
	private boolean sealed = false;
	private Result<O, Q> value = null;
	
	// Functions
	private Aggregator<O, T, Q> agg;
	private Acceptor<O, Q> acceptor;
	
	// Listeners
	private List<Listener> listeners = new ArrayList<>();
	
	public EstimateImpl(Aggregator<O, T, Q> agg, Acceptor<O, Q> acceptor) {
		this.agg = agg;
		this.acceptor = acceptor;
	}

	/**
	 * Augments the Estimate with a new opinion. Cannot be called if the
	 * estimate has been sealed already.
	 * 
	 * @param opinion The opinion to augment the Estimate with
	 */
	public synchronized void augment(ListenableFuture<Opinion<O, T>> opinion) {
		if (sealed)
			throw new Error("Cannot augment a sealed Estimate");

		incomplete++;

		Futures.addCallback(opinion, new FutureCallback<Opinion<O, T>>() {

			@Override
			public void onSuccess(Opinion<O, T> opinion) {
				synchronized(EstimateImpl.this) {
					if (isDone())
						return;
					
					// We can record the opinion now!
					opinions.add(opinion);
					incomplete--;
					
					// Caching
					if (acceptor != null)
						value = aggregate();
					
					for (Listener listener : listeners) {
						try {
							listener.execute();
						} catch (Exception e) {
							System.err.print("Exception while executing listener: ");
							e.printStackTrace();
						}
					}
					
					// Check if we are done
					if ((sealed && incomplete <= 0) || (acceptor != null && acceptor.isAcceptable(value)))
						done();
				}
			}

			@Override
			public void onFailure(Throwable t) {
				synchronized(EstimateImpl.this) {
					if (isDone())
						return;

					// We can still mark it as incomplete
					incomplete--;
					
					// Log the error TODO: Remove?
					System.err.println("Error thrown by Opinion");
					t.printStackTrace();
					
					// Check if we are done
					if (sealed && incomplete <= 0)
						done();
				}
			}
			
		});
	}
	
	/**
	 * Seals the Estimate. This marks that no more opinions will
	 * be used to augment the Estimate. If {@link augment(Opinion<T>)}
	 * is called after the Estimate is sealed, it will throw.
	 */
	public synchronized void seal() {
		if (sealed)
			return;
		
		sealed = true;
		
		if (incomplete <= 0)
			done();
	}
	
	/**
	 * @return Whether the estimate has been sealed
	 */
	public boolean isSealed() {
		return sealed;
	}

	@Override
	public Optional<Result<O, Q>> getCurrent() {
		if (value == null)
			return Optional.fromNullable(aggregate());
		else
			return Optional.of(value);
	}
	
	/**
	 * Mark the Estimate as complete, firing callbacks etc.
	 */
	public synchronized void done() {
		sealed = true;

		if (isDone())
			return;
		
		value = getCurrent().orNull();
		if (value == null)
			setException(new Error("Unknown")); // TODO: More meaningful error? Should it throw?
		else
			set(value);
	}
	
	/**
	 * Filters out incomplete opinions, and calls agg.aggregate
	 * with the complete ones.
	 * @return The aggregate opinion based on done opinions
	 */
	private Result<O, Q> aggregate() {
		try {
			return agg.aggregate(opinions);
		} catch (Exception e) {
			// There was a problem aggregating
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.google.common.util.concurrent.AbstractFuture#interruptTask()
	 */
	@Override
	protected void interruptTask() {
		// When the task has been interrupted, we need to seal the estimate, such that
		// no more sources can be added to the Estimate.
		seal();
	}

	@Override
	public synchronized void addPartialListener(Runnable listener, Executor executor) {
		listeners.add(new Listener(listener, executor));
	}
	
	private class Listener {
		
		private final Runnable listener;
		private final Executor executor;
		
		public Listener(Runnable listener, Executor executor) {
			if (listener == null || executor == null)
				throw new NullPointerException("Runnable/Executor not null.");

			this.listener = listener;
			this.executor = executor;
		}
		
		public void execute() {
			executor.execute(listener);
		}

	}

}
