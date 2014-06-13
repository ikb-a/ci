package edu.toronto.cs.se.ci.aggregators;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

/**
 * This Aggregator selects the single opinion with the highest trust value.
 * 
 * <p>The quality of the result is this trust value.
 * 
 * @author Michael Layzell
 *
 * @param <T>
 */
public class RankAggregator<T> implements Aggregator<T> {

	@Override
	public Result<T> aggregate(Iterable<Opinion<T>> opinions) {
		double bestTrust = 0;
		Opinion<T> bestOpinion = null;
		
		for (Opinion<T> opinion : opinions) {
			double trust = opinion.getBelief();
			if (trust > bestTrust) {
				bestOpinion = opinion;
				bestTrust = trust;
			}
		}
		
		return new Result<T>(bestOpinion.getValue(), bestTrust);
	}

}
