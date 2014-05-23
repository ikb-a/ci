package edu.toronto.cs.se.ci;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface Selector<F, T> {
	
	public Source<F, T> getNextSource(List<Source<F, T>> considered, List<Opinion<T>> opinions, List<Source<F, T>> sources, Map<String, Float> budget, F args);

}
