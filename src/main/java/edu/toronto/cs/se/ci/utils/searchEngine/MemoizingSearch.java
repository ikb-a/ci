package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class is a wrapper for any JSONSearchEngine. It memoizes the results of
 * previous searches, and saves them /reads them to/from a file.
 * 
 * @author Ian Berlot-Attwell
 *
 */
public class MemoizingSearch implements GenericSearchEngine {
	// The search engine being wrapped
	GenericSearchEngine search;
	// The json object containing all previous searches
	JSONObject json;
	// The file containing the json object on the hard drive
	File file;

	// demo
	public static void main(String[] args) throws IOException {
		MemoizingSearch search = new MemoizingSearch("./googleMemoization.json", new GoogleCSESearchJSON());
		System.out.println(search.search("avocado"));
		System.out.println(search.search("pineaple"));
		System.out.println(search.search("grapes"));
	}

	/**
	 * Creates a search engine which returns the same result for previously
	 * searched queries, and otherwise searches for the result using
	 * {@code search}. All previous queries are saved to and read from
	 * {@code path}.
	 * 
	 * @param path
	 *            A valid path to which search results will be saved. If the
	 *            file does not exist, it will be created. If the file contains
	 *            search results from the last time the memoizing search was
	 *            run, then these search results will be used.
	 * @param search
	 *            The search engine to memoize.
	 * @throws IOException
	 */
	public MemoizingSearch(String path, GenericSearchEngine search) throws IOException {
		this.search = search;
		file = new File(path);
		String fileContents = readFile(file);
		if (fileContents.equals("")) {
			this.json = new JSONObject();
		} else {
			if (!fileContents.startsWith("{")) {
				fileContents = fileContents.substring(fileContents.indexOf("{"));
			}
			this.json = new JSONObject(fileContents);
		}
	}

	private String readFile(File f) throws IOException {
		if (!f.exists()) {
			f.createNewFile();
			return "";
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = br.readLine();
		String result = "";
		while (line != null) {
			result += line;
			line = br.readLine();
		}
		br.close();
		return result;
	}

	@Override
	public SearchResults search(String searchString) throws IOException {
		return search(searchString, 1);
	}

	@Override
	public SearchResults search(String searchString, int pageNumber) throws IOException {
		/*
		 * In the file, the JSON's format is as follows: { "searchString":{
		 * "hits":0, "pageNumber":0, "query":"query", "results":[ { "title":
		 * "title", "link": "link", "snippet": "snippet" }, { "title": "title",
		 * "link": "link", "snippet": "snippet" } ] }
		 */

		if (json.has(searchString)) {
			List<SearchResult> listOfResult = new ArrayList<SearchResult>();
			JSONObject jsonSearchResults = json.getJSONObject(searchString);
			JSONArray jsonResults = jsonSearchResults.getJSONArray("results");
			for (int x = 0; x < jsonResults.length(); x++) {
				JSONObject jsonResult = jsonResults.getJSONObject(x);
				listOfResult.add(new SearchResult(jsonResult.getString("title"), jsonResult.getString("link"),
						jsonResult.getString("snippet")));
			}
			return new SearchResults(jsonSearchResults.getInt("hits"), listOfResult,
					jsonSearchResults.getString("query"), jsonSearchResults.getInt("pageNumber"));
		}

		SearchResults results = search.search(searchString, pageNumber);
		JSONArray jsonResults = new JSONArray();
		for (SearchResult result : results) {
			JSONObject jsonResult = new JSONObject();
			jsonResult.put("title", result.getTitle());
			jsonResult.put("link", result.getLink());
			jsonResult.put("snippet", result.getSnippet());
			jsonResults.put(jsonResult);
		}
		JSONObject jsonSearchResults = new JSONObject();
		jsonSearchResults.put("hits", results.getHits());
		jsonSearchResults.put("query", results.getQuery());
		jsonSearchResults.put("pageNumber", results.getPageNumber());
		jsonSearchResults.put("results", jsonResults);

		json.put(searchString, jsonSearchResults);

		try {
			FileWriter fw = new FileWriter(file);
			fw.write(json.toString());
			fw.close();
		} catch (IOException e) {
			System.err.println("unable to save data");
			e.printStackTrace();
		}

		return results;
	}

	@Override
	public SearchResults nextPage(SearchResults previousPage) throws IOException {
		return search(previousPage.getQuery(), previousPage.getPageNumber() + 1);

	}

	@Override
	public int getPageSize() {
		return search.getPageSize();
	}

	@Override
	public String getRawResults() {
		return search.getRawResults();
	}

}
