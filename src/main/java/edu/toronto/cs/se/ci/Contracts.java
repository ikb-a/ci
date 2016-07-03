package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import edu.toronto.cs.se.ci.machineLearning.MLContract;

/**
 * The {@link Contract} registry. Manages and allows for source discovery via
 * contracts.
 * 
 * @author Michael Layzell
 *
 */
public final class Contracts {

	private Contracts() {
	}

	private static Collection<Object> providers = new HashSet<>();

	/**
	 * Register an object as a provider for a given contract
	 * 
	 * @param provider
	 *            The provider which implements a non-generic subclass of
	 *            {@link Contract}
	 */
	public static void register(Object provider) {
		if (provider == null) {
			throw new IllegalArgumentException("Source cannot be null.");
		}
		if (!(provider instanceof Contract<?, ?, ?>) && !(provider instanceof MLContract<?, ?>))
			throw new Error("Provider " + provider.getClass().getName() + " must provide at least one Contract");

		providers.add(provider);
	}

	/**
	 * Discover all registered sources which implement the passed contract
	 * 
	 * @param contract
	 *            The contract which must be implemented
	 * @return A list of sources which implement the given contract
	 */
	public static <I, O, T> List<Source<I, O, T>> discover(Class<? extends Contract<I, O, T>> contract) {
		List<Source<I, O, T>> sources = new ArrayList<>();

		for (Object maybeProvider : providers) {
			if (contract.isInstance(maybeProvider)) {
				// I actually have verified this is the case, as the type
				// `contract` must be
				// of type Contract<F, T> - thus if src is of type `contract`,
				// then it is of
				// type Contract<F, T>
				@SuppressWarnings("unchecked")
				Contract<I, O, T> provider = (Contract<I, O, T>) maybeProvider;

				sources.addAll(provider.provide());
			}
		}
		return sources;
	}

	/**
	 * Discover all registered sources which implement the passed MLContract
	 * 
	 * @param contract
	 *            The contract which must be implemented
	 * @return A list of sources which implement the given contract
	 */
	public static <I, O> List<Source<I, O, ?>> discoverML(Class<? extends MLContract<I, O>> contract) {
		List<Source<I, O, ?>> sources = new ArrayList<>();

		for (Object maybeProvider : providers) {
			if (contract.isInstance(maybeProvider)) {
				@SuppressWarnings("unchecked")
				MLContract<I, O> provider = (MLContract<I, O>) maybeProvider;

				sources.addAll(provider.provideML());
			}
		}
		return sources;
	}

	/**
	 * Removes all registered sources.
	 */
	public static void clear() {
		providers = new ArrayList<>();
	}

}
