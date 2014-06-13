package edu.toronto.cs.se.ci.selectors;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;
import edu.toronto.cs.se.ci.Selector;
import edu.toronto.cs.se.ci.Source;

public class TrustSelector<F, T> implements Selector<F, T> {

	@Override
	public Optional<Source<F, T>> getNextSource(CI<F, T>.Invocation invocation) {
		List<Source<F, T>> sources = new ArrayList<>(invocation.getRemaining());
		sources.sort(new TrustComparator<F>(invocation.getArgs()));
		
		try {
			for (Source<F, T> source : sources) {
				if (invocation.withinBudget(source)) {
					return Optional.of(source);
				}
			}
		} catch (Exception e) {
			return Optional.absent();
		}
		
		return Optional.absent();
	}
	
	public static class TrustComparator<F> implements Comparator<Source<F, ?>> {

		private F args;

		public TrustComparator(F args) {
			this.args = args;
		}

		@Override
		public int compare(Source<F, ?> o1, Source<F, ?> o2) {
			return (int) (o1.getTrust(args, Optional.absent()).getDisbelief()
					    - o2.getTrust(args, Optional.absent()).getDisbelief());
		}
		
	}

}
