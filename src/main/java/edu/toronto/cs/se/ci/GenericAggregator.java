package edu.toronto.cs.se.ci;

import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;

/**
 * The GenericAggregator is a single-method interface. It is used to combine the
 * opinions returned by sources into a single unified result.
 * <p>
 * More specifically, it aggregates a list of Opinions (usually from a
 * {@link Source} with a value of type O and a Trust of type T, into a Result
 * with a value of type FO and a quality of type Q.
 * 
 * @author Michael Layzell
 * @author Ian Berlot-Attwel
 *
 * @param <O>
 *            output type of source
 * @param <FO>
 *            output type of aggregator
 * @param <T>
 *            trust type of source
 * @param <Q>
 *            quality type of aggregator
 */
public interface GenericAggregator<O, FO, T, Q> {

	/**
	 * @param opinions
	 *            The opinions provided by sources in the CI
	 * @return An aggregated result
	 */
	public Optional<Result<FO, Q>> aggregate(List<Opinion<O, T>> opinions);

}
