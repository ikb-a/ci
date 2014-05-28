package edu.toronto.cs.se.ci;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public final class Sources {
	
	private Sources() {}
	
	/**
	 * Wraps the source, modifying its inputs and outputs.
	 * 
	 * @param source The original source
	 * @param in Wraps the source's inputs
	 * @param out Wraps the source's outputs
	 * @return A new source, which is equivalent to the original source, except that the function 
	 * {@code in} is applied to the input before it is passed to the source, and the function 
	 * {@code out} is applied to the output opinion before it is returned.
	 */
	public static <F, T, NF, NT> Source<NF, NT> wrap(final Source<F, T> source, 
			final Function<NF, F> in, final Function<Opinion<T>, Opinion<NT>> out) {
		return new Source<NF, NT>() {

			@Override
			public Cost getCost(NF args) throws Exception {
				F nArgs = in.apply(args);
				return source.getCost(nArgs);
			}

			@Override
			public Opinion<NT> getOpinion(NF args)
					throws UnknownException {
				F nArgs = in.apply(args);
				Opinion<T> opinion = source.getOpinion(nArgs);
				return out.apply(opinion);
			}

		};
	}
	
	/**
	 * Wraps the source, modifying its inputs
	 * 
	 * @param source The original source
	 * @param fn Wraps the source's inputs
	 * @return A new source, which is equivalent to the original source, except inputs are
	 * transformed by {@code fn} before they are passed to the source.
	 */
	public static <F, T, NF> Source<NF, T> wrapArgs(final Source<F, T> source, final Function<NF, F> fn) {
		return wrap(source, fn, Functions.<Opinion<T>>identity());
	}
	
	/**
	 * Wraps the source, modifying its outputs
	 * 
	 * @param source The original source
	 * @param fn Wraps the source's opinion
	 * @return A new source, which is equivalent to the original source, except that the source's
	 * opinion is transformed by {@code fn} before it is returned.
	 */
	public static <F, T, NT> Source<F, NT> wrapOpinion(final Source<F, T> source, final Function<Opinion<T>, Opinion<NT>> fn) {
		return wrap(source, Functions.<F>identity(), fn);
	}
	
	/**
	 * Wraps the source, modifying its response
	 * 
	 * @param source The original source
	 * @param fn Wraps the source's response
	 * @return A new source, which is equivalent to the original source, except that the source's
	 * value is transformed by {@code fn} before it is returned.
	 */
	public static <F, T, NT> Source<F, NT> wrapResponse(final Source<F, T> source, final Function<T, NT> fn) {
		return wrapOpinion(source, new Function<Opinion<T>, Opinion<NT>>() {

			@Override
			public Opinion<NT> apply(Opinion<T> input) {
				return new Opinion<NT>(fn.apply(input.getValue()), input.getTrust());
			}
			
		});
	}
	
	/**
	 * Wraps the source, modifying its trust. This can be used for changing the trust values of
	 * sources which were written by other developers.
	 * 
	 * @param source The original source
	 * @param fn Wraps the source's trust
	 * @return A new source, which is equivalent to the original source, except that the source's
	 * trust is transformed by {@code fn} before it is returned.
	 */
	public static <F, T> Source<F, T> wrapTrust(final Source<F, T> source, final Function<Opinion<T>, Double> fn) {
		return wrapOpinion(source, new Function<Opinion<T>, Opinion<T>>() {

			@Override
			public Opinion<T> apply(Opinion<T> input) {
				return new Opinion<T>(input.getValue(), fn.apply(input));
			}
			
		});
	}
	
}
