package edu.toronto.cs.se.ci;

/**
 * The acceptor is a single-method interface. It is used to determine whether to
 * continue executing sources for a CI, or if the current answer is acceptable.
 * 
 * @author Michael Layzell
 *
 * @param <T>
 */
public interface Acceptor<T> {
	
	/**
	 * @param result The aggregated result of a CI
	 * @return Whether the result is acceptable
	 */
	public boolean isAcceptable(Result<T> result);

}
