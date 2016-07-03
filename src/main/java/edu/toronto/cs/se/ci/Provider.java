package edu.toronto.cs.se.ci;

import java.util.Collection;

/**
 * Implementing this interface signifies that the implementing class has a
 * method {@link #provide()}, which provides a collection of {@link Source}
 * {@code <I, O, T>}.
 * 
 * @author ikba
 *
 * @param <I>
 *            The input type of the source(s) provided
 * @param <O>
 *            The output type of the source(s) provided
 * @param <T>
 *            The trust type of the source(s) provided
 */
public interface Provider<I, O, T> {
	/**
	 * Provides a collection of sources of the type {@code <I, O, T>}.
	 */
	public Collection<Source<I, O, T>> provide();
}
