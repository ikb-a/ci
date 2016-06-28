package edu.toronto.cs.se.ci.utils.searchEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding a list of search results
 * 
 * @author wginsberg
 * @author Ian Berlot-Attwell
 *
 */
public class SearchResults extends ArrayList<SearchResult> {

	/**
	 * The total number of hits for this search.
	 */
	private final int hits;
	/**
	 * The page of results represented by this object.
	 */
	private final int pageNumber;
	/**
	 * The query that produced these results.
	 */
	private final String query;

	/**
	 * Create a new SearchResults object.
	 * 
	 * @param hits
	 *            The total number of hits for this {@code query}.
	 * @param results
	 *            The {@link SearchResult}s produced for this {@code query} and
	 *            this {@code page}.
	 * @param query
	 *            The query used to create these results.
	 * @param pageNumber
	 *            The page of results that this object corresponds to.
	 */
	public SearchResults(int hits, List<SearchResult> results, String query, int pageNumber) {
		super(results);
		this.hits = hits;
		this.query = query;
		this.pageNumber = pageNumber;
	}

	@Override
	public String toString() {
		return "Search Results: Hits:" + hits + "Query: \"" + query + "\" Page Number: " + pageNumber + "Results:"
				+ super.toString();
	}

	/**
	 * Checks if this SearchResults and {@code obj} are equal. They are equal
	 * only if {@code obj} is a {@link SearchResults} with the same value
	 * returned for {@link #getQuery()}, the same size, which contain equal
	 * SearchResult objects (where eqaulity of search result objects is defined
	 * by {@link SearchResult #equals(Object)}).
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SearchResults)) {
			return false;
		}
		SearchResults o = (SearchResults) obj;
		if (this.size() != o.size() || !o.getQuery().equals(this.getQuery())) {
			return false;
		}
		for (int x = 0; x < this.size(); x++) {
			if (!this.get(x).equals(o.get(x))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the query that created these results.
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Returns the total number of hits (not just on this page) for the query
	 * that created these results.
	 */
	public int getHits() {
		return hits;
	}

	/**
	 * Returns the page number that these results correspond to.
	 */
	public int getPageNumber() {
		return pageNumber;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
