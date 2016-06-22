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
	 * @return A SearchResults object containing the first page of results. If
	 *         there are no search results, the object will be empty.
	 * @throws IOException
	 *             Thrown if there is an IO problem getting the search results
	 */
	public SearchResults search(String searchString) throws IOException;

	/**
	 * Searches for {@code searchString}, and returns the {@code pageNumber}
	 * page of results as a {@link SearchResults}.
	 * 
	 * @param searchString
	 *            The String that should be used to search
	 * @param pageNumber
	 *            The page number for the results wanted
	 * @return A SearchResults object containing the first page of results. If
	 *         there are no search results, the object will be empty.
	 * @throws IOException
	 *             Thrown if there is an IO problem getting the search results
	 */
	public SearchResults search(String searchString, int pageNumber) throws IOException;

	/**
	 * Returns the next page of results. Note that {@link #search(String)} or
	 * {@link #search(String, int)} must be called before this method, or an
	 * {@link IllegalStateException} will be thrown.
	 * 
	 * @return The next page of results as a {@link SearchResults}
	 * @throws IOException
	 *             Thrown if there is an IO problem getting the search results
	 */
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

	/**
	 * Returns the number of results in one full page of results for this search
	 * engine.
	 */
	public int getPageSize();
}
