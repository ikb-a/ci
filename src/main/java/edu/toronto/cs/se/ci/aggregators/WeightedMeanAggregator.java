package edu.toronto.cs.se.ci.aggregators;

import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;

/**
 * This {@link Aggregator} aggregates by taking the weighted mean of the opinions.
 * The weighting of each opinion is its trust. The quality of the result is 
 * {@code 1.0/(stdev + 1)} where stdev is the weighted standard deviation.
 * 
 * @author Michael Layzell
 *
 */
public class WeightedMeanAggregator implements Aggregator<Double, Double, Double> {

	@Override
	public Optional<Result<Double, Double>> aggregate(List<Opinion<Double, Double>> opinions) {
		// Get the weighted mean
		double sum = 0;
		double totalWeight = 0;

		for (Opinion<Double, Double> opinion : opinions) {
			double trust = opinion.getTrust();
			sum += opinion.getValue() * trust;
			totalWeight += trust;
		}

		double mean = sum / totalWeight;
		
		// Get the weighted standard deviation
		double squareDiffSum = 0;
		
		for (Opinion<Double, Double> opinion : opinions) {
			squareDiffSum += Math.pow(opinion.getValue() - mean, 2) * opinion.getTrust();
		}
		
		double stdev = Math.sqrt(squareDiffSum / totalWeight);
		
		// Return the result
		return Optional.of(new Result<Double, Double>(mean, 1.0/(stdev + 1)));
	}

}
