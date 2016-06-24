package edu.toronto.cs.se.ci;

import java.util.Collection;

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
}
