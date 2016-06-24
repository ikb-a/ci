package edu.toronto.cs.se.ci.aggregators;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Aggregator;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.data.Trust;

public class ProbBeliefAggregator<O> implements Aggregator<O, Double, Double> {
	
	ProbabalisticAggregator<O> inner = new ProbabalisticAggregator<O>();
	
	@Override
	public Optional<Result<O, Double>> aggregate(List<Opinion<O, Double>> opinions) {
		List<Opinion<O, Trust>> newOpinions = new ArrayList<>(opinions.size());
		
		for (Opinion<O, Double> opinion : opinions) {
			newOpinions.add(new Opinion<>(opinion.getValue(), new Trust(opinion.getTrust()), "Prob-Belief-Aggregator"));
		}

		return inner.aggregate(newOpinions);
	}

}
