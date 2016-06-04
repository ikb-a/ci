package edu.toronto.cs.se.ci.data;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Source;

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
	 * The Class of the source that created this Opinion
	 */
	private final Source<?, ?, ?> source;
	/**
	 * The arguments given to the Source that created this Opinion
	 */
	private final Optional<Object> args;

	/**
	 * Create an Opinion object
	 * 
	 * @param value
	 * @param trust
	 */
	public Opinion(O value, T trust) {
		this.value = value;
		this.trust = trust;
		source = null;
		args = Optional.absent();
	}

	public Opinion(Object args, O value, T trust, Source<?, ?, ?> source) {
		this.value = value;
		this.trust = trust;
		this.args = Optional.of(args);
		this.source = source;
	}

	public Source<?, ?, ?> getSource() {
		return source;
	}

	public O getValue() {
		return value;
	}

	public T getTrust() {
		return trust;
	}

}
