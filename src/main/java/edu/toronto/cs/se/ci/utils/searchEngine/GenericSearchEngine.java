package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.IOException;

/**
 * This interface models the behavior of Search Engines, where the type of
 * output that the search engine returns in uncertain (i.e. may be JSON, XML,
 * etc...).
 * 
 * @author Ian Berlot-Attwell
 */
public interface GenericSearchEngine {
	/**
	 * Searches for {@code searchString}, and returns the first page of results
	 * as a {@link SearchResults}.
	 * 
	 * @param searchString
	 *            The String that should be used to search
	 * @throws IOException
	 *             Thrown if there is an IO problem getting the search results
	 * @return A SearchResults object containing the first page of results. If
	 *         there are no search results, the object will be empty.
	 */
	public SearchResults search(String searchString) throws IOException;

	public SearchResults search(String searchString, int pageNumber) throws IOException;

	public SearchResults nextPage() throws IOException;

	/**
	 * Returns the unformatted search results produced by previously calling
	 * search.
	 * <p>
	 * If search was not called previously, then an IllegalStateException is
	 * thrown.
	 * 
	 * @return The raw output of the search engine
	 */
	public String getRawResults();
}
