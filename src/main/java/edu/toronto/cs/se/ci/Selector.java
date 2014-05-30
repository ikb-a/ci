package edu.toronto.cs.se.ci;

public interface Selector<F, T> {
	
	/**
	 * Get the next source to be consulted by the CI. This function will be called
	 * repeatedly until it returns {@code null}, at which point, the CI will stop
	 * calling functions. It may block to wait for sources to be consulted.
	 * 
	 * @param invocation The current invocation of the CI
	 * @return The next source to consult, or {@code null}
	 */
	public Source<F, T> getNextSource(CI<F, T>.Invocation invocation);

}
