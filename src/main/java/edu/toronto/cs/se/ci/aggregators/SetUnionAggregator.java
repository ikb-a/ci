package edu.toronto.cs.se.ci.aggregators;

import java.util.HashSet;
import java.util.Set;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

public class SetUnionAggregator<T> implements Aggregator<Set<T>> {

	@Override
	public Result<Set<T>> aggregate(Iterable<Opinion<Set<T>>> opinions) {
		int intersectSize = 0;
		int totalSize = 0;
		Set<T> result = new HashSet<T>();
		
		for (Opinion<Set<T>> opinion : opinions) {
			Set<T> value = opinion.getValue();

			// Getting the intersection
			Set<T> intersection = new HashSet<T>(result);
			intersection.retainAll(value);

			// Values for determining quality
			intersectSize += intersection.size();
			totalSize += value.size();

			// Add to the result set
			result.addAll(value);
		}
		
		return new Result<Set<T>>(result, ((double) intersectSize)/totalSize);
	}

}
