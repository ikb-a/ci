package edu.toronto.cs.se.ci.data;

/**
 * The aggregated result of a CI.
 * 
 * @author Michael Layzell
 *
 * @param <T>
 */
public final class Result<T> {
	
	private final T value;
	private final double quality;

	/**
	 * Create an immutable Result object
	 * @param value The result's value
	 * @param quality The result's quality
	 */
	public Result(T value, double quality) {
		this.value = value;
		this.quality = quality;
	}

	/**
	 * @return The result's value
	 */
	public T getValue() {
		return value;
	}
	
	/**
	 * @return The result's quality
	 */
	public double getQuality() {
		return quality;
	}

}
