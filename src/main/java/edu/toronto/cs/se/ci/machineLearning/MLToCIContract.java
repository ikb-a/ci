package edu.toronto.cs.se.ci.machineLearning;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Contract;
import edu.toronto.cs.se.ci.Contracts;
import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;

/**
 * This class is a special adaptor and contract for use with the CI class.
 * During it's construction it accepts a {@link MLContract}. It then retrieves
 * all classes registered in {@link edu.toronto.cs.se.ci.Contracts} that
 * implement this contract (note that these sources can have multiple different
 * types of Trust). This class then acts as a contract of type <I, O, Void>
 * which can be passed to a CI of type <I, O, Void> for evaluation.
 * 
 * @param <I>
 *            The input type of the MLContract to be wrapped.
 * @param <O>
 *            The output type of the MLContract to be wrapped.
 */
public class MLToCIContract<I, O> implements Contract<I, O, Void> {
	/**
	 * The contract that this {@link MLCIContract} wraps
	 */
	public final Class<? extends MLContract<I, O>> contract;

	/**
	 * Constructs a new {@link MLCIContract} that will provide the sources that
	 * implement this contract as sources of type <I, O, Void>.
	 * 
	 * @param contract
	 *            The {@link MLContract} to be wrapped.
	 */
	public MLToCIContract(Class<? extends MLContract<I, O>> contract) {
		this.contract = contract;
	}

	/**
	 * Provides a list of {@link edu.toronto.cs.se.ci.Source} of type <I, O,
	 * Void>, where all the sources returned are sources registered in
	 * {@link edu.toronto.cs.se.ci.Contracts} that implement {@link #contract}
	 * 
	 * @return List of sources that implement the {@link MLContract} given at
	 *         construction
	 */
	@Override
	public List<Source<I, O, Void>> provide() {
		// Gets all the sources that implement the class contract, that are
		// registered in Contracts
		List<Source<I, O, ?>> unconvertedSources = Contracts.discoverML(contract);
		List<Source<I, O, Void>> convertedSources = new ArrayList<Source<I, O, Void>>();

		// Creates a wrapper source for each source discovered
		for (Source<I, O, ?> unconvertedSource : unconvertedSources) {
			convertedSources.add(new transformedSource<I, O>(unconvertedSource));
		}
		return convertedSources;
	}

	/**
	 * This class wraps a {@link edu.toronto.cs.se.ci.Source<II, IO, ?>} into a
	 * Source<II, IO, Void>
	 * 
	 * @author ikba
	 *
	 * @param <II>
	 *            The Input type of the inner source
	 * @param <IO>
	 *            The Output type of the inner source
	 */
	private class transformedSource<II, IO> extends MLSource<II, IO> {
		private Source<II, IO, ?> originalSource;

		public transformedSource(Source<II, IO, ?> originalSource) {
			this.originalSource = originalSource;
		}

		@Override
		public Expenditure[] getCost(II args) throws Exception {
			return originalSource.getCost(args);
		}

		@Override
		public Opinion<IO, Void> getOpinion(II args) throws UnknownException {
			Opinion<IO, ?> originalOpinion = originalSource.getOpinion(args);
			// Create a new Opinion with the same value, but null as the trust
			return new Opinion<IO, Void>(originalOpinion.getValue(), null);
		}

		@Override
		public Void getTrust(II args, Optional<IO> value) {
			return null;
		}
	}
}
