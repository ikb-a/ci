package edu.toronto.cs.se.ci;

public interface Acceptor<F, T> {
	
	/**
	 * @param result The aggregated result of a CI
	 * @param invocation The CI's current invocation
	 * @return Whether the result is acceptable
	 */
	public boolean isAcceptable(Result<T> result, CI<F, T>.Invocation invocation);

}
