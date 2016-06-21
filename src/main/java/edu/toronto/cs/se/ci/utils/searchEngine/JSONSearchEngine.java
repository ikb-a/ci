package edu.toronto.cs.se.ci.utils.searchEngine;

public interface JSONSearchEngine extends GenericSearchEngine {

	/**
	 * Returns JSON search results produced by previously calling search.
	 * <p>
	 * If search was not called previously, then an IllegalStateException is
	 * thrown.
	 * 
	 * @return The raw output of the search engine as a JSON formatted string
	 */
	@Override
	public String getRawResults();
}
