package edu.toronto.cs.se.ci.budget;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;

/**
 * An expenditure represents a cost. The cost of a particular source
 * is represented as an array of Expenditure objects.
 * 
 * @author Michael Layzell
 *
 */
public interface Expenditure {
	
	/**
	 * Expend the amount of the expenditure from the budget.
	 * @param budget The budget to expend from
	 * @param invocation The current CI invocation
	 * @return Either the new budget, less the Expenditure, or Optional.absent()
	 */
	public Optional<Allowance[]> expend(Allowance[] budget, Optional<CI<?, ?, ?, ?>.Invocation> invocation);

}
