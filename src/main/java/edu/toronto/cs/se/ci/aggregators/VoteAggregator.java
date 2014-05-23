package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.Map;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

/**
 * This {@link Aggregator} aggregates by counting each opinion as a vote as to
 * what the correct answer is. The value of the result is the opinion with
 * the most votes. Each vote is weighted as the opinion's trust. The quality
 * of the result is the ratio consenting_votes/all_votes.
 * 
 * <p>Opinion values are compared using a HashMap.
 * 
 * @author layzellm
 *
 * @param <T>
 */
public class VoteAggregator<T> implements Aggregator<T> {

	@Override
	public Result<T> aggregate(Iterable<Opinion<T>> opinions) {
		// Map from each value to its current aggregate weight
		Map<T, Double> options = new HashMap<T, Double>();
		
		// The total trust in all sources in the system
		double totalVotes = 0.0;
		
		for (Opinion<T> opinion : opinions) {
			// Get values from the opinion
			T value = opinion.getValue();
			double trust = opinion.getTrust();

			// Get the current vote count
			Double votes = options.get(value);
			if (votes == null)
				votes = 0.0;

			// Record the opinion's vote
			votes += trust;
			totalVotes += trust;
			
			// Store the new vote back in the Map
			options.put(value, votes);
		}
		
		T bestValue = null;
		double bestWeight = 0.0;

		// Choose the entry with the highest weight
		for (Map.Entry<T, Double> e : options.entrySet()) {
			if (e.getValue() > bestWeight)
				bestValue = e.getKey();
		}
		
		// Return the result
		return new Result<T>(bestValue, bestWeight / totalVotes);
	}

}
