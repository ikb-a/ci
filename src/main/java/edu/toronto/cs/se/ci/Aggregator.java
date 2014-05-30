package edu.toronto.cs.se.ci;

public interface Aggregator<T> {

	/**
	 * @param opinions The opinions provided by sources in the CI
	 * @return An aggregated result
	 */
	public Result<T> aggregate(Iterable<Opinion<T>> opinions);

}
