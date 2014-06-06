package edu.toronto.cs.se.ebt;

/**
 * A representation of the Evidence triple <r, s, t> from [Wang and Singh, 2010].
 * 
 * @author Michael Layzell
 *
 */
public final class Evidence {
	
	/**
	 * Default value for {@code epsilon} when converting from the Trust space into the Evidence space.
	 */
	public static final double EPSILON = 1;
	
	/**
	 * Default value for {@code tMax} when converting from the Trust space into the Evidence space.
	 */
	public static final double T_MAX = 1000;
	
	private final double consenting, dissenting;
	
	/**
	 * Create a value in the Evidence space. Total is inferred.
	 * 
	 * @param consenting
	 * @param dissenting
	 */
	public Evidence(double consenting, double dissenting) {
		this.consenting = consenting;
		this.dissenting = dissenting;
	}
	
	/**
	 * Convert a value in the Trust space into the Evidence space. <br>
	 * Uses the default values for {@code epsilon} and {@code tMax}
	 * 
	 * @param trust The value in the Trust space.
	 */
	public Evidence(Trust trust) {
		this(trust, EPSILON, T_MAX);
	}

	/**
	 * Convert a value in the Trust space into the Evidence space.
	 * 
	 * @param trust The value in the Trust space.
	 * @param epsilon Maximum error in estimated t
	 * @param tMax The upper bound on t
	 */
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

	/**
	 * @return This value, in the Trust space.
	 */
	public Trust toTrust() {
		return new Trust(this);
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
