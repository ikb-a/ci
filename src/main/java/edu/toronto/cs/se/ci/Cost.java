package edu.toronto.cs.se.ci;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Cost {
	
	public enum FlagStatus {
		REQUIRED,
		IGNORED,
		RESTRICTED
	}
	
	private long time = 0;
	private Map<String, Double> depletables = new HashMap<String, Double>();
	private Map<String, FlagStatus> flagReqs = new HashMap<String, FlagStatus>();
	
	public Cost() {}
	
	public long getTime(TimeUnit unit) {
		return unit.convert(time, TimeUnit.NANOSECONDS);
	}

	public void setTime(long t, TimeUnit unit) {
		time = unit.toNanos(t);
	}
	
	public double getCost(String unit) {
		return depletables.getOrDefault(unit, 0.0);
	}
	
	public Map<String, Double> getCosts() {
		return depletables;
	}

	public void setCost(double amount) {
		setCost(amount, "CAD");
	}

	public void setCost(double amount, String unit) {
		depletables.put(unit, amount);
	}
	
	public void require(String flag) {
		flagReqs.put(flag, FlagStatus.REQUIRED);
	}
	
	public void restrict(String flag) {
		flagReqs.put(flag, FlagStatus.RESTRICTED);
	}
	
	public void ignore(String flag) {
		flagReqs.remove(flag);
	}
	
	public FlagStatus getFlagStatus(String flag) {
		return flagReqs.getOrDefault(flag, FlagStatus.IGNORED);
	}
	
	public void setFlagStatus(String flag, FlagStatus status) {
		if (status == FlagStatus.IGNORED)
			flagReqs.remove(flag);
		else
			flagReqs.put(flag, status);
	}
	
	public Map<String, FlagStatus> getFlagReqs() {
		return flagReqs;
	}
	
}
