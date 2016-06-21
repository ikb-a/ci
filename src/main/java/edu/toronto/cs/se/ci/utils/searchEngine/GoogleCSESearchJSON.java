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

public class GoogleCSESearchJSON implements JSONSearchEngine {
	private String rawResults;
	private int nextPageNumber;
	private String currentSearch;
	private static final int pageSize = 10;
	private static final String prefix = "https://www.googleapis.com/customsearch/v1?";
	private final String APP_ID;
	private final String API_KEY;
	private static final String RESULT_TITLE_KEY = "title";
	private static final String RESULT_SNIPPET_KEY = "snippet";
	private static final String RESULT_LINK_KEY = "link";

	public GoogleCSESearchJSON() {
		APP_ID = System.getenv("GOOGLE_CSE_ID");
		API_KEY = System.getenv("GOOGLE_API_KEY");
	}

	@Override
	public SearchResults search(String searchString) throws IOException {
		nextPageNumber = 2;
		currentSearch = searchString;
		return search(searchString, 1);
	}

	@Override
	public String getRawResults() {
		if (rawResults == null) {
			throw new IllegalStateException();
		}
		return rawResults;
	}

	@Override
	public SearchResults nextPage() throws IOException {
		return search(currentSearch, nextPageNumber);
	}

	@Override
	public SearchResults search(String searchString, int pageNumber) throws IOException {
		nextPageNumber = pageNumber + 1;
		currentSearch = searchString;
		URL url = new URL(formatSearch(searchString, caclulateStartIndex(pageNumber)));
		rawResults = readURL(url);
		JSONObject json = new JSONObject(rawResults);
		return convertJSONToSearchResults(json);
	}

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

	private int caclulateStartIndex(int pageNumber) {
		return (pageNumber - 1) * pageSize + 1;
	}

	private SearchResults convertJSONToSearchResults(JSONObject json) {
		// Get the number of hits. If we can't get this number then there was
		// not a successful search
		int hits;
		try {
			hits = json.getJSONObject("queries").getJSONArray("request").getJSONObject(0).getInt("totalResults");
		} catch (JSONException e) {
			throw new RuntimeException(e);// TODO: Make this less terrible
		}

		// get the results
		if (json.has("items")) {

			JSONArray items = json.getJSONArray("items");
			List<SearchResult> results = new ArrayList<SearchResult>(items.length());

			for (int i = 0; i < items.length(); i++) {
				try {
					JSONObject item = items.getJSONObject(i);
					SearchResult result = new SearchResult();
					result.setTitle(item.getString(RESULT_TITLE_KEY));
					result.setSnippet(item.getString(RESULT_SNIPPET_KEY));
					result.setLink(item.getString(RESULT_LINK_KEY));
					results.add(result);
				} catch (JSONException e) {
					continue;
				}
			}
			return new SearchResults(hits, results);
		} else {
			return new SearchResults(hits, new ArrayList<SearchResult>(0));
		}
	}
}
