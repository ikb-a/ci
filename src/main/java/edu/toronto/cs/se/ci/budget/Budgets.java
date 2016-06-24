package edu.toronto.cs.se.ci.budget;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.GenericCI;

/**
 * Static class containing useful methods for interacting with Budgets
 * 
 * @author Michael Layzell
 *
 */
public final class Budgets {

	private Budgets() {
	}

	/**
	 * Produce a new budget, as though cost had been spent in the original
	 * budget
	 * 
	 * @param budget
	 *            The budget to spend the cost from
	 * @param cost
	 *            The cost to spend from the budget
	 * @return A new budget, or Optional.absent() if the budget cannot support
	 *         the cost
	 */
	public static Optional<Allowance[]> expend(Allowance[] budget, Expenditure[] cost) {
		return expend(budget, cost, Optional.absent());
	}

	/**
	 * Produce a new budget, as though cost had been spent in the original
	 * budget
	 * 
	 * <p>
	 * This call will allow the functions to interact with the current CI
	 * invocation
	 * 
	 * @param budget
	 *            The budget to spend the cost from
	 * @param cost
	 *            The cost to spend from the budget
	 * @param invocation
	 *            The current CI invocation
	 * @return A new budget, or Optional.absent() if the budget cannot support
	 *         the cost
	 */
	public static Optional<Allowance[]> expend(Allowance[] budget, Expenditure[] cost,
			Optional<GenericCI<?, ?, ?, ?, ?>.Invocation> invocation) {
		for (Expenditure expenditure : cost) {
			Optional<Allowance[]> spent = expenditure.expend(budget, invocation);
			if (spent.isPresent())
				budget = spent.get();
			else
				return Optional.absent();
		}

		return Optional.of(budget);
	}

	/**
	 * Determine if the given cost is within budget
	 * 
	 * <p>
	 * This call will allow the functions to interact with the current CI
	 * invocation
	 * 
	 * @param budget
	 *            The budget to spend the cost from
	 * @param cost
	 *            The cost to spend from the budget
	 * @param invocation
	 *            The current CI invocation
	 * @return Whether the given cost is within budget
	 */
	public static boolean withinBudget(Allowance[] budget, Expenditure[] cost,
			Optional<GenericCI<?, ?, ?, ?, ?>.Invocation> invocation) {
		return expend(budget, cost, invocation).isPresent();
	}

	/**
	 * Determine if the given cost is within budget
	 * 
	 * @param budget
	 *            The budget to spend the cost from
	 * @param cost
	 *            The cost to spend from the budget
	 * @return Whether the given cost is within budget
	 */
	public static boolean withinBudget(Allowance[] budget, Expenditure[] cost) {
		return expend(budget, cost).isPresent();
	}

	/**
	 * Return all elements of Budget which have the given class
	 * 
	 * @param budget
	 *            The budget
	 * @param clazz
	 *            The class to search for
	 * @return All elements of the budget with the given class
	 */
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
