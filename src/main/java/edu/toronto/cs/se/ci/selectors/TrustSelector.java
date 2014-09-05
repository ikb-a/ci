package edu.toronto.cs.se.ci.selectors;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;
import edu.toronto.cs.se.ci.Selector;
import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.data.Trust;

/**
 * Select elements in order of decreasing trust. (Uses EBT {@link Trust} values)
 * 
 * @author Michael Layzell
 *
 * @param <I>
 * @param <O>
 */
public class TrustSelector<I, O> implements Selector<I, O, Trust> {

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Selector#getNextSource(edu.toronto.cs.se.ci.CI.Invocation)
	 */
	@Override
	public Optional<Source<I, O, Trust>> getNextSource(CI<I, O, Trust, ?>.Invocation invocation) {
		List<Source<I, O, Trust>> sources = new ArrayList<>(invocation.getRemaining());
		sources.sort(new TrustComparator<I>(invocation.getArgs()));
		
		try {
			for (Source<I, O, Trust> source : sources) {
				if (invocation.withinBudget(source)) {
					return Optional.of(source);
				}
			}
		} catch (Exception e) {
			return Optional.absent();
		}
		
		return Optional.absent();
	}
	
	/**
	 * A comparator, used for the implementation of the selection function
	 * 
	 * @author Michael Layzell
	 *
	 * @param <I>
	 */
	public static class TrustComparator<I> implements Comparator<Source<I, ?, Trust>> {

		private I args;

		public TrustComparator(I args) {
			this.args = args;
		}

		@Override
		public int compare(Source<I, ?, Trust> o1, Source<I, ?, Trust> o2) {
			return (int) (o1.getTrust(args, Optional.absent()).getDisbelief()
					    - o2.getTrust(args, Optional.absent()).getDisbelief());
		}
		
	}

}
