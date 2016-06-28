package edu.toronto.cs.se.ci.data;

/**
 * The aggregated result of a CI.
 * 
 * @author Michael Layzell
 *
 * @param <FO>
 */
public final class Result<FO, Q> {
	
	private final FO value;
	private final Q quality;

	/**
	 * Create an immutable Result object
	 * @param value The result's value
	 * @param quality The result's quality
	 */
	public Result(FO value, Q quality) {
		this.value = value;
		this.quality = quality;
	}

	/**
	 * @return The result's value
	 */
	public FO getValue() {
		return value;
	}
	
	/**
	 * @return The result's quality
	 */
	public Q getQuality() {
		return quality;
	}
	
	@Override
	public String toString() {
		if (value != null)
			return "Result " + value.toString() + " " + quality.toString();
		else
			return "UnknownResult";
	}

}
