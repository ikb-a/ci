package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;

/**
 * This {@link Aggregator} aggregates by counting each opinion as a vote as to
 * what the correct answer is. The value of the result is the opinion with
 * the most votes. Each vote is weighted as the opinion's trust. 
 * 
 * <p>Opinion values are compared using a HashMap.
 * 
 * @author Michael Layzell
 *
 * @param <O>
 */
public class VoteAggregator<O> implements Aggregator<O, Double, Double> {

	@Override
	public Optional<Result<O, Double>> aggregate(List<Opinion<O, Double>> opinions) {
		// Map from each value to its current aggregate weight
		Map<O, Double> options = new HashMap<O, Double>();
		
		// The total trust in all sources in the system
		double totalVotes = 0.0;
		
		for (Opinion<O, Double> opinion : opinions) {
			// Get values from the opinion
			O value = opinion.getValue();
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
		
		O bestValue = null;
		double bestWeight = 0.0;
		
		// Choose the entry with the highest weight
		for (Map.Entry<O, Double> e : options.entrySet()) {
			if (e.getValue() > bestWeight) {
				bestValue = e.getKey();
				bestWeight = e.getValue();
			}
		}
		
		double quality = getQuality(bestWeight, totalVotes - bestWeight);
		
		// Return the result
		return Optional.of(new Result<O, Double>(bestValue, quality));
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
