package edu.toronto.cs.se.ci;

import java.util.Collection;

/**
 * A Contributional Implementation (CI) of a function. Queries a set of sources,
 * and aggregates their opinions to get answers to otherwise unanswerable
 * questions. This CI returns a value of type O, as does it's sources. For This
 * reason it is equivalent to {@link GenericCI<I,O,O,T,Q>}.
 * 
 * @author Michael Layzell
 * @author Ian Berlot-Attwel
 * 
 * @param <I>
 *            The input type to the sources
 * @param <O>
 *            The output type of the sources, and the aggregator (and as a
 *            result, of the CI as well)
 * @param <T>
 *            The trust type returned by the sources
 * @param <Q>
 *            The Quality returned by the aggregator.
 */
public class CI<I, O, T, Q> extends GenericCI<I, O, O, T, Q> {
	/**
	 * Create a CI using source discovery
	 * 
	 * @param contract
	 *            The {@link Contract} to discover sources with
	 * @param agg
	 *            The {@link GenericAggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 */
	public CI(Class<? extends Contract<I, O, T>> contract, Aggregator<O, T, Q> agg, Selector<I, O, T> sel) {
		super(Contracts.discover(contract), agg, sel);
	}

	/**
	 * Create a CI using source discovery
	 * 
	 * @param contract
	 *            The {@link Contract} to discover sources with
	 * @param agg
	 *            The {@link GenericAggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 * @param acceptor
	 *            The {@link Acceptor} to use
	 */
	public CI(Class<? extends Contract<I, O, T>> contract, Aggregator<O, T, Q> agg, Selector<I, O, T> sel,
			Acceptor<O, Q> acceptor) {
		super(Contracts.discover(contract), agg, sel, acceptor);
	}

	/**
	 * Create a CI using an explicit source set
	 * 
	 * @param sources
	 *            The {@link Source}s to select from
	 * @param agg
	 *            The {@link GenericAggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 */
	public CI(Collection<Source<I, O, T>> sources, Aggregator<O, T, Q> agg, Selector<I, O, T> sel) {
		super(sources, agg, sel);
	}

	/**
	 * Create a CI using an explicit source set
	 * 
	 * @param sources
	 *            The {@link Source}s to select from
	 * @param agg
	 *            The {@link GenericAggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 * @param acceptor
	 *            The {@link Acceptor}
	 */
	public CI(Collection<Source<I, O, T>> sources, Aggregator<O, T, Q> agg, Selector<I, O, T> sel,
			Acceptor<O, Q> acceptor) {
		super(sources, agg, sel, acceptor);
	}

	/**
	 * Create a CI using a {@link Provider}
	 * 
	 * @param provider
	 *            The {@link Source}s to select from
	 * @param agg
	 *            The {@link GenericAggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 */
	public CI(Provider<I, O, T> provider, Aggregator<O, T, Q> agg, Selector<I, O, T> sel) {
		super(provider, agg, sel);
	}

	/**
	 * Create a CI using a {@link Provider}
	 * 
	 * @param provider
	 *            The {@link Source}s to select from
	 * @param agg
	 *            The {@link GenericAggregator} to use
	 * @param sel
	 *            The {@link Selector} to use
	 * @param acceptor
	 *            The {@link Acceptor}
	 */
	public CI(Provider<I, O, T> provider, Aggregator<O, T, Q> agg, Selector<I, O, T> sel, Acceptor<O, Q> acceptor) {
		super(provider, agg, sel, acceptor);
	}
}
