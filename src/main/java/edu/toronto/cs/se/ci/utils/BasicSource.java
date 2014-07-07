package edu.toronto.cs.se.ci.utils;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Trust;

/**
 * A source object which splits the acts of getting a response ({@code getResponse})
 * from the act of getting the trust ({@code getTrust}).
 * 
 * @author Michael Layzell
 *
 * @param <A> Input type (from)
 * @param <T> Output type (to)
 */
public abstract class BasicSource<A, T> extends Source<A, T> {
	
	/**
	 * Queries the source, getting its response.
	 * 
	 * @param args The arguments passed to the source
	 * @return The value of the source's opinion.
	 */
	public abstract T getResponse(A input) throws UnknownException;
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
	 */
	@Override
	public Opinion<T> getOpinion(A input) throws UnknownException {
		T response = getResponse(input);
		Trust trust = getTrust(input, Optional.of(response));
		
		return new Opinion<T>(response, trust);
	}

}
