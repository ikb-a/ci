package edu.toronto.cs.se.ci.utils;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Adaptor;
import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.data.Cost;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Trust;

/**
 * This source wraps another source, transforming its arguments, results, 
 * and trust before it is returned to the CI for aggregation.
 * 
 * @author Michael Layzell
 *
 * @param <F> Transformed input type
 * @param <T> Transformed output type
 * @param <OF> Original input type
 * @param <OT> Original output type
 */
public abstract class BasicAdaptor<F, T, OF, OT> extends Adaptor<F, T, OF, OT> {
	
	/**
	 * Transforms the arguments to provide to the adaptee
	 * 
	 * @param args The input arguments
	 * @return The arguments passed to the adaptee
	 */
	public abstract OF transformArgs(F args);
	
	/**
	 * Transforms the result provided by the adaptee
	 * 
	 * @param result The original result
	 * @return The transformed result
	 */
	public abstract T transformResult(OT result);
	
	/**
	 * Generates a trust value for an opinion provided by the adaptee
	 * 
	 * @param result The result of {@code transformResult}
	 * @param opinion The original opinion
	 * @return The new trust value
	 */
	public abstract Trust transformTrust(Trust trust, Optional<T> result, Optional<OT> originalResult);

	/**
	 * Transforms an opinion provided by the adaptee
	 * 
	 * @param opinion The original opinion
	 * @return The transformed opinion
	 */
	public Opinion<T> transformOpinion(Opinion<OT> opinion) {
		T newResult = transformResult(opinion.getValue());
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
	public Cost getCost(F args, Source<OF, OT> adaptee) throws Exception {
		return adaptee.getCost(transformArgs(args));
	}

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
	 */
	@Override
	public Opinion<T> getOpinion(F args, Source<OF, OT> adaptee) throws UnknownException {
		return transformOpinion(adaptee.getOpinion(transformArgs(args)));
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getTrust(java.lang.Object, com.google.common.base.Optional)
	 */
	@Override
	public Trust getTrust(F args, Optional<T> result, Source<OF, OT> adaptee) {
		Trust trust = adaptee.getTrust(transformArgs(args), Optional.<OT>absent());
		return transformTrust(trust, result, Optional.<OT>absent());
	}

}
