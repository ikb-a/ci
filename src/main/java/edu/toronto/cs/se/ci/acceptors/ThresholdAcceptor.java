package edu.toronto.cs.se.ci.acceptors;

import edu.toronto.cs.se.ci.Acceptability;
import edu.toronto.cs.se.ci.Acceptor;
import edu.toronto.cs.se.ci.data.Result;

/**
 * Accepts a result if its quality exceeds a given threshold
 * 
 * @author Michael Layzell
 *
 * @param <O>
 */
public class ThresholdAcceptor<O> implements Acceptor<O, Double> {
	
	private double ok;
	private double good;

	/**
	 * Create a ThresholdAcceptor with the given thresholds
	 * 
	 * @param ok Required to produce {@link Acceptability.OK}
	 * @param good Required to produce {@link Acceptability.GOOD}
	 */
	public ThresholdAcceptor(double ok, double good) {
		this.ok = ok;
		this.good = good;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Acceptor#isAcceptable(edu.toronto.cs.se.ci.data.Result)
	 */
	@Override
	public Acceptability isAcceptable(Result<O, Double> result) {
		if (result.getQuality() < ok)
			return Acceptability.BAD;
		else if (result.getQuality() > good)
			return Acceptability.GOOD;
		
		return Acceptability.OK;
	}

}
