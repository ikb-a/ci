package edu.toronto.cs.se.ci.machineLearning;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Source;

/**
 * This class is for sources that will only be used with ML Aggregators
 * (although any aggregator which accepts a trust of Void will work). If you
 * wish that a source work with both ML and non-ML aggregators, it is
 * recommended to create a subclass of {@link edu.toronto.cs.se.ci.Source} or of
 * {@link edu.toronto.cs.se.ci.utils.BasicSource}, and to denote it as obeying
 * an MLContract by implementing either {@link MLContract} or a subinterface
 * thereof.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <I>
 *            The input type to the source
 * @param <O>
 *            The output type of the source
 */
public abstract class MLSource<I, O> extends Source<I, O, Void> {
	/**
	 * Machine learning computes the trust of a source based on training data,
	 * so the trust of the source is not needed. For that reason, {@code null}
	 * is returned.
	 */
	@Override
	public Void getTrust(I args, Optional<O> value) {
		return null;
	}
}
