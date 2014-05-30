package edu.toronto.cs.se.ci;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Budget {
	
	private final long time;
	private final ImmutableMap<String, BigDecimal> depletables;
	private final ImmutableSet<String> flags;
	
	/**
	 * Create an immutable Budget object
	 * 
	 * @param time The amount of time, in nanoseconds, in the budget
	 * @param depletables Map from units to amounts of depletable resources in budget
	 * @param flags The flags set on the budget
	 */
	public Budget(long time, Map<String, BigDecimal> depletables, Set<String> flags) {
		this.time = time;
		this.depletables = ImmutableMap.copyOf(depletables);
		this.flags = ImmutableSet.copyOf(flags);
	}

	/**
	 * @param unit The unit to return time in
	 * @return The time in the budget
	 */
	public long getTime(TimeUnit unit) {
		return unit.convert(time, TimeUnit.NANOSECONDS);
	}
	
	/**
	 * @param unit A resource type
	 * @return The amount of the resource in the budget
	 */
	public BigDecimal getBudget(String unit) {
		return depletables.getOrDefault(unit, BigDecimal.ZERO);
	}
	
	/**
	 * @param flag The name of a flag
	 * @return Whether the flag is set
	 */
	public boolean hasFlag(String flag) {
		return flags.contains(flag);
	}
	
	/**
	 * The BudgetBuilder allows easy creation of a Budget object. Simply mutate the
	 * BudgetBuilder, and then call {@code build} to create the budget object.
	 * 
	 * @author layzellm
	 *
	 */
	public static class BudgetBuilder {

		private long time = 0;
		private Map<String, BigDecimal> depletables = new HashMap<String, BigDecimal>();
		private Set<String> flags = new HashSet<String>();
	
		/**
		 * Create a empty BudgetBuilder
		 */
		public BudgetBuilder() {};
		
		/**
		 * Create a BudgetBuilder instantiated to the state of the given budget
		 * 
		 * @param budget The budget to copy the state of
		 */
		public BudgetBuilder(Budget budget) {
			time = budget.time;
			depletables.putAll(budget.depletables);
			flags.addAll(budget.flags);
		}
		
		/**
		 * Set the amount of a depletable resource
		 * 
		 * @param amount
		 * @param unit
		 * @return The BudgetBuilder for chaining
		 */
		public BudgetBuilder depletable(BigDecimal amount, String unit) {
			depletables.put(unit, amount);
			return this;
		}
		
		/**
		 * Set the time on the BudgetBuilder
		 * 
		 * @param duration
		 * @param unit
		 * @return The BudgetBuilder for chaining
		 */
		public BudgetBuilder time(long duration, TimeUnit unit) {
			time = unit.toNanos(duration);
			return this;
		}
		
		/**
		 * Sets the given flag
		 * 
		 * @param name
		 * @return The BudgetBuilder for chaining
		 */
		public BudgetBuilder flag(String name) {
			flags.add(name);
			return this;
		}
		
		/**
		 * Clears the given flag
		 * 
		 * @param name
		 * @return The BudgetBuilder for chaining
		 */
		public BudgetBuilder clearFlag(String name) {
			flags.remove(name);
			return this;
		}
		
		/**
		 * @return The built Budget object
		 */
		public Budget build() {
			return new Budget(time, depletables, flags);
		}
		
	}
}
