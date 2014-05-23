package edu.toronto.cs.se.ci;

public interface Selector<F, T> {
	
	public Source<F, T> getNextSource(CI<F, T>.Invocation invocation);

}
