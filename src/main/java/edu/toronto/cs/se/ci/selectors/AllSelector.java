package edu.toronto.cs.se.ci.selectors;

import java.util.Collection;

import edu.toronto.cs.se.ci.CI;
import edu.toronto.cs.se.ci.Selector;
import edu.toronto.cs.se.ci.Source;

/**
 * This selector selects every source, and runs them all in parallel.
 * 
 * @author Michael Layzell
 *
 * @param <F>
 * @param <T>
 */
public class AllSelector<F, T> implements Selector<F, T> {

	@Override
	public Source<F, T> getNextSource(CI<F, T>.Invocation invocation) {
		// TODO: Ensure that the sources we select fit within the budget
		Collection<Source<F, T>> sources = invocation.getSources();
		Collection<Source<F, T>> consulted = invocation.getConsulted();
		
		try {
			for (Source<F, T> source : sources) {
				if (! consulted.contains(source) && invocation.withinBudget(source))
					return source;
			}
		} catch (Exception e) {
			// There was a problem with getting the cost of the source. Throw an exception
			return null;
		}

		return null;
	}

}
