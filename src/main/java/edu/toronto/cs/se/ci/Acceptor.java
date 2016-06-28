package edu.toronto.cs.se.ci;

import edu.toronto.cs.se.ci.data.Result;

/**
 * The acceptor is a single-method interface. It is used to determine whether to
 * continue executing sources for a CI, or if the current answer is acceptable.
 * 
 * @author Michael Layzell
 *
 * @param <FO> CI Output Type (Output type of aggregator)
 * @param <Q> CI Quality Type (Quality type of aggregator)
 */
public interface Acceptor<FO, Q> {
	
	/**
	 * @param result The aggregated result of a CI
	 * @return Whether the result is acceptable
	 */
	public Acceptability isAcceptable(Result<FO, Q> result);

}
