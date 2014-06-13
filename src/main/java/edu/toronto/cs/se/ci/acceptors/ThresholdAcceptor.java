package edu.toronto.cs.se.ci.acceptors;

import edu.toronto.cs.se.ci.Acceptor;
import edu.toronto.cs.se.ci.data.Result;

public class ThresholdAcceptor<T> implements Acceptor<T> {
	
	private double threshold;

	public ThresholdAcceptor(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public boolean isAcceptable(Result<T> result) {
		return result.getQuality() > threshold;
	}

}
