package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class models a Google CSE. Note that due to restrictions in the API,
 * only the first 10 pages of results can be accessed.
 * 
 * @author ikba
 *
 */
public class GoogleCSESearchJSON implements JSONSearchEngine {
	/**
	 * The String returned by the Google CSE API which is a validly formatted
	 * JSON String.
	 */
	private String rawResults;

	/**
	 * The size of one Google CSE page
	 */
	private static final int pageSize = 10;

	/**
	 * The prefix to the Google CSE API
	 */
	private static final String prefix = "https://www.googleapis.com/customsearch/v1?";

	/**
	 * The ID of the CSE
	 */
	private final String APP_ID;

	/**
	 * The google API key for the CSE refered to by {@link #APP_ID}
	 */
	private final String API_KEY;

	/**
	 * The key that maps to the title of an article in the JSON returned by
	 * google.
	 */
	private static final String RESULT_TITLE_KEY = "title";

	/**
	 * The key that maps to the snippet of an article in the JSON returned by
	 * google.
	 */
	private static final String RESULT_SNIPPET_KEY = "snippet";

	/**
	 * The key that maps to the link of an article in the JSON returned by
	 * google.
	 */
	private static final String RESULT_LINK_KEY = "link";

	public GoogleCSESearchJSON() {
		APP_ID = System.getenv("GOOGLE_CSE_ID");
		API_KEY = System.getenv("GOOGLE_API_KEY");
	}

	@Override
	public SearchResults search(String searchString) throws IOException {
		return search(searchString, 1);
	}

	@Override
	public String getRawResults() {
		if (rawResults == null) {
			throw new IllegalStateException();
		}
		return rawResults;
	}

	/**
	 * Returns the next page of results. Note that due to the Google CSE API,
	 * the 11th page cannot be retrieved. Attempting to do so will result in an
	 * {@link IllegalArgumentException}
	 * 
	 * @return The next page of results as a {@link SearchResults}
	 * @throws IOException
	 *             Thrown if there is an IO problem getting the search results
	 * @throws IllegalArgumentException
	 *             Thrown if {@code previousPage} is null, or if the next page
	 *             is the 11th and therefore cannot be retrieved by the Google
	 *             CSE API.
	 */
	@Override
	public SearchResults nextPage(SearchResults previousPage) throws IOException {
		if (previousPage == null) {
			throw new IllegalArgumentException();
		}
		if (previousPage.getPageNumber() >= 10) {
			throw new IllegalArgumentException("The Google CSE API does not allow for the retrieval of the 11th page");
		}
		return search(previousPage.getQuery(), previousPage.getPageNumber() + 1);
	}

	/**
	 * Searches for {@code searchString}, and returns the {@code pageNumber}
	 * page of results as a {@link SearchResults}. Due to limitations in the
	 * Google CSE API, an {@link IllegalArgumentException} will be thrown if
	 * {@code pageNumber} exceeds 10.
	 * 
	 * @param searchString
	 *            The String that should be used to search
	 * @param pageNumber
	 *            The page number for the results wanted. Should be between 1-10
	 *            inclusive.
	 * @return A SearchResults object containing the first page of results. If
	 *         there are no search results, the object will be empty.
	 * @throws IOException
	 *             Thrown if there is an IO problem getting the search results
	 */
	@Override
	public SearchResults search(String searchString, int pageNumber) throws IOException {
		if (pageNumber < 1 || pageNumber > 10) {
			throw new IllegalArgumentException("Due to the Google CSE, pageNumber must be from 1-10 inclusive");
		}
		URL url = new URL(formatSearch(searchString, caclulateStartIndex(pageNumber)));
		rawResults = readURL(url);
		JSONObject json = new JSONObject(rawResults);
		return convertJSONToSearchResults(json, searchString, pageNumber);
	}

	/**
	 * Connects to the URL and reads the page as a String.
	 * 
	 * @param url
	 *            The URL to connect to and read
	 * @return The String contained in the webpage pointed to by {@code url}
	 * @throws IOException
	 *             If there is any problem retrieving/reading the page
	 */
	private String readURL(URL url) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

		// Read in the entire file
		StringBuilder sb = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			sb.append(line);
			line = reader.readLine();
		}
		reader.close();
		return sb.toString();
	}

	/**
	 * Creates the String for the URL for the Google CSE API.
	 * 
	 * @param searchString
	 *            The query to the search engine.
	 * @param startIndex
	 *            The index (from 1-99) of the first web result that should be
	 *            on the page
	 * @return String url that calls the Google CSE API
	 */
	private String formatSearch(String searchString, int startIndex) {
		String parameters;
		if (searchString.contains("site:")) {
			String restrictToSite = searchString.substring(5, searchString.indexOf(" "));
			searchString = searchString.substring(searchString.indexOf(" ") + 1, searchString.length());
			parameters = String.format("q=%s&start=%d&cx=%s&key=%s&siteSearch=%s", searchString.replace(" ", "-"),
					startIndex, APP_ID, API_KEY, restrictToSite);
		} else {
			parameters = String.format("q=%s&start=%d&cx=%s&key=%s", searchString.replace(" ", "-"), startIndex, APP_ID,
					API_KEY);
		}
		return prefix + parameters;
	}

	/**
	 * Returns the index of the first web result on the results page given page
	 * number.
	 */
	private int caclulateStartIndex(int pageNumber) {
		return (pageNumber - 1) * pageSize + 1;
	}

	/**
	 * Converts the {@link JSONObject} produced by the Google CSE API into a
	 * {@link SearchResults}. The {@link SearchResults} contains the total
	 * number of hits, as well as the title, snippet, and URL of a single page's
	 * results. It also contains the query and page number as given to the
	 * method.
	 * 
	 * @param json
	 *            The {@link JSONObject} representing the output of the Google
	 *            CSE API.
	 * @param query
	 *            The query that created {@code json}
	 * @param pageNumber
	 *            The page number for {@code json}
	 * @return A {@link SearchResults} representing the search results contained
	 *         in {@code json}.
	 * @throws RuntimeException
	 *             if the total result of the search cannot be found.
	 */
	private SearchResults convertJSONToSearchResults(JSONObject json, String query, int pageNumber) {
		// Get the number of hits. If we can't get this number then there was
		// not a successful search
		int hits;
		try {
			hits = json.getJSONObject("queries").getJSONArray("request").getJSONObject(0).getInt("totalResults");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		// get the results
		if (json.has("items")) {

			JSONArray items = json.getJSONArray("items");
			List<SearchResult> results = new ArrayList<SearchResult>(items.length());

			for (int i = 0; i < items.length(); i++) {
				try {
					JSONObject item = items.getJSONObject(i);
					SearchResult result = new SearchResult(item.getString(RESULT_TITLE_KEY),
							item.getString(RESULT_LINK_KEY), item.getString(RESULT_SNIPPET_KEY));
					results.add(result);
				} catch (JSONException e) {
					continue;
				}
			}
			return new SearchResults(hits, results, query, pageNumber);
		} else {
			return new SearchResults(hits, new ArrayList<SearchResult>(0), query, pageNumber);
		}
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}
}
