package edu.toronto.cs.se.ci.utils.searchEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding a list of search results
 * 
 * @author wginsberg
 *
 */
public class SearchResults extends ArrayList<SearchResult> {

	public int hits;

	public SearchResults(int hits, List<SearchResult> results) {
		super(results);
		this.hits = hits;
	}

	@Override
	public String toString() {
		return "Search Results: " + hits + " Hits, and these results " + super.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SearchResults)) {
			return false;
		}
		SearchResults o = (SearchResults) obj;
		if (this.size() != o.size()) {
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
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
