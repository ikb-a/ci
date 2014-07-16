package edu.toronto.cs.se.ci.data;

/**
 * The aggregated result of a CI.
 * 
 * @author Michael Layzell
 *
 * @param <O>
 */
public final class Result<O, Q> {
	
	private final O value;
	private final Q quality;

	/**
	 * Create an immutable Result object
	 * @param value The result's value
	 * @param quality The result's quality
	 */
	public Result(O value, Q quality) {
		this.value = value;
		this.quality = quality;
	}

	/**
	 * @return The result's value
	 */
	public O getValue() {
		return value;
	}
	
	/**
	 * @return The result's quality
	 */
	public Q getQuality() {
		return quality;
	}

}
