package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.GenericAggregator;
import edu.toronto.cs.se.ci.data.Evidence;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.data.Trust;

/**
 * This {@link GenericAggregator} performs voting to determine the opinion to select,
 * then the quality of the answer is determined by using EBT.
 * 
 * @author Michael Layzell
 *
 * @param <O> The result type
 */
public class VoteProbAggregator<O> implements Aggregator<O, Trust, Double> {

	@Override
	public Optional<Result<O, Double>> aggregate(List<Opinion<O, Trust>> opinions) {
		Map<O, Double> options = new HashMap<>();
		double total = 0;
		
		for (Opinion<O, Trust> opinion : opinions) {
			O value = opinion.getValue();
			double trust = opinion.getTrust().getBelief();
			options.put(value, options.getOrDefault(value, 0.0) + trust);
			total += trust;
		}
		
		// Choose the best one
		O bestOption = null;
		double bestTrust = 0;
		
		for (Map.Entry<O, Double> entry : options.entrySet()) {
			if (entry.getValue() > bestTrust) {
				bestOption = entry.getKey();
				bestTrust = entry.getValue();
			}
		}
		
		// Generate a confidence level
		double conf = new Trust(new Evidence(bestTrust, total - bestTrust)).getBelief();
				
		return Optional.of(new Result<O, Double>(bestOption, conf));
	}

}
