package edu.toronto.cs.se.ci.budget;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;

public final class Budgets {
	
	private Budgets() {}
	
	public static Optional<Allowance[]> expend(Allowance[] budget, Expenditure[] cost) {
		return expend(budget, cost, Optional.absent());
	}

	public static Optional<Allowance[]> expend(Allowance[] budget, Expenditure[] cost, Optional<CI<?, ?>.Invocation> invocation) {
		for (Expenditure expenditure : cost) {
			Optional<Allowance[]> spent = expenditure.expend(budget, invocation);
			if (spent.isPresent())
				budget = spent.get();
			else
				return Optional.absent();
		}
		
		return Optional.of(budget);
	}
	
	public static boolean withinBudget(Allowance[] budget, Expenditure[] cost, Optional<CI<?, ?>.Invocation> invocation) {
		return expend(budget, cost, invocation).isPresent();
	}

	public static boolean withinBudget(Allowance[] budget, Expenditure[] cost) {
		return expend(budget, cost).isPresent();
	}

	public static <T> Optional<T> getByClass(Allowance[] budget, Class<T> clazz) {
		for (Allowance allowance : budget) {
			if (clazz.isInstance(allowance)) {
				@SuppressWarnings("unchecked")
				T chosen = (T) allowance;

				return Optional.of(chosen);
			}
		}

		return Optional.absent();
	}

}
