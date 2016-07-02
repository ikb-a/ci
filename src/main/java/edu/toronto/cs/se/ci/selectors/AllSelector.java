package edu.toronto.cs.se.ci.selectors;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.GenericCI;
import edu.toronto.cs.se.ci.Selector;
import edu.toronto.cs.se.ci.Source;

/**
 * This selector selects every source, and runs them all in parallel.
 * 
 * @author Michael Layzell
 *
 * @param <I>
 * @param <O>
 * @param <T>
 */
public class AllSelector<I, O, T> implements Selector<I, O, T> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.toronto.cs.se.ci.Selector#getNextSource(edu.toronto.cs.se.ci.CI.
	 * Invocation)
	 */
	@Override
	public Optional<Source<I, O, T>> getNextSource(GenericCI<I, O, ?, T, ?>.Invocation invocation) {
		try {
			for (Source<I, O, T> source : invocation.getRemaining()) {
				if (invocation.withinBudget(source))
					return Optional.of(source);
			}
		} catch (Exception e) {
			System.out.println("Exception thrown by source budget in selector."+e);
			return Optional.absent();
		}

		return Optional.absent();
	}

}
