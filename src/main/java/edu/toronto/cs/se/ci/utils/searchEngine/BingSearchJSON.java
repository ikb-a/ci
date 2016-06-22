package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BingSearchJSON implements JSONSearchEngine {

	private static final int pageSize = 50;
	/**
	 * The page number of the next page. Valid page numbers are from 1-21
	 * inclusive.
	 */
	private int nextPage;
	/**
	 * The current query (i.e. "e3 2016 games being released")
	 */
	private String currentSearch;

	/**
	 * The Bing Web-Results
	 * (https://datamarket.azure.com/dataset/bing/searchweb) API Key
	 */
	private final String API_KEY;

	/**
	 * The JSON formated String that is returned by the Bing API.
	 */
	private String rawResults;
	/*
	 * When the Bing api runs out of results, it returns the last page of
	 * results. Possibly we could store a map of search word to SearchResults,
	 * but that would be rather memory inefficient.
	 */

	/**
	 * Creates a new {@link BingSearchJSON} object. Note that the environment
	 * variable "BING_KEY" must be set to the Bing Web-Results API Key
	 * (https://datamarket.azure.com/dataset/bing/searchweb).
	 */
	public BingSearchJSON() {
		API_KEY = System.getenv("BING_KEY");
	}

	@Override
	public SearchResults search(String searchString) throws IOException {
		return search(searchString, 1);
	}

	/**
	 * Searches for {@code searchString}, and returns the {@code pageNumber}
	 * page of results as a {@link SearchResults}.
	 * <p>
	 * Note that if a page has less that n pages of results, then a request for
	 * page n will return the last page of results.
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
	@Override
	public SearchResults search(String searchString, int pageNumber) throws IOException {
		if (pageNumber > 21) {
			throw new IllegalArgumentException("Due to limitations in the Bing API, pageNumber cannot exceed 21");
		} else if (pageNumber < 1) {
			throw new IllegalArgumentException("pageNumber must be between 1-21 inclusive.");
		}
		nextPage = pageNumber + 1;
		currentSearch = searchString;

		// Connect to the remote URL
		URL url = new URL("https://api.datamarket.azure.com/Bing/SearchWeb/v1/Composite?Query="
				+ URLEncoder.encode("'" + searchString + "'", "UTF-8") + "&$top=50&$skip=" + calcSkip(pageNumber));
		rawResults = connectToBingAndRead(url);
		SearchResults currResult = convertRawResultsToSearchResults();
		return currResult;
	}

	/**
	 * Reads the rawResults variable, and converts it into a
	 * {@link SearchResults} object.
	 * 
	 * @return The Bing API results converted into a {@link SearchResults}
	 *         object.
	 */
	private SearchResults convertRawResultsToSearchResults() {
		assert (rawResults != null);
		JSONObject json = new JSONObject(rawResults);
		int hits = json.getJSONObject("d").getJSONArray("results").getJSONObject(0).getInt("WebTotal");

		JSONArray jsonArray = json.getJSONObject("d").getJSONArray("results").getJSONObject(0).getJSONArray("Web");
		int numberOfHitsOnThisPage = jsonArray.length();

		List<SearchResult> allResults = new ArrayList<SearchResult>();
		for (int x = 0; x < numberOfHitsOnThisPage; x++) {
			JSONObject resultXJson = jsonArray.getJSONObject(x);
			String title = resultXJson.getString("Title");
			String snippet = resultXJson.getString("Description");
			String url = resultXJson.getString("Url");
			SearchResult resultXSearchResult = new SearchResult(title, url, snippet);
			allResults.add(resultXSearchResult);
		}
		return new SearchResults(hits, allResults);
	}

	/**
	 * Connects to the BING API url, and reads the results as a String.
	 * 
	 * @param url
	 *            Must be properly formatted for a BING REST request.
	 * @return String produced by the BING API
	 * @throws IOException
	 *             If there is any problem connecting to the Bing API
	 */
	private String connectToBingAndRead(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Authorization",
				"Basic " + Base64.getEncoder().encodeToString((API_KEY + ":" + API_KEY).getBytes()));
		conn.setRequestProperty("Accept", "application/json");
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

		// Read in the entire file
		StringBuilder sb = new StringBuilder();
		String line = reader.readLine();

		while (line != null) {
			sb.append(line);
			line = reader.readLine();
		}
		return sb.toString();
	}

	/**
	 * Calculates the skip value required in the BING API rest request, to reach
	 * the {@code pageNumber} page of results.
	 * 
	 * @param pageNumber
	 *            The page number for the results wanted. Should be between 1-21
	 *            inclusive.
	 * @return The value required for the skip to reach that page number.
	 */
	// TODO: Note skip cannot exceed 1000 (that is the Bing API limit)
	private int calcSkip(int pageNumber) {
		return (pageNumber - 1) * pageSize;
	}

	@Override
	public SearchResults nextPage() throws IOException {
		if (currentSearch == null) {
			throw new IllegalStateException("Cannot retrieve next page of results if there is no search.");
		} else if (nextPage >= 22) {
			throw new IllegalStateException("The Bing API does not allow for the retrieval of the 22nd page");
		}
		return search(currentSearch, nextPage);
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	@Override
	public String getRawResults() {
		if (rawResults == null) {
			throw new IllegalStateException();
		}
		return rawResults;
	}

}
