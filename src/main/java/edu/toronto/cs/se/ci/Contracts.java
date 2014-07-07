package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.List;

public final class Contracts {

	private Contracts() {}
	
	private static List<Object> providers = new ArrayList<>();
	
	public static void register(Object provider) {
		if (! (provider instanceof Contract<?, ?>))
			throw new Error("Provider must provide at least one Contract");
		
		providers.add(provider);
	}
	
	public static <F, T> List<Source<F, T>> discover(Class<? extends Contract<F, T>> contract) {
		List<Source<F, T>> sources = new ArrayList<>();

		for (Object maybeProvider : providers) {
			if (contract.isInstance(maybeProvider)) {
				// I actually have verified this is the case, as the type `contract` must be
				// of type Contract<F, T> - thus if src is of type `contract`, then it is of
				// type Contract<F, T>
				@SuppressWarnings("unchecked")
				Contract<F, T> provider = (Contract<F, T>) maybeProvider;

				sources.addAll(provider.provide());
			}
		}

		return sources;
	}

}
