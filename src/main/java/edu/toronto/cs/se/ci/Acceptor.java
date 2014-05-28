package edu.toronto.cs.se.ci;

public interface Acceptor<F, T> {
	
	public boolean isAcceptable(Result<T> result, CI<F, T>.Invocation invocation);

}
