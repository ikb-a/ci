package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.List;

public final class Contracts {

	private Contracts() {}
	
	private static List<Object> providers = new ArrayList<>();
	
	public static void register(Object provider) {
		if (! (provider instanceof Contract<?, ?, ?>))
			throw new Error("Provider " + provider.getClass().getName() + " must provide at least one Contract");
		
		providers.add(provider);
	}
	
	public static <I, O, T> List<Source<I, O, T>> discover(Class<? extends Contract<I, O, T>> contract) {
		List<Source<I, O, T>> sources = new ArrayList<>();

		for (Object maybeProvider : providers) {
			if (contract.isInstance(maybeProvider)) {
				// I actually have verified this is the case, as the type `contract` must be
				// of type Contract<F, T> - thus if src is of type `contract`, then it is of
				// type Contract<F, T>
				@SuppressWarnings("unchecked")
				Contract<I, O, T> provider = (Contract<I, O, T>) maybeProvider;

				sources.addAll(provider.provide());
			}
		}

		return sources;
	}

}
