package edu.toronto.cs.se.ci.data;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.TrapezoidIntegrator;

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
	
	private final double r, s;
	
	/**
	 * Create a value in the Evidence space. Total is inferred.
	 * 
	 * @param consenting
	 * @param dissenting
	 */
	public Evidence(double consenting, double dissenting) {
		this.r = consenting;
		this.s = dissenting;
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

			if (new Evidence(r, s).getConfidence() < conf)
				t1 = t;
			else
				t2 = t;
		}
		
		this.r = r;
		this.s = s;
	}

	/**
	 * @return This value, in the Trust space.
	 */
	public Trust toTrust() {
		return new Trust(this);
	}
	
	public double getConsenting() {
		return r;
	}
	
	public double getDissenting() {
		return s;
	}
	
	public double getTotal() {
		return r + s;
	}

	public double getConfidence() {
		try {
			TrapezoidIntegrator integrator = new TrapezoidIntegrator();

			UnivariateRealFunction p = new UnivariateRealFunction() {

				@Override
				public double value(double x) throws FunctionEvaluationException {
					return Math.pow(x, r) * Math.pow(1 - x, s);
				}
				
			};

			final double intp = integrator.integrate(p, 0, 1);
			
			// If our initial integration returns a value of 0, we can't compute anything
			// more. We assume that there is sufficient evidence that the answer is certain
			if (intp == 0)
				return 1;
			
			UnivariateRealFunction q = new UnivariateRealFunction() {

				@Override
				public double value(double x) throws FunctionEvaluationException {
					return Math.abs((Math.pow(x, r) * Math.pow(1 - x, s)) / intp - 1);
				}
				
			};
			
			return integrator.integrate(q, 0, 1) / 2;
		} catch (MaxIterationsExceededException | FunctionEvaluationException
				| IllegalArgumentException e) {
			// This shouldn't happen, and I don't feel like declaring the throws right now.
			throw new Error(e);
		}
	}


}
