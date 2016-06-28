package edu.toronto.cs.se.ci.data;

import edu.toronto.cs.se.ci.Source;

/**
 * A source's opinion.
 * 
 * @author Michael Layzell
 * @author Ian Berlot-Attwell
 *
 * @param <O>
 */
public final class Opinion<O, T> {

	private final T trust;
	private final O value;
	private final String name;
	/**
	 * The Class of the source that created this Opinion
	 */
	private final Source<?, ?, ?> source;

	/**
	 * Create an Opinion object
	 * 
	 * @param value
	 * @param trust
	 */
	public Opinion(O value, T trust, Source<?, ?, ?> source) {
		this.value = value;
		this.trust = trust;
		this.source = source;
		this.name = source.getName();
	}

	/**
	 * Create an Opinion object with a specific name and no source.
	 * 
	 * @param value
	 * @param trust
	 * @param name
	 */
	public Opinion(O value, T trust, String name) {
		this.value = value;
		this.trust = trust;
		this.name = name;
		source = null;
	}

	/**
	 * Create an Opinion object with a specific name and source.
	 * 
	 * @param value
	 * @param trust
	 * @param name
	 * @param source
	 */
	public Opinion(O value, T trust, String name, Source<?, ?, ?> source) {
		this.value = value;
		this.trust = trust;
		this.name = name;
		this.source = source;
	}

	/**
	 * Returns the source which created this opinion, or {@code null} if none is
	 * available.
	 */
	public Source<?, ?, ?> getSource() {
		return source;
	}

	public O getValue() {
		return value;
	}

	public T getTrust() {
		return trust;
	}

	/**
	 * Returns the name of this Opinion. By default, this value is the name of
	 * the source that created the opinion, as determined by
	 * {@link edu.toronto.cs.se.ci.Source #getName()}.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	@Override
	public String toString(){
		return "Name: "+name+" Value: "+value+" Trust: "+trust;
	}

}
