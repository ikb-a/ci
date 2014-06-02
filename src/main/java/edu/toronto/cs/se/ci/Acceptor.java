package edu.toronto.cs.se.ci;

public interface Acceptor<T> {
	
	/**
	 * @param result The aggregated result of a CI
	 * @return Whether the result is acceptable
	 */
	public boolean isAcceptable(Result<T> result);

}
