package edu.toronto.cs.se.ebt;

public final class Trust {
	
	private final double belief, disbelief;
	
	public Trust(double belief, double disbelief) {
		this.belief = belief;
		this.disbelief = disbelief;
	}
	
	public Trust(Evidence evidence) {
		double c = EBT.confidence(evidence);
		double alpha = evidence.getConsenting() / evidence.getTotal();

		this.belief = alpha * c;
		this.disbelief = (1 - alpha) * c;
	}
	
	public double getBelief() {
		return belief;
	}
	
	public double getDisbelief() {
		return disbelief;
	}
	
	public double getUncertainty() {
		return 1 - belief - disbelief;
	}

}
