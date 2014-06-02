package edu.toronto.cs.se.ci.aggregators;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

public class RankAggregator<T> implements Aggregator<T> {

	@Override
	public Result<T> aggregate(Iterable<Opinion<T>> opinions) {
		double bestTrust = 0;
		Opinion<T> bestOpinion = null;
		
		for (Opinion<T> opinion : opinions) {
			double trust = opinion.getTrust();
			if (trust > bestTrust) {
				bestOpinion = opinion;
				bestTrust = trust;
			}
		}
		
		return new Result<T>(bestOpinion.getValue(), bestTrust);
	}

}
