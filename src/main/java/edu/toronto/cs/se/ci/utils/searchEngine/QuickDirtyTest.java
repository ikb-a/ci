package edu.toronto.cs.se.ci.utils.searchEngine;

import java.io.IOException;

public class QuickDirtyTest {
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		if(false){
			GoogleCSESearchJSON bob = new GoogleCSESearchJSON();
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
	}

}
