package edu.toronto.cs.se.ci;

import java.util.List;

public interface Contract<I, O, T> {
	
	public List<Source<I, O, T>> provide();

}
