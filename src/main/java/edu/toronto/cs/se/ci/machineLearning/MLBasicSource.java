package edu.toronto.cs.se.ci.machineLearning;

import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.data.Opinion;

/**
 * A source object which splits the acts of getting a response ({@code getResponse})
 * from the act of getting the trust ({@code getTrust}). As this source is for ML Aggregators, the
 * trust type is implicitly {@code Void}.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <I> Input type
 * @param <O> Output type
 */
public abstract class MLBasicSource<I, O> extends MLSource<I, O> {

	/**
	 * Queries the source, getting its response.
	 * 
	 * @param args
	 *            The arguments passed to the source
	 * @return The value of the source's opinion.
	 */
	public abstract O getResponse(I input) throws UnknownException;

	/**
	 * Gets the value of the opinion using {@link #getResponse(Object)}, and
	 * returns an {@link edu.toronto.cs.se.ci.data.Opinion} with a trust of
	 * {@code null}.
	 */
	@Override
	public Opinion<O, Void> getOpinion(I input) throws UnknownException {
		O response = getResponse(input);
		return new Opinion<O, Void>(response, null);
	}

}
