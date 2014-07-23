package edu.toronto.cs.se.ci.acceptors;

import edu.toronto.cs.se.ci.Acceptability;
import edu.toronto.cs.se.ci.Acceptor;
import edu.toronto.cs.se.ci.data.Result;

public class ThresholdAcceptor<O> implements Acceptor<O, Double> {
	
	private double ok;
	private double good;

	public ThresholdAcceptor(double ok, double good) {
		this.ok = ok;
		this.good = good;
	}

	@Override
	public Acceptability isAcceptable(Result<O, Double> result) {
		if (result.getQuality() < ok)
			return Acceptability.BAD;
		else if (result.getQuality() > good)
			return Acceptability.GOOD;
		
		return Acceptability.OK;
	}

}
