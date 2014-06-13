package edu.toronto.cs.se.ci;

import com.google.common.base.Optional;

/**
 * A source object which splits the acts of getting a response ({@code getResponse})
 * from the act of getting the trust ({@code getTrust}).
 * 
 * @author Michael Layzell
 *
 * @param <F> Input type (from)
 * @param <T> Output type (to)
 */
public abstract class AbstractSource<F, T> implements Source<F, T> {
	
	/**
	 * Queries the source, getting its response.
	 * 
	 * @param args The arguments passed to the source
	 * @return The value of the source's opinion.
	 */
	public abstract T getResponse(F input) throws UnknownException;
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
	 */
	@Override
	public Opinion<T> getOpinion(F input) throws UnknownException {
		T response = getResponse(input);
		double trust = getTrust(input, Optional.of(response));
		
		return new Opinion<T>(response, trust);
	}

}
