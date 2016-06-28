package edu.toronto.cs.se.ci.utils.searchEngine;

/**
 * An object which holds one search result from an engine such as Google Search.
 * This class is immutable.
 * 
 * @author wginsberg
 * @author Ian Berlot-Attwell
 *
 */
public class SearchResult {

	private final String title;
	private final String link;
	private final String snippet;

	public SearchResult(String title, String link, String snippet) {
		super();
		this.title = title;
		this.link = link;
		this.snippet = snippet;
	}

	/**
	 * Returns the title of this result (the title of this webpage).
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the link to this webpage.
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Returns a snippet of text from the webpage (typically containing some or
	 * all of the words in the query that produced this result).
	 */
	public String getSnippet() {
		return snippet;
	}

	@Override
	public String toString() {
		return String.format("%s\n%s\n%s\n", getTitle(), getSnippet(), getLink());
	}

	/**
	 * An object is equal to this if the object is also a {@link SearchResult},
	 * with the same values for {@link #getTitle()}, {@link #getLink()} and
	 * {@link #getSnippet()}.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SearchResult)) {
			return false;
		}
		SearchResult o = (SearchResult) obj;
		return this.getLink().equals(o.getLink()) && this.getTitle().equals(o.getTitle())
				&& this.getSnippet().equals(o.getSnippet());
	}

}
