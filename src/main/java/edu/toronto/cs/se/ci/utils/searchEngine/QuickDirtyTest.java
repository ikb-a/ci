package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.IOException;

/*
 * How to write proper tests?
 */
public class QuickDirtyTest {
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		GoogleCSESearchJSON bob = new GoogleCSESearchJSON();
		BingSearchJSON sal = new BingSearchJSON();

		if (false) {
			System.out.println("SearchResults:");
			System.out.println(bob.search("Dilbert"));
			System.out.println("raw:");
			System.out.println(bob.getRawResults());
			System.out.println("Next:");
			System.out.println(bob.nextPage());
			System.out.println("raw:");
			System.out.println(bob.getRawResults());
			System.out.println("4rth Page:");
			System.out.println(bob.search("Dilbert", 4));
			System.out.println("raw:");
			System.out.println(bob.getRawResults());
			System.out.println("Next:");
			System.out.println(bob.nextPage());
			System.out.println("raw:");
			System.out.println(bob.getRawResults());
		}
		/*
		 * Note that the number of hits works; oddly based on which page of
		 * results is retrieved. For this reason I choose not to compare the
		 * number of hits for the SearchResults equals() method.
		 */
		if (false) {
			System.out.println("SearchResults:");
			System.out.println(sal.search("Lego"));
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
			System.out.println("Next:");
			System.out.println(sal.nextPage());
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
		}
		/*
		 * Note the very bizzaire results on the last of the 4rth page, and the
		 * entire 5th page
		 */
		if (false) {
			System.out.println("4rth Page:");
			System.out.println(sal.search("Lego", 4));
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
			System.out.println("Next:");
			System.out.println(sal.nextPage());
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
		}
		if (false) {
			System.out.println("9th Page:");
			System.out.println(bob.search("simian beta decay and relativistic revitalization wrt ice cream", 9));
			System.out.println("raw:");
			System.out.println(bob.getRawResults());
			System.out.println("Next:");
			System.out.println(bob.nextPage());
			System.out.println("raw:");
			System.out.println(bob.getRawResults());
		}

		if (false) {
			System.out.println(sal.search("Dilbert"));
			System.out.println(sal.getRawResults());
		}
		/*
		 * Presently all these calls return the same results, due to Bing API
		 * being awful
		 */
		if (true) {
			System.out.println("19th Page:");
			System.out.println(sal.search("simian beta decay and relativistic revitalization wrt ice cream", 19));
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
			System.out.println("Next:");
			System.out.println(sal.nextPage());
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
			System.out.println("Next-21rst:");
			System.out.println(sal.nextPage());
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
			System.out.println("Next-22nd:");
			System.out.println(sal.nextPage());
			System.out.println("raw:");
			System.out.println(sal.getRawResults());
		}
	}

}
