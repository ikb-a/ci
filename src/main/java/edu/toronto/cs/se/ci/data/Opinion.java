package edu.toronto.cs.se.ci.data;


/**
 * A source's opinion.
 * 
 * @author Michael Layzell
 *
 * @param <O>
 */
public final class Opinion<O, T> {

	private final T trust;
	private final O value;
	
	/**
	 * Create an Opinion object
	 * 
	 * @param value
	 * @param trust
	 */
	public Opinion(O value, T trust) {
		this.value = value;
		this.trust = trust;
	}
	
	public O getValue() {
		return value;
	}
	
	public T getTrust() {
		return trust;
	}
	
}
