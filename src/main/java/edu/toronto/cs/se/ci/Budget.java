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
	
	public Budget(long time, Map<String, BigDecimal> depletables, Set<String> flags) {
		this.time = time;
		this.depletables = ImmutableMap.copyOf(depletables);
		this.flags = ImmutableSet.copyOf(flags);
	}

	public long getTime(TimeUnit unit) {
		return unit.convert(time, TimeUnit.NANOSECONDS);
	}
	
	public BigDecimal getBudget(String unit) {
		return depletables.getOrDefault(unit, BigDecimal.ZERO);
	}
	
	public boolean hasFlag(String flag) {
		return flags.contains(flag);
	}
	
	public static BudgetBuilder builder() {
		return new BudgetBuilder();
	}

	public static class BudgetBuilder {

		private long time = 0;
		private Map<String, BigDecimal> depletables = new HashMap<String, BigDecimal>();
		private Set<String> flags = new HashSet<String>();
	
		public BudgetBuilder() {};
		
		public BudgetBuilder(Budget budget) {
			time = budget.time;
			depletables.putAll(budget.depletables);
			flags.addAll(budget.flags);
		}
		
		public BudgetBuilder depletable(BigDecimal amount, String unit) {
			depletables.put(unit, amount);
			return this;
		}
		
		public BudgetBuilder time(long duration, TimeUnit unit) {
			time = unit.toNanos(duration);
			return this;
		}
		
		public BudgetBuilder flag(String name) {
			flags.add(name);
			return this;
		}
		
		public BudgetBuilder clearFlag(String name) {
			flags.remove(name);
			return this;
		}
		
		public Budget build() {
			return new Budget(time, depletables, flags);
		}
		
	}
}
