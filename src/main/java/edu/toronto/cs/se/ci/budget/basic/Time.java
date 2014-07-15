package edu.toronto.cs.se.ci.budget.basic;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.CI;
import edu.toronto.cs.se.ci.budget.Allowance;
import edu.toronto.cs.se.ci.budget.Expenditure;

/**
 * The time Allowance represents an amount of time which is available
 * for a computation to be completed in. If the time Allowance is not
 * present, it is assumed that unlimited time is available for the computation
 * to be completed.
 * 
 * The time Expenditure represents the amount of time needed to complete
 * a computation. If it is not present, then it is assumed that the time necessary
 * is negligible.
 * 
 * @author Michael Layzell
 *
 */
public class Time implements Allowance, Expenditure {
	
	private long nanos;
	
	public Time(long duration, TimeUnit unit) {
		nanos = unit.toNanos(duration);
	}
	
	public long getDuration(TimeUnit unit) {
		return unit.convert(nanos, TimeUnit.NANOSECONDS);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.budget.Expenditure#expend(edu.toronto.cs.se.ci.budget.Allowance[], com.google.common.base.Optional)
	 */
	@Override
	public Optional<Allowance[]> expend(Allowance[] budget, Optional<CI<?, ?>.Invocation> invocation) {
		for (int i = 0; i < budget.length; i++) {
			if (budget[i] instanceof Time) {
				Time allowance = (Time) budget[i];

				long elapsed = 0;
				if (invocation.isPresent())
					elapsed = invocation.get().getElapsedTime(TimeUnit.NANOSECONDS);
				
				// If insufficient time is remaining, abort.
				if (allowance.getDuration(TimeUnit.NANOSECONDS) - elapsed - getDuration(TimeUnit.NANOSECONDS) < 0)
					return Optional.absent();
			}
		}

		return Optional.of(budget);
	}

}
