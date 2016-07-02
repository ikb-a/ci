package edu.toronto.cs.se.ci.machineLearning.util;

import edu.toronto.cs.se.ci.Acceptability;
import edu.toronto.cs.se.ci.Acceptor;
import edu.toronto.cs.se.ci.data.Result;

public class MLNominalThresholdAcceptor<FO> implements Acceptor<FO, double[]> {
	private final double ok;
	private final double good;

	public MLNominalThresholdAcceptor(double ok, double good) {
		this.ok = ok;
		this.good = good;
	}

	@Override
	public Acceptability isAcceptable(Result<FO, double[]> result) {
		double highest = 0;
		double[] quality = result.getQuality();
		for (double prob : quality) {
			if (prob > highest) {
				highest = prob;
			}
		}
		if (highest >= good) {
			return Acceptability.GOOD;
		} else if (highest >= ok) {
			return Acceptability.OK;
		} else {
			return Acceptability.BAD;
		}
	}

}
