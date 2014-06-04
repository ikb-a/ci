package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.Map;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;
import edu.toronto.cs.se.ebt.Evidence;
import edu.toronto.cs.se.ebt.Trust;

public class VoteProbAggregator<T> implements Aggregator<T> {

	@Override
	public Result<T> aggregate(Iterable<Opinion<T>> opinions) {
		Map<T, Double> options = new HashMap<>();
		double total = 0;
		
		for (Opinion<T> opinion : opinions) {
			T value = opinion.getValue();
			double trust = opinion.getTrust();
			options.put(value, options.getOrDefault(value, 0.0) + trust);
			total += trust;
		}
		
		// Choose the best one
		T bestOption = null;
		double bestTrust = 0;
		
		for (Map.Entry<T, Double> entry : options.entrySet()) {
			if (entry.getValue() > bestTrust) {
				bestOption = entry.getKey();
				bestTrust = entry.getValue();
			}
		}
		
		// Generate a confidence level
		double conf = new Trust(new Evidence(bestTrust, total - bestTrust)).getBelief();
				
		return new Result<T>(bestOption, conf);
	}

}
