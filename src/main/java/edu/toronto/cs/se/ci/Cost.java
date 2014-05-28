package edu.toronto.cs.se.ci;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

public class Cost {
	
	public enum FlagStatus {
		REQUIRED,
		IGNORED,
		RESTRICTED
	}
	
	private final long time;
	private final ImmutableMap<String, BigDecimal> depletables;
	private final ImmutableMap<String, FlagStatus> flagReqs;
	
	public Cost(long time, Map<String, BigDecimal> depletables, Map<String, FlagStatus> flagReqs) {
		this.time = time;
		this.depletables = ImmutableMap.copyOf(depletables);
		this.flagReqs = ImmutableMap.copyOf(flagReqs);
	}

	public long getTime(TimeUnit unit) {
		return unit.convert(time, TimeUnit.NANOSECONDS);
	}

	public BigDecimal getCost(String unit) {
		return depletables.getOrDefault(unit, BigDecimal.ZERO);
	}
	
	public FlagStatus getFlagStatus(String flag) {
		return flagReqs.getOrDefault(flag, FlagStatus.IGNORED);
	}
	
	public boolean withinBudget(Budget budget, long elapsed, TimeUnit unit) {
		if (budget.getTime(TimeUnit.NANOSECONDS) < time + unit.toNanos(elapsed))
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
	
	public Budget spend(Budget budget) {
		Budget.BudgetBuilder builder = new Budget.BudgetBuilder(budget);
		
		for (Map.Entry<String, BigDecimal> depletable : depletables.entrySet()) {
			String unit = depletable.getKey();
			builder.depletable(budget.getBudget(unit).subtract(depletable.getValue()), unit);
		}
		
		return builder.build();
	}
	
	public static CostBuilder builder() {
		return new CostBuilder();
	}
	
	public static class CostBuilder {
		
		private long time = 0;
		private Map<String, BigDecimal> depletables = new HashMap<String, BigDecimal>();
		private Map<String, FlagStatus> flagReqs = new HashMap<String, FlagStatus>();
		
		public CostBuilder() {};
		
		public CostBuilder(Cost cost) {
			time = cost.time;
			depletables.putAll(cost.depletables);
			flagReqs.putAll(cost.flagReqs);
		}
		
		public CostBuilder time(long duration, TimeUnit unit) {
			time = unit.toNanos(duration);
			return this;
		}
		
		public CostBuilder depletable(BigDecimal depletable, String unit) {
			this.depletables.put(unit, depletable);
			return this;
		}
		
		public CostBuilder require(String flag) {
			this.flagReqs.put(flag, FlagStatus.REQUIRED);
			return this;
		}
		
		public CostBuilder restrict(String flag) {
			this.flagReqs.put(flag, FlagStatus.RESTRICTED);
			return this;
		}
		
		public CostBuilder ignore(String flag) {
			this.flagReqs.remove(flag);
			return this;
		}
		
		public Cost build() {
			return new Cost(time, depletables, flagReqs);
		}
	
	}
	
}
