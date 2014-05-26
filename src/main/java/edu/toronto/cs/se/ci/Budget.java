package edu.toronto.cs.se.ci;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Budget {
	
	private long time = 0;
	private Map<String, Double> depletables = new HashMap<String, Double>();
	private Set<String> flags = new HashSet<String>();
	
	public Budget() {}
	
	public Budget(Budget other) {
		time = other.time;
		depletables.putAll(other.depletables);
		flags.addAll(other.flags);
	}

	public long getTime(TimeUnit unit) {
		return unit.convert(time, TimeUnit.NANOSECONDS);
	}
	
	public void setTime(long t, TimeUnit unit) {
		time = unit.toNanos(t);
	}
	
	public double getBudget(String unit) {
		return depletables.getOrDefault(unit, 0.0);
	}
	
	public void setBudget(double value, String unit) {
		depletables.put(unit, value);
	}
	
	public boolean hasFlag(String flag) {
		return flags.contains(flag);
	}
	
	public void setFlag(String flag) {
		flags.add(flag);
	}
	
	public void clearFlag(String flag) {
		flags.remove(flag);
	}
	
	public boolean withinBudget(Cost cost) {
		return withinBudget(cost, 0);
	}

	public boolean withinBudget(Cost cost, long elapsed) {
		// Check time
		if (cost.getTime(TimeUnit.NANOSECONDS) > (time - elapsed))
			return false;

		// Check flag reqs
		Map<String, Cost.FlagStatus> flagReqs = cost.getFlagReqs();
		for (Map.Entry<String, Cost.FlagStatus> status : flagReqs.entrySet()) {
			if (status.getValue() == Cost.FlagStatus.REQUIRED
					&& ! flags.contains(status.getKey())) {
                return false;
			} else if (status.getValue() == Cost.FlagStatus.RESTRICTED
					&& flags.contains(status.getKey())) {
				return false;
			}
		}
		
		// Check depletable costs
		Map<String, Double> deps = cost.getCosts();
		for (Map.Entry<String, Double> depletable : deps.entrySet()) {
			double remaining = depletables.getOrDefault(depletable.getKey(), 0.0);
			if (depletable.getValue() > remaining) {
				return false;
			}
		}

		return true;
	}
	
	public boolean expend(Cost cost) {
		return expend(cost, 0);
	}

	public boolean expend(Cost cost, long elapsed) {
		if (! withinBudget(cost, elapsed))
			return false;
		
		Map<String, Double> deps = cost.getCosts();
		for (Map.Entry<String, Double> depletable : deps.entrySet()) {
			double remaining = depletables.getOrDefault(depletable.getKey(), 0.0);
			remaining -= depletable.getValue();
			depletables.put(depletable.getKey(), remaining);
		}
		
		return true;
	}
	
}
