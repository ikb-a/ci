package edu.toronto.cs.se.ci.budget.basic;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;
import edu.toronto.cs.se.ci.budget.Allowance;
import edu.toronto.cs.se.ci.budget.Expenditure;

/**
 * Represents a requirement on a given flag. If the flag is not present within the budget,
 * then this Expenditure cannot be expended. If it is, then it is expended without causing
 * any changes to the Budget.
 * 
 * @author Michael Layzell
 *
 */
public class RequiredFlag implements Expenditure {
	
	private String name;
	
	public RequiredFlag(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.budget.Expenditure#expend(edu.toronto.cs.se.ci.budget.Allowance[], com.google.common.base.Optional)
	 */
	@Override
	public Optional<Allowance[]> expend(Allowance[] budget, Optional<CI<?, ?>.Invocation> invocation) {
		for (Allowance allowance : budget) {
			if (allowance instanceof Flag && ((Flag) allowance).getName().equals(name)) {
				return Optional.of(budget);
			}
		}

		return Optional.absent();
	}

}
