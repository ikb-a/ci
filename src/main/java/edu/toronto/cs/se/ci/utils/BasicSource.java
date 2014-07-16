package edu.toronto.cs.se.ci.utils;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.data.Opinion;

/**
 * A source object which splits the acts of getting a response ({@code getResponse})
 * from the act of getting the trust ({@code getTrust}).
 * 
 * @author Michael Layzell
 *
 * @param <I> Input type (from)
 * @param <O> Output type (to)
 */
public abstract class BasicSource<I, O, T> extends Source<I, O, T> {
	
	/**
	 * Queries the source, getting its response.
	 * 
	 * @param args The arguments passed to the source
	 * @return The value of the source's opinion.
	 */
	public abstract O getResponse(I input) throws UnknownException;
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
	 */
	@Override
	public Opinion<O, T> getOpinion(I input) throws UnknownException {
		O response = getResponse(input);
		T trust = getTrust(input, Optional.of(response));
		
		return new Opinion<O, T>(response, trust);
	}

}
