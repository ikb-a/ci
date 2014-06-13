package edu.toronto.cs.se.ci.selectors;

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
		try {
			for (Source<F, T> source : invocation.getRemaining()) {
				if (invocation.withinBudget(source))
					return source;
			}
		} catch (Exception e) {
			return null;
		}
		
		return null;
	}

}
