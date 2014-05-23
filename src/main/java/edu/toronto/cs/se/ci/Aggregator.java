package edu.toronto.cs.se.ci;

public interface Aggregator<T> {

	public Result<T> aggregate(Iterable<Opinion<T>> opinions);

}
