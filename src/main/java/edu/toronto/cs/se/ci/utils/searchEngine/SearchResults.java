package edu.toronto.cs.se.ci.utils.searchEngine;

import java.util.ArrayList;
import java.util.List;


/**
 * A class for holding a list of search results
 * @author wginsberg
 *
 */
public class SearchResults extends ArrayList<SearchResult>{

	
	public int hits;

	public SearchResults(int hits, List<SearchResult> results){
		super(results);
		this.hits = hits;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
}
