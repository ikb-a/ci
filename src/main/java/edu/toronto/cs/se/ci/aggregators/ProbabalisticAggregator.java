package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.data.Evidence;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.data.Trust;

/**
 * The ProbabalisticAggregator uses ideas from the paper
 * "Evidence-Based Trust: A Mathematical Model Geared for Multiagent Systems" to
 * determine which result to choose, and the quality of the result.
 * 
 * @author Michael Layzell
 *
 */
public class ProbabalisticAggregator<O> implements Aggregator<O, Trust, Double> {

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

	public Evidence addEvidence(Map<O, Evidence> options, O answer, Evidence evidence, Evidence memo) {
		// The counter-evidence is the evidence which, because of the evidence
		// for k, will be acting
		// "for" every other option. This depends on the number of options which
		// are avaliable.
		// If nOptions == -1, nOptions is assumed to be infinity.
		Evidence counter;
		if (nOptions == -1)
			counter = new Evidence(0, evidence.getConsenting());
		else
			counter = new Evidence(evidence.getDissenting() / (nOptions + 1), evidence.getConsenting());

		// Record the evidence for k
		options.put(answer, combine(options.getOrDefault(answer, memo), evidence));

		// Record the evidence for everything else
		for (Map.Entry<O, Evidence> option : options.entrySet()) {
			if (option.getKey() != answer)
				options.put(option.getKey(), combine(option.getValue(), counter));
		}

		return combine(memo, counter);
	}

	@Override
	public Optional<Result<O, Double>> aggregate(List<Opinion<O, Trust>> opinions) {
		// Add the evidence from every source
		Map<O, Evidence> options = new HashMap<>();
		Evidence memo = new Evidence(0, 0);
		for (Opinion<O, Trust> opinion : opinions) {
			memo = addEvidence(options, opinion.getValue(), new Evidence(opinion.getTrust()), memo);
		}

		// Convert each evidence into trust space
		Map<O, Trust> optionTrusts = new HashMap<>();
		for (Map.Entry<O, Evidence> option : options.entrySet()) {
			optionTrusts.put(option.getKey(), new Trust(option.getValue()));
		}

		// And choose the best one
		double bestBelief = 0;
		O bestOption = null;
		for (Map.Entry<O, Trust> option : optionTrusts.entrySet()) {
			if (option.getValue().getBelief() > bestBelief) {
				bestBelief = option.getValue().getBelief();
				bestOption = option.getKey();
			}
		}

		if (bestOption == null)
			return Optional.absent();

		return Optional.of(new Result<O, Double>(bestOption, bestBelief));
	}

}
