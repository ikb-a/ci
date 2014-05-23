package edu.toronto.cs.se.ci;

public class Result<T> {
	
	private T value;
	private double quality;

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
