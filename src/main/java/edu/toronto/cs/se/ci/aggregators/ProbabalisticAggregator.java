package edu.toronto.cs.se.ci.aggregators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.TrapezoidIntegrator;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

public class ProbabalisticAggregator implements Aggregator<Boolean> {
	
	private double epsilon;
	
	public ProbabalisticAggregator(double acceptableTError) {
		epsilon = acceptableTError;
	}

	@Override
	public Result<Boolean> aggregate(Iterable<Opinion<Boolean>> opinions) {
		// Create the set of trusts
		List<Trust> trusts = new ArrayList<>();
		for (Opinion<Boolean> opinion : opinions) {
			double trust = opinion.getTrust();
			if (trust < 0 || trust > 1)
				throw new Error("Invalid trust value, ProbabalisticAggregator expects trust in range [0, 1]");

			if (opinion.getValue())
				trusts.add(new Trust(trust, 0));
			else
				trusts.add(new Trust(0, trust));
		}
		
		// Merge the trusts
		Trust merged = mergeTrusts(trusts);
		
		// Create a report
		return new Result<Boolean>(merged.b > merged.d, merged.b);
	}
	
	/**
	 * Merges the given trusts into a single trust. This is done by converting them into evidence space,
	 * summing the evidences, and then converting them back into trust space again.
	 * @param trusts The set of trusts
	 * @return The merged trust
	 */
	private Trust mergeTrusts(List<Trust> trusts) {
		Evidence mergedEvidence = new Evidence(0, 0);
		for (Trust trust : trusts) {
			Evidence asEvidence = new Evidence(trust);
			mergedEvidence.r += asEvidence.r;
			mergedEvidence.s += asEvidence.s;
		}

		return new Trust(mergedEvidence);
	}
	
	/**
	 * Math.pow runs much faster on integers
	 * @param d
	 * @param e
	 * @return
	 */
	static double intPow(double d, double e) {
		return Math.pow(d, e);
	}
	
	/**
	 * An implementation of the Confidence function from the paper (Wang et. al. 2010)
	 * @param r The positive evidence
	 * @param s The negative evidence
	 * @return The confidence (1 - uncertainty)
	 */
	static double confidence(final double r, final double s) {
		try {
			TrapezoidIntegrator integrator = new TrapezoidIntegrator();

			UnivariateRealFunction p = new UnivariateRealFunction() {

				@Override
				public double value(double x) throws FunctionEvaluationException {
					return intPow(x, r) * intPow(1 - x, s);
				}
				
			};

			final double intp = integrator.integrate(p, 0, 1);
			System.out.println("intp done: " + intp);
			
			// If our initial integration returns a value of 0, we can't compute anything
			// more. We assume that there is sufficient evidence that the answer is certain
			if (intp == 0)
				return 1;
			
			UnivariateRealFunction q = new UnivariateRealFunction() {

				@Override
				public double value(double x) throws FunctionEvaluationException {
					return Math.abs((intPow(x, r) * intPow(1 - x, s)) / intp - 1);
				}
				
			};
			
			System.out.println("here");
			
			double intq = integrator.integrate(q, 0, 1) / 2;
			System.out.println("intq done: " + intq);
			return intq;
		} catch (MaxIterationsExceededException | FunctionEvaluationException
				| IllegalArgumentException e) {
			// This shouldn't happen, and I don't feel like declaring the throws right now.
			throw new Error(e);
		}
	}
	
	class Trust {
		
		double b, d, u;
		
		public Trust(double b, double d) {
			this(b, d, 1-b-d);
		}

		public Trust(double b, double d, double u) {
			this.b = b;
			this.d = d;
			this.u = u;
		}
		
		/**
		 * Convert a set of values from the Evidence space into the Trust space
		 * @param evidence The value in the evidence space
		 */
		public Trust(Evidence evidence) {
			double c = confidence(evidence.r, evidence.s);
			double alpha = evidence.r / (evidence.r + evidence.s);
			this.b = alpha * c;
			this.d = (1 - alpha) * c;
			this.u = 1 - c;
		}

	}
	
	class Evidence {
		
		double r, s;
		
		public Evidence(double r, double s) {
			this.r = r;
			this.s = s;
		}
		
		/**
		 * Convert a set of values from the Trust space into the Evidence space
		 * @param trust The value in the trust space
		 */
		public Evidence(Trust trust) {
			System.out.println("here");
			double alpha = trust.b / (trust.b + trust.d);
			double conf = 1 - trust.u;
			double t1 = 0;
			double t2 = 2000; // We need to choose this...

			int i=0;
			while (t2 - t1 >= epsilon) {
				System.out.println(i);
				i++;
				double t = (t1 + t2) /2;
				r = alpha * t;
				s = t - r;

				if (confidence(r, s) < conf)
					t1 = t;
				else
					t2 = t;
			}
			System.out.println("there");
		}

	}

}
