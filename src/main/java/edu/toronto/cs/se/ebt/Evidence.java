package edu.toronto.cs.se.ebt;

public final class Evidence {
	
	public static final double EPSILON = 1;
	public static final double T_MAX = 1000;
	
	private final double consenting, dissenting;
	
	public Evidence(double consenting, double dissenting) {
		this.consenting = consenting;
		this.dissenting = dissenting;
	}
	
	public Evidence(Trust trust) {
		this(trust, EPSILON, T_MAX);
	}

	public Evidence(Trust trust, double epsilon, double tMax) {
		double alpha = trust.getBelief() / (trust.getBelief() + trust.getDisbelief());
		double conf = 1 - trust.getUncertainty();
		double t1 = 0;
		double t2 = tMax;
		
		// Current estimated r & s values
		double r = 0;
		double s = 0;

		while (t2 - t1 >= epsilon) {
			double t = (t1 + t2) /2;
			r = alpha * t;
			s = t - r;

			if (EBT.confidence(r, s) < conf)
				t1 = t;
			else
				t2 = t;
		}
		
		consenting = r;
		dissenting = s;
	}
	
	public double getConsenting() {
		return consenting;
	}
	
	public double getDissenting() {
		return dissenting;
	}
	
	public double getTotal() {
		return consenting + dissenting;
	}

}
