package edu.toronto.cs.se.ci.aggregators;

import java.util.ArrayList;
import java.util.List;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;
import edu.toronto.cs.se.ebt.Evidence;
import edu.toronto.cs.se.ebt.Trust;

/**
 * The ProbabalisticAggregator uses ideas from the paper 
 * "Evidence-Based Trust: A Mathematical Model Geared for Multiagent Systems"
 * to determine which result to choose, and the quality of the result.
 * 
 * @author Michael Layzell
 *
 */
public class ProbabalisticAggregator implements Aggregator<Boolean> {
	
	private double epsilon;
	private double tMax;
	
	public ProbabalisticAggregator(double epsilon, double tMax) {
		this.epsilon = epsilon;
		this.tMax = tMax;
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
		boolean result = merged.getBelief() > merged.getDisbelief();
		return new Result<Boolean>(result, result ? merged.getBelief() : merged.getDisbelief());
	}
	
	/**
	 * Merges the given trusts into a single trust. This is done by converting them into evidence space,
	 * summing the evidences, and then converting them back into trust space again.
	 * 
	 * @param trusts The set of trusts
	 * @return The merged trust
	 */
	private Trust mergeTrusts(List<Trust> trusts) {
		double mergedR = 0;
		double mergedS = 0;

		for (Trust trust : trusts) {
			Evidence asEvidence = new Evidence(trust, epsilon, tMax);
			mergedR += asEvidence.getConsenting();
			mergedS += asEvidence.getDissenting();
		}

		return new Trust(new Evidence(mergedR, mergedS));
	}
	
}
