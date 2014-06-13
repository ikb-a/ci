package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.Map;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.data.Evidence;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.data.Trust;

/**
 * The ProbabalisticAggregator uses ideas from the paper 
 * "Evidence-Based Trust: A Mathematical Model Geared for Multiagent Systems"
 * to determine which result to choose, and the quality of the result.
 * 
 * @author Michael Layzell
 *
 */
public class ProbabalisticAggregator<T> implements Aggregator<T> {
	
	private int nOptions;
	
	public ProbabalisticAggregator() {
		this(-1);
	}

	public ProbabalisticAggregator(int nOptions) {
		this.nOptions = nOptions;
	}

	public Evidence combine(Evidence a, Evidence b) {
		return new Evidence(a.getConsenting() + b.getConsenting(), a.getDissenting() + b.getDissenting());
	}

	public Evidence addEvidence(Map<T, Evidence> options, T answer, Evidence evidence, Evidence memo) {
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
		for (Map.Entry<T, Evidence> option : options.entrySet()) {
			if (option.getKey() != answer)
				options.put(option.getKey(), combine(option.getValue(), counter));
		}
		
		return combine(memo, counter);
	}
	
	@Override
	public Result<T> aggregate(Iterable<Opinion<T>> opinions) {
		// Add the evidence from every source
		Map<T, Evidence> options = new HashMap<>();
		Evidence memo = new Evidence(0, 0);
		for (Opinion<T> opinion : opinions) {
			memo = addEvidence(options, opinion.getValue(), new Evidence(opinion.getTrust()), memo);
		}

		// Convert each evidence into trust space
		Map<T, Trust> optionTrusts = new HashMap<>();
		for (Map.Entry<T, Evidence> option : options.entrySet()) {
			optionTrusts.put(option.getKey(), new Trust(option.getValue()));
		}
		
		// And choose the best one
		double bestBelief = 0;
		T bestOption = null;
		for (Map.Entry<T, Trust> option : optionTrusts.entrySet()) {
			if (option.getValue().getBelief() > bestBelief) {
				bestBelief = option.getValue().getBelief();
				bestOption = option.getKey();
			}
		}
		
		return new Result<T>(bestOption, bestBelief);
	}
	
}
