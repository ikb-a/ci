package edu.toronto.cs.se.ci.aggregators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

/**
 * The SetVotingAggregator works on the opinions of sources, each of which approximates the
 * target set. Each possible element is given weight based on the trust of the sources which
 * include it in their set. All elements with at least {@code threshold} weight are included
 * in the resulting set.
 * 
 * <p>The quality of the result is the average agreement level of items included in
 * the resulting set.
 * 
 * @author Michael Layzell
 *
 * @param <T> Set element type
 */
public class SetVotingAggregator<T> implements Aggregator<Set<T>> {

	private double threshold;

	/**
	 * @param threshold A value in [0, 1). For a item to be included in the result set,
	 * the ratio of (weighted) sources which include the item in their set over all
	 * sources must be at least {@code threshold}
	 */
	public SetVotingAggregator(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public Result<Set<T>> aggregate(Iterable<Opinion<Set<T>>> opinions) {
		double totalWeight = 0;
		Map<T, Double> votes = new HashMap<T, Double>();
		
		// Each source votes for the items in its set
		for (Opinion<Set<T>> opinion : opinions) {
			double trust = opinion.getTrust();

			for (T item : opinion.getValue()) {
				double weight = votes.getOrDefault(item, 0.0) + trust;
				votes.put(item, weight);
			}
			
			totalWeight += trust;
		}
		
		// Items which have an agreement level above the threshold are added to the set
		Set<T> results = new HashSet<T>();
		double agreementSum = 0;
		
		for (Map.Entry<T, Double> entry : votes.entrySet()) {
			double agreement = entry.getValue() / totalWeight;

			if (agreement > threshold) {
				// Add the entry
				results.add(entry.getKey());
				
				// Record the level of agreement
				agreementSum += agreement;
			}
		}
		
		// The quality is the average agreement level of items in the set
		return new Result<Set<T>>(results, agreementSum / results.size());
	}

}
