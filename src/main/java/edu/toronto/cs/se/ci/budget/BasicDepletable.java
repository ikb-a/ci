package edu.toronto.cs.se.ci.budget;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;

/**
 * A BasicDepletable represents a depletable resource, and the expenditures involved in
 * consuming that depletable resource. The expenditure will deplete any allowances which
 * are of the same Class.
 * 
 * Concrete subclasses of BasicDepletable should _not_ be generic, as otherwise Type Erasure will prevent
 * acceptable targets from being correctly identified, and the algorithm may act strangely.
 * 
 * @author Michael Layzell
 *
 * @param <T> The quantity type wrapped by the BasicDepletable
 */
public abstract class BasicDepletable<T> implements Allowance, Expenditure, Cloneable {
	
	private T quantity;
	
	public BasicDepletable(T quantity) {
		this.quantity = quantity;
	}
	
	/**
	 * This returns the result of removing the quantity of this BasicDepletable from the given
	 * quantity passed in. If the resulting quantity is invalid, return {@link Optional.absent()},
	 * otherwise return {@link Optional.of(T)}
	 * 
	 * @param allowance The allowed quantity value
	 * @return Either {@link Optional.of(T)} representing the remaining depletable, or {@link Optional.absent()}
	 */
	protected abstract Optional<T> deplete(T allowance);
	
	/**
	 * @return The quantity contained within the Depletable
	 */
	public T getQuantity() {
		return quantity;
	}

	/**
	 * Creates a new LongDepletable with the same quantity as the current LongDepletable,
	 * and the same Class. This is done by using {@link Object.clone()} and then modifying
	 * the {@code quantity} field.
	 * 
	 * @param quantity The value for the new {@code quantity} field
	 * @return A new LongDepletable object
	 */
	public BasicDepletable<T> withQuantity(T quantity) {
		try {
			@SuppressWarnings("unchecked")
			BasicDepletable<T> clone = (BasicDepletable<T>) this.clone();
			clone.quantity = quantity;
			
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // This shouldn't happen
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.budget.Expenditure#expend(edu.toronto.cs.se.ci.budget.Allowance[], com.google.common.base.Optional)
	 */
	@Override
	public Optional<Allowance[]> expend(Allowance[] budget, Optional<CI<?, ?>.Invocation> invocation) {
		Allowance[] newBudget = new Allowance[budget.length];
		boolean seen = false;

		for (int i = 0; i < budget.length; i++) {
			newBudget[i] = budget[i];
			
			if (this.getClass().isInstance(budget[i])) {
				// If the class matches between this, and the current allowance, then try to
				// use the deplete(T) function to reduce the resources remaining in the budget
				try {
					@SuppressWarnings("unchecked")
					BasicDepletable<T> allowance = (BasicDepletable<T>) budget[i];
					Optional<T> depleted = deplete(allowance.getQuantity());
					
					if (depleted.isPresent()) {
						newBudget[i] = allowance.withQuantity(depleted.get());
						seen = true;
					} else {
						return Optional.absent();
					}
				} catch (ClassCastException e) {
					// Something horrible has happened with Generics
					// Let us avert our eyes, and carry on.
				}
			}
		}
		
		if (seen)
			return Optional.of(newBudget);
		else
			return Optional.absent(); // If the depletable is not present - return Optional.absent()
	}
	
}
