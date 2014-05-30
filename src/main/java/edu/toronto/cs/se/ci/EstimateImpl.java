package edu.toronto.cs.se.ci;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class EstimateImpl<T> extends AbstractFuture<Result<T>> implements Estimate<T> {
	
	private Set<Opinion<T>> opinions = new HashSet<Opinion<T>>();
	private int incomplete = 0;
	private boolean sealed = false;
	private Result<T> value = null;
	
	// Functions
	private Aggregator<T> agg;
	private Function<Result<T>, Boolean> sat;
	
	public EstimateImpl(Aggregator<T> agg, Function<Result<T>, Boolean> sat) {
		this.agg = agg;
		this.sat = sat;
	}

	/**
	 * Augments the Estimate with a new opinion. Cannot be called if the
	 * estimate has been sealed already.
	 * 
	 * @param opinion The opinion to augment the Estimate with
	 */
	public synchronized void augment(ListenableFuture<Opinion<T>> opinion) {
		if (sealed)
			throw new Error("Cannot augment a sealed Estimate");

		incomplete++;

		Futures.addCallback(opinion, new FutureCallback<Opinion<T>>() {

			@Override
			public void onSuccess(Opinion<T> opinion) {
				synchronized(EstimateImpl.this) {
					if (isDone())
						return;
					
					// We can record the opinion now!
					opinions.add(opinion);
					incomplete--;
					
					// Caching
					if (sat != null)
						value = aggregate();
					
					// Check if we are done
					if ((sealed && incomplete <= 0) || (sat != null && sat.apply(value)))
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
	public Result<T> getCurrent() {
		if (value == null)
			return aggregate();
		else
			return value;
	}
	
	/**
	 * Mark the Estimate as complete, firing callbacks etc.
	 */
	private synchronized void done() {
		sealed = true;

		if (isDone())
			return;
		
		value = getCurrent();
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
	private Result<T> aggregate() {
		return agg.aggregate(opinions);
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

}
