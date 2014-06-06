package edu.toronto.cs.se.ci.aggregators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private int nOptions;
	private double epsilon;
	private double tMax;
	
	public ProbabalisticAggregator(int nOptions, double epsilon, double tMax) {
		this.nOptions = nOptions;
		this.epsilon = epsilon;
		this.tMax = tMax;
	}

	public Evidence combine(Evidence a, Evidence b) {
		return new Evidence(a.getConsenting() + b.getConsenting(), a.getDissenting() + b.getDissenting());
	}

	public Evidence addEvidence(Map<Boolean, Evidence> options, Boolean answer, Evidence evidence, Evidence memo) {
		// The counter-evidence is the evidence which, because of the evidence for k, will be acting
		// "for" every other option. This depends on the number of options which are avaliable.
		// If nOptions == -1, nOptions is assumed to be infinity.
		Evidence counter;
		if (nOptions == -1)
			counter = new Evidence(0, evidence.getConsenting());
		else
			counter = new Evidence(evidence.getDissenting() / (nOptions + 1), evidence.getConsenting());
		
		// Record the evidence for k
		options.put(answer, combine(options.getOrDefault(answer, memo), evidence));
		
		// Record the evidence for everything else
		for (Map.Entry<Boolean, Evidence> option : options.entrySet()) {
			if (option.getKey() != answer)
				options.put(option.getKey(), combine(option.getValue(), counter));
		}
		
		return combine(memo, counter);
	}
	
	@Override
	public Result<Boolean> aggregate(Iterable<Opinion<Boolean>> opinions) {
		// Add the evidence from every source
		Map<Boolean, Evidence> options = new HashMap<>();
		Evidence memo = new Evidence(0, 0);
		for (Opinion<Boolean> opinion : opinions) {
			memo = addEvidence(options, opinion.getValue(), new Evidence(new Trust(opinion.getTrust(), 0)), memo);
		}

		// Convert each evidence into trust space
		Map<Boolean, Trust> optionTrusts = new HashMap<>();
		for (Map.Entry<Boolean, Evidence> option : options.entrySet()) {
			optionTrusts.put(option.getKey(), new Trust(option.getValue()));
		}
		
		// And choose the best one
		double bestBelief = 0;
		Boolean bestOption = null;
		for (Map.Entry<Boolean, Trust> option : optionTrusts.entrySet()) {
			if (option.getValue().getBelief() > bestBelief) {
				bestBelief = option.getValue().getBelief();
				bestOption = option.getKey();
			}
		}
		
		return new Result<Boolean>(bestOption, bestBelief);
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
