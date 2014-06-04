package edu.toronto.cs.se.ebt;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.TrapezoidIntegrator;

public final class EBT {
	
	private EBT() {}
	
	public static double confidence(Evidence e) {
		return confidence(e.getConsenting(), e.getDissenting());
	}

	public static double confidence(final double r, final double s) {
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
