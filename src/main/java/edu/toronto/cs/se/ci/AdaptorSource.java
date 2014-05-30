package edu.toronto.cs.se.ci;

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
public abstract class AdaptorSource<F, T, OF, OT> implements Source<F, T> {
	
	private Source<OF, OT> adaptee;

	/**
	 * Create an AdaptorSource, setting the adaptee.
	 * 
	 * @param adaptee The source to transform the inputs/outputs of
	 */
	public AdaptorSource(Source<OF, OT> adaptee) {
		this.adaptee = adaptee;
	}
	
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
	public abstract double transformTrust(T result, Opinion<OT> opinion);

	/**
	 * Transforms an opinion provided by the adaptee
	 * 
	 * @param opinion The original opinion
	 * @return The transformed opinion
	 */
	public Opinion<T> transformOpinion(Opinion<OT> opinion) {
		T newResult = transformResult(opinion.getValue());
		return new Opinion<>(newResult, transformTrust(newResult, opinion));
	}

	/**
	 * @return The Source wrapped by this AdaptorSource
	 */
	public Source<OF, OT> getAdaptee() {
		return adaptee;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getCost(java.lang.Object)
	 */
	@Override
	public Cost getCost(F args) throws Exception {
		return adaptee.getCost(transformArgs(args));
	}

	/*
	 * (non-Javadoc)
	 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
	 */
	@Override
	public Opinion<T> getOpinion(F args) throws UnknownException {
		return transformOpinion(adaptee.getOpinion(transformArgs(args)));
	}

}
