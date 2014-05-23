package edu.toronto.cs.se.ci;

import java.util.concurrent.Future;

public interface Estimate<T> extends Future<Result<T>> {
	
	/**
	 * Gets the current estimate. Will not block.
	 * @return the current best estimate
	 */
	public Result<T> getCurrent();

}
