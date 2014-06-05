package edu.toronto.cs.se.ebt;

/**
 * A representation of the Trust triple <b, d, u> from [Wang and Singh, 2010].
 * 
 * @author Michael Layzell
 *
 */
public final class Trust {
	
	private final double belief, disbelief;
	
	/**
	 * Create a value within the Trust space. Uncertainty is inferred.
	 * 
	 * @param belief
	 * @param disbelief
	 */
	public Trust(double belief, double disbelief) {
		this.belief = belief;
		this.disbelief = disbelief;
	}
	
	/**
	 * Get a value in the Trust space which is approximately equivalent to the passed Evidence
	 * 
	 * @param evidence
	 */
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
