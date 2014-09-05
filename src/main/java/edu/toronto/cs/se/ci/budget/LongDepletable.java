package edu.toronto.cs.se.ci.budget;

import com.google.common.base.Optional;

/**
 * A generic class for depletables which deal with Long values
 * 
 * @author Michael Layzell
 *
 */
public abstract class LongDepletable extends BasicDepletable<Long> {
	
	/**
	 * Create a new LongDepletable with the given quantity value
	 * @param quantity
	 */
	public LongDepletable(long quantity) {
		super(quantity);
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.budget.BasicDepletable#deplete(java.lang.Object)
	 */
	@Override
	public Optional<Long> deplete(Long allowance) {
		if (allowance > getQuantity())
			return Optional.of(allowance - getQuantity());
		
		return Optional.absent();
	}
	
}
