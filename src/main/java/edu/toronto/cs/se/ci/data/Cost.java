package edu.toronto.cs.se.ci.data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

/**
 * A Cost is a representation of the resource cost of querying a source.
 * A Cost consists of a time cost, depletable costs, and a set of flags requirements,
 * which can be used to restrict which sources will be run.
 * 
 * @author Michael Layzell
 *
 */
public class Cost {
	
	public enum FlagStatus {
		REQUIRED,
		IGNORED,
		RESTRICTED
	}
	
	private final long time;
	private final ImmutableMap<String, BigDecimal> depletables;
	private final ImmutableMap<String, FlagStatus> flagReqs;
	
	/**
	 * Create a Cost object. Can also be done using the {@link CostBuilder}
	 * 
	 * @param time
	 * @param depletables
	 * @param flagReqs
	 */
	public Cost(long time, Map<String, BigDecimal> depletables, Map<String, FlagStatus> flagReqs) {
		this.time = time;
		this.depletables = ImmutableMap.copyOf(depletables);
		this.flagReqs = ImmutableMap.copyOf(flagReqs);
	}

	/**
	 * @param unit
	 * @return The time amount of the Cost
	 */
	public long getTime(TimeUnit unit) {
		return unit.convert(time, TimeUnit.NANOSECONDS);
	}

	/**
	 * @param unit 
	 * @return The depletable cost for the given type
	 */
	public BigDecimal getCost(String unit) {
		return depletables.getOrDefault(unit, BigDecimal.ZERO);
	}
	
	/**
	 * @param flag The name of the flag
	 * @return The flag's status, either REQUIRED, IGNORED, or RESTRICTED
	 */
	public FlagStatus getFlagStatus(String flag) {
		return flagReqs.getOrDefault(flag, FlagStatus.IGNORED);
	}
	
	/**
	 * @param budget The Budget object
	 * @param elapsed How much time has elapsed
	 * @param unit The unit of time which has elapsed
	 * @return {@code true} if the cost fits within the budget, {@code false} otherwise
	 */
	public boolean withinBudget(Budget budget, long elapsed, TimeUnit unit) {
		long nanos = budget.getTime(TimeUnit.NANOSECONDS);
		
		if (nanos > 0 && nanos < time + unit.toNanos(elapsed))
			return false;
		
		for (Map.Entry<String, BigDecimal> depletable : depletables.entrySet()) {
			if (depletable.getValue().compareTo(budget.getBudget(depletable.getKey())) > 0)
				return false;
		}
		
		for (Map.Entry<String, FlagStatus> flagReq : flagReqs.entrySet()) {
			if (flagReq.getValue() == FlagStatus.REQUIRED && ! budget.hasFlag(flagReq.getKey()))
				return false;
			
			if (flagReq.getValue() == FlagStatus.RESTRICTED && budget.hasFlag(flagReq.getKey()))
				return false;
		}
		
		return true;
	}
	
	/**
	 * @param budget The Budget object
	 * @return A new budget, with less depletables equivalent to the depletable cost of the Cost
	 */
	public Budget spend(Budget budget) {
		Budget.BudgetBuilder builder = new Budget.BudgetBuilder(budget);
		
		for (Map.Entry<String, BigDecimal> depletable : depletables.entrySet()) {
			String unit = depletable.getKey();
			builder.depletable(budget.getBudget(unit).subtract(depletable.getValue()), unit);
		}
		
		return builder.build();
	}
	
	/**
	 * The CostBuilder allows easy creation of a Cost object. Simply mutate the
	 * CostBuilder, and then call {@code build} to create the Cost object.
	 * 
	 * @author layzellm
	 *
	 */
	public static class CostBuilder {
		
		private long time = 0;
		private Map<String, BigDecimal> depletables = new HashMap<String, BigDecimal>();
		private Map<String, FlagStatus> flagReqs = new HashMap<String, FlagStatus>();
		
		/**
		 * Create an empty CostBuilder
		 */
		public CostBuilder() {};
		
		/**
		 * Create a cost builder with initial values
		 * 
		 * @param cost The Cost to copy the initial values from
		 */
		public CostBuilder(Cost cost) {
			time = cost.time;
			depletables.putAll(cost.depletables);
			flagReqs.putAll(cost.flagReqs);
		}
		
		/**
		 * Set the time cost of the CostBuilder
		 * 
		 * @param duration
		 * @param unit
		 * @return The CostBuilder for chaining
		 */
		public CostBuilder time(long duration, TimeUnit unit) {
			time = unit.toNanos(duration);
			return this;
		}
		
		/**
		 * Set the depletable cost of the CostBuilder
		 * 
		 * @param depletable
		 * @param unit
		 * @return The CostBuilder for chaining
		 */
		public CostBuilder depletable(BigDecimal depletable, String unit) {
			this.depletables.put(unit, depletable);
			return this;
		}
		
		/**
		 * Set a flag as REQUIRED
		 * 
		 * @param flag
		 * @return The CostBuilder for chaining
		 */
		public CostBuilder require(String flag) {
			this.flagReqs.put(flag, FlagStatus.REQUIRED);
			return this;
		}
		
		/**
		 * Set a flag as RESTRICTED
		 * 
		 * @param flag
		 * @return The CostBuilder for chaining
		 */
		public CostBuilder restrict(String flag) {
			this.flagReqs.put(flag, FlagStatus.RESTRICTED);
			return this;
		}
		
		/**
		 * Set a flag as IGNORED
		 * 
		 * @param flag
		 * @return The CostBuilder for chaining
		 */
		public CostBuilder ignore(String flag) {
			this.flagReqs.remove(flag);
			return this;
		}
		
		/**
		 * @return The built Cost object
		 */
		public Cost build() {
			return new Cost(time, depletables, flagReqs);
		}
	
	}
	
}
