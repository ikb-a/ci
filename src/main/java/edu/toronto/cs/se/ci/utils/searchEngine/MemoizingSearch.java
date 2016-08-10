package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a wrapper for any JSONSearchEngine. It memoizes the results of
 * previous searches, and saves them /reads them to/from a file.
 * 
 * @author Ian Berlot-Attwell
 *
 */
public class MemoizingSearch implements GenericSearchEngine {
	/**
	 * The search engine being wrapped
	 */
	GenericSearchEngine search;

	/**
	 * Maps from a String query to the SearchResults produced by searching said
	 * query with {@link #search}.
	 */
	Map<String, SearchResults> savedSearches;

	/**
	 * The file containing the json object on the hard drive
	 */
	String filePath;

	// demo
	public static void main(String[] args) throws IOException {
		MemoizingSearch search = new MemoizingSearch("./googleMemoization.ser", new GoogleCSESearchJSON());
		System.out.println(search.search("avocado"));
		// System.out.println(search.search("pineaple"));
		// System.out.println(search.search("grapes"));
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
		filePath = path;
		try {
			savedSearches = loadMemoizedContents(path);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Attempts to return the serialized hashmap at {@code path}. If the file at
	 * {@code path} does not exist, but the directory does, then the file is
	 * created and an empty hashmap is returned.
	 * 
	 * @param path
	 * @return The hashmap serialized at {@code path}, or an empty hashmap if
	 *         {@code path} did not lead to a file, but a new empty file could
	 *         be created there.
	 * @throws IOException
	 *             A problem occured in reading the file, or in creating the
	 *             non-existant file.
	 * @throws ClassNotFoundException
	 *             A problem occured in the deserialization of the file.
	 */
	private HashMap<String, SearchResults> loadMemoizedContents(String path)
			throws IOException, ClassNotFoundException {
		File f = new File(path);
		if (!f.exists()) {
			f.createNewFile();
			return new HashMap<String, SearchResults>();
		}

		try (FileInputStream fis = new FileInputStream(path)) {
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			HashMap<String, SearchResults> result = (HashMap<String, SearchResults>) ois.readObject();
			ois.close();
			return result;
		} catch (EOFException e) {
			return new HashMap<String, SearchResults>();
		}
	}

	@Override
	public SearchResults search(String searchString) throws IOException {
		return search(searchString, 1);
	}

	@Override
	public SearchResults search(String searchString, int pageNumber) throws IOException {
		String key;
		if (pageNumber != 1) {
			key = searchString+"_PageNumber"+pageNumber;
		}else{
			key = searchString;
		}

		// If this search has been done before, return the previously found
		// result.
		if (savedSearches.containsKey(key)) {
			return savedSearches.get(key);
		}

		// otherwise search for the result and add it to memory
		SearchResults results = search.search(searchString, pageNumber);
		savedSearches.put(key, results);

		// update the file of search queries and responses.
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.savedSearches);
			oos.close();
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
