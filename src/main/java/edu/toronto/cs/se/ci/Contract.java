package edu.toronto.cs.se.ci;

import java.util.List;

public interface Contract<F, T> {
	
	public List<Source<F, T>> provide();

}
