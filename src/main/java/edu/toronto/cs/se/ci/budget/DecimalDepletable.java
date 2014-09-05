package edu.toronto.cs.se.ci.budget;

import java.math.BigDecimal;

import com.google.common.base.Optional;

/**
 * A generic class for depletables which deal with BigDecimal values
 * 
 * @author Michael Layzell
 *
 */
public abstract class DecimalDepletable extends BasicDepletable<BigDecimal> {
	
	/**
	 * Create a new LongDepletable with the given quantity value
	 * @param quantity
	 */
	public DecimalDepletable(BigDecimal quantity) {
		super(quantity);
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.budget.BasicDepletable#deplete(java.lang.Object)
	 */
	@Override
	public Optional<BigDecimal> deplete(BigDecimal allowance) {
		if (allowance.compareTo(getQuantity()) > 0)
			return Optional.of(allowance.subtract(getQuantity()));
		
		return Optional.absent();
	}
	
}
