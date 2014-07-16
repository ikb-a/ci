package edu.toronto.cs.se.ci.acceptors;

import edu.toronto.cs.se.ci.Acceptor;
import edu.toronto.cs.se.ci.data.Result;

public class ThresholdAcceptor<O> implements Acceptor<O, Double> {
	
	private double threshold;

	public ThresholdAcceptor(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public boolean isAcceptable(Result<O, Double> result) {
		return result.getQuality() > threshold;
	}

}
