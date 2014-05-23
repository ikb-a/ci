package edu.toronto.cs.se.ci.aggregators;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

/**
 * This {@link Aggregator} aggregates by taking the weighted mean of the opinions.
 * The weighting of each opinion is its trust. The quality of the result is 
 * {@code 1.0/(stdev + 1)} where stdev is the weighted standard deviation.
 * 
 * @author layzellm
 *
 */
public class WeightedMeanAggregator implements Aggregator<Double> {

	@Override
	public Result<Double> aggregate(Iterable<Opinion<Double>> opinions) {
		// Get the weighted mean
		double sum = 0;
		double totalWeight = 0;

		for (Opinion<Double> opinion : opinions) {
			double trust = opinion.getTrust();
			sum += opinion.getValue() * trust;
			totalWeight += trust;
		}

		double mean = sum / totalWeight;
		
		// Get the weighted standard deviation
		double squareDiffSum = 0;
		
		for (Opinion<Double> opinion : opinions) {
			squareDiffSum += Math.pow(opinion.getValue() - mean, 2) * opinion.getTrust();
		}
		
		double stdev = Math.sqrt(squareDiffSum / totalWeight);
		
		// Return the result
		return new Result<Double>(mean, 1.0/(stdev + 1));
	}

}
