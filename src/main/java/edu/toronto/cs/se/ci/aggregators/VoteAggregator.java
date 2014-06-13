package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.Map;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

/**
 * This {@link Aggregator} aggregates by counting each opinion as a vote as to
 * what the correct answer is. The value of the result is the opinion with
 * the most votes. Each vote is weighted as the opinion's trust. 
 * 
 * <p>Opinion values are compared using a HashMap.
 * 
 * @author Michael Layzell
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
			double trust = opinion.getBelief();

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
			if (e.getValue() > bestWeight) {
				bestValue = e.getKey();
				bestWeight = e.getValue();
			}
		}
		
		double quality = getQuality(bestWeight, totalVotes - bestWeight);
		
		// Return the result
		return new Result<T>(bestValue, quality);
	}

	/**
	 * This function was selected as it satisfies a few different conditions, which are
	 * useful for a quality function. These properties are as follows:<br>
	 * 1) When there is no consenting evidence, quality is 0 <br>
	 * 2) As the amount of evidence increases, at a constant ratio {@code consenting / dissenting}, the quality increases <br>
	 * 3) As the amount of consenting evidence increases relative to the amount of dissenting evidence, the quality increases <br>
	 * 4) As the amount of dissenting evidence increases relative to the amount of consenting evidence, the quality decreases <br>
	 * 5) The value is always in [0, 1), for all valid values of {@code consenting} and {@code dissenting}
	 * 
	 * @param consenting The quantity of consenting evidence
	 * @param dissenting The quantity of dissenting evidence
	 * @return The quality of the answer
	 */
	private double getQuality(double consenting, double dissenting) {
		return consenting / (consenting + 2 * dissenting + 1);
	}

}
