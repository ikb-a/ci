package edu.toronto.cs.se.ci.data;


/**
 * A source's opinion.
 * 
 * @author Michael Layzell
 *
 * @param <T>
 */
public final class Opinion<T> {

	private final Trust trust;
	private final T value;
	
	public Opinion(T value, double belief) {
		this.value = value;
		this.trust = new Trust(belief);
	}

	/**
	 * Create an Opinion object
	 * 
	 * @param value
	 * @param trust
	 */
	public Opinion(T value, Trust trust) {
		this.value = value;
		this.trust = trust;
	}
	
	public T getValue() {
		return value;
	}
	
	public Trust getTrust() {
		return trust;
	}
	
	public double getBelief() {
		return trust.getBelief();
	}

}
