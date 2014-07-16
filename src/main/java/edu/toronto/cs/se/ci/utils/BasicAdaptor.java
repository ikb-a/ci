package edu.toronto.cs.se.ci.utils;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Adaptor;
import edu.toronto.cs.se.ci.Contract;
import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;

/**
 * This source wraps another source, transforming its arguments, results, 
 * and trust before it is returned to the CI for aggregation.
 * 
 * @author Michael Layzell
 *
 * @param <I> Transformed input type
 * @param <O> Transformed output type
 * @param <OI> Original input type
 * @param <OO> Original output type
 */
public abstract class BasicAdaptor<I, O, T, OI, OO, OT> extends Adaptor<I, O, T, OI, OO, OT> {
	
	public BasicAdaptor(Class<? extends Contract<OI, OO, OT>> around) {
		super(around);
	}
	
	/**
	 * Transforms the arguments to provide to the adaptee
	 * 
	 * @param args The input arguments
	 * @return The arguments passed to the adaptee
	 */
	public abstract OI transformArgs(I args);
	
	/**
	 * Transforms the result provided by the adaptee
	 * 
	 * @param result The original result
	 * @return The transformed result
	 */
	public abstract O transformResult(OO result);
	
	/**
	 * Generates a trust value for an opinion provided by the adaptee
	 * 
	 * @param result The result of {@code transformResult}
	 * @param opinion The original opinion
	 * @return The new trust value
	 */
	public abstract T transformTrust(OT trust, Optional<O> result, Optional<OO> originalResult);

	/**
	 * Transforms an opinion provided by the adaptee
	 * 
	 * @param opinion The original opinion
	 * @return The transformed opinion
	 */
	public Opinion<O, T> transformOpinion(Opinion<OO, OT> opinion) {
		O newResult = transformResult(opinion.getValue());
		return new Opinion<>(newResult, 
				transformTrust(
						opinion.getTrust(), 
						Optional.of(newResult), 
						Optional.of(opinion.getValue())));
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getCost(java.lang.Object)
	 */
	@Override
	public Expenditure[] getCost(I args, Source<OI, OO, OT> adaptee) throws Exception {
		return adaptee.getCost(transformArgs(args));
	}

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
	 */
	@Override
	public Opinion<O, T> getOpinion(I args, Source<OI, OO, OT> adaptee) throws UnknownException {
		return transformOpinion(adaptee.getOpinion(transformArgs(args)));
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getTrust(java.lang.Object, com.google.common.base.Optional)
	 */
	@Override
	public T getTrust(I args, Optional<O> result, Source<OI, OO, OT> adaptee) {
		OT trust = adaptee.getTrust(transformArgs(args), Optional.<OO>absent());
		return transformTrust(trust, result, Optional.<OO>absent());
	}

}
