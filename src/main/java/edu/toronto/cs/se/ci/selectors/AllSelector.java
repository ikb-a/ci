package edu.toronto.cs.se.ci.selectors;

import java.util.Collection;

import edu.toronto.cs.se.ci.CI;
import edu.toronto.cs.se.ci.Selector;
import edu.toronto.cs.se.ci.Source;

public class AllSelector<F, T> implements Selector<F, T> {

	@Override
	public Source<F, T> getNextSource(CI<F, T>.Invocation invocation) {
		// TODO: Ensure that the sources we select fit within the budget
		Collection<Source<F, T>> sources = invocation.getSources();
		Collection<Source<F, T>> consulted = invocation.getConsulted();
		
		for (Source<F, T> source : sources) {
			if (! consulted.contains(source))
				return source;
		}

		return null;
	}

}
