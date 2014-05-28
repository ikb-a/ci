package edu.toronto.cs.se.ci;

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
	 * Gets the trust in the source. This can vary based on the response which
	 * the source has provided.
	 * 
	 * @param response The response which the source provided (the value returned by getResponse)
	 * @param args The arguments passed to the source
	 * @return A double representing the trust in the response
	 */
	public abstract double getTrust(T response, F input);
	
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
		double trust = getTrust(response, input);
		
		return new Opinion<T>(response, trust);
	}

}
