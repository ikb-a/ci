package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;

/**
 * Wraps around a source, transforming its inputs and outputs, allowing a
 * source to fulfil multiple different contracts. 
 * 
 * @author michael
 *
 * @param <I> New Source Input Type
 * @param <O> New Source Output Type
 * @param <T> New Source Trust Type
 * @param <OI> Original Source Input Type
 * @param <OO> Original Source Output Type
 * @param <OT> Original Source Trust Type
 */
public abstract class Adaptor<I, O, T, OI, OO, OT> {
	
	private Class<? extends Contract<OI, OO, OT>> around;
	private Source<OI, OO, OT> aroundSource;
	
	/**
	 * Create a source which wraps around a given contract type
	 * 
	 * @param around The contract to wrap around. Contract discovery will be used to {@link provide()}
	 */
	public Adaptor(Class<? extends Contract<OI, OO, OT>> around) {
		this.around = around;
	}
	
	/**
	 * Create a source which wraps around an individual source
	 * 
	 * @param around The source to wrap around.
	 */
	public Adaptor(Source<OI, OO, OT> around) {
		this.aroundSource = around;
	}
	
	/**
	 * Discover all sources which are wrapped by this adaptor, and generate concrete adapted sources.
	 * 
	 * @return A list of concrete sources generated by the Adaptor
	 */
	public List<Source<I, O, T>> provide() {
		List<Source<I, O, T>> sources = new ArrayList<>();

		if (this.around != null) {
			for (Source<OI, OO, OT> s : Contracts.discover(around)) {
				sources.add(new SrcAdaptor(s));
			}
		} else {
			sources.add(new SrcAdaptor(aroundSource));
		}

		return sources;
	}
	
	/**
	 * Determine the cost of the adapted source
	 * 
	 * @param args The input arguments to the Source
	 * @param around The source being wrapped
	 * @return The cost of the adapted source
	 * @throws Exception
	 */
	public abstract Expenditure[] getCost(I args, Source<OI, OO, OT> around) throws Exception;
	
	/**
	 * Determine the opinion of the adapted source
	 * 
	 * @param args The input arguments to the Source
	 * @param around The source being wrapped
	 * @return The opinion of the adapted source
	 * @throws UnknownException If the source doesn't produce an opinion
	 */
	public abstract Opinion<O, T> getOpinion(I args, Source<OI, OO, OT> around) throws UnknownException;
	
	/**
	 * Determine the trust in the adapted source
	 * 
	 * @param args The input arguments to the Source
	 * @param value Potentially the value produced by the source
	 * @param around The source being wrapped
	 * @return The trust in the adapted source
	 */
	public abstract T getTrust(I args, Optional<O> value, Source<OI, OO, OT> around);
	

	/**
	 * Internal concrete implementation of source. Generated by {@link Adaptor#provide()}
	 * 
	 * @author Michael Layzell
	 *
	 */
	private class SrcAdaptor extends Source<I, O, T> {
		
		private Source<OI, OO, OT> around;
		
		public SrcAdaptor(Source<OI, OO, OT> around) {
			this.around = around;
		}
		
		/*
		 * (non-Javadoc)
		 * @see edu.toronto.cs.se.ci.Source#getName()
		 */
		@Override
		public String getName() {
			return Adaptor.this.getClass().getName() + ":" + around.getName();
		}

		/*
		 * (non-Javadoc)
		 * @see edu.toronto.cs.se.ci.Source#getCost(java.lang.Object)
		 */
		@Override
		public Expenditure[] getCost(I args) throws Exception {
			return Adaptor.this.getCost(args, around);
		}

		/*
		 * (non-Javadoc)
		 * @see edu.toronto.cs.se.ci.Source#getOpinion(java.lang.Object)
		 */
		@Override
		public Opinion<O, T> getOpinion(I args) throws UnknownException {
			return Adaptor.this.getOpinion(args, around);
		}

		/*
		 * (non-Javadoc)
		 * @see edu.toronto.cs.se.ci.Source#getTrust(java.lang.Object, com.google.common.base.Optional)
		 */
		@Override
		public T getTrust(I args, Optional<O> value) {
			return Adaptor.this.getTrust(args, value, around);
		}
		
	}
	
}
