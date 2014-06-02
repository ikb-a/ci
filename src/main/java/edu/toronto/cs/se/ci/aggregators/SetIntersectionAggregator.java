package edu.toronto.cs.se.ci.aggregators;

import java.util.HashSet;
import java.util.Set;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;

public class SetIntersectionAggregator<T> implements Aggregator<Set<T>> {

	@Override
	public Result<Set<T>> aggregate(Iterable<Opinion<Set<T>>> opinions) {
		Set<T> intersection = new HashSet<T>();
		Set<T> union = new HashSet<T>();
		
		for (Opinion<Set<T>> opinion : opinions) {
			Set<T> value = opinion.getValue();
			
			union.addAll(value);
			intersection.retainAll(value);
		}
		
		return new Result<Set<T>>(intersection, ((double) intersection.size())/union.size());
	}

}
