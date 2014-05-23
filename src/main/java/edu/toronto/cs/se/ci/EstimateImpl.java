package edu.toronto.cs.se.ci;

import java.util.HashSet;
import java.util.List;
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
	public void augment(Opinion<T> opinion) {
		if (sealed)
			throw new Error("Cannot augment a sealed Estimate");

		incomplete++;
		opinions.add(opinion);
		
		ListenableFuture<T> valFuture = opinion.getValueFuture();
		ListenableFuture<Double> trustFuture = opinion.getTrustFuture();
		
		// We can suppress the warnings here, as it is complaining about us combining
		// Futures of type T and Float. This is fine, as we don't actually care about
		// the combined values, we just want to know when they are both done.
		@SuppressWarnings("unchecked")
		ListenableFuture<List<Object>> allAsList = Futures.allAsList(valFuture, trustFuture);

		Futures.addCallback(allAsList, new FutureCallback<List<Object>>() {

			@Override
			public void onSuccess(List<Object> result) {
				if (isDone())
					return;
				
				incomplete--;
				
				if (sat != null)
					value = aggregate();
				
				// Check if we are done
				if ((sealed && incomplete <= 0) || (sat != null && sat.apply(value)))
					done();
			}

			@Override
			public void onFailure(Throwable t) {
				if (isDone())
					return;

				incomplete--;
				
				System.err.println("Error thrown by Opinion");
				t.printStackTrace();
				
				// Check if we are done
				if (sealed && incomplete <= 0)
					done();
			}
			
		});
	}
	
	/**
	 * Seals the Estimate. This marks that no more opinions will
	 * be used to augment the Estimate. If {@link augment(Opinion<T>)}
	 * is called after the Estimate is sealed, it will throw.
	 */
	public void seal() {
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
	private void done() {
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
		Set<Opinion<T>> doneOpinions = new HashSet<Opinion<T>>();
		for (Opinion<T> opinion : opinions) {
			if (opinion.isDone())
				doneOpinions.add(opinion);
		}
		
		return agg.aggregate(doneOpinions);
	}
	
	@Override
	protected void interruptTask() {
		seal();
	}

}
