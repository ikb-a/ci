package edu.toronto.cs.se.ci;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;

/**
 * The aggregator is a single-method interface. It is used to combine the opinions
 * returned by sources into a single unified result.
 * 
 * @author Michael Layzell
 *
 * @param <T>
 */
public interface Aggregator<T> {

	/**
	 * @param opinions The opinions provided by sources in the CI
	 * @return An aggregated result
	 */
	public Result<T> aggregate(Iterable<Opinion<T>> opinions);

}
