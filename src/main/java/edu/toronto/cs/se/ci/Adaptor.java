package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;

public abstract class Adaptor<I, O, T, OI, OO, OT> {
	
	private Class<? extends Contract<OI, OO, OT>> around;
	private Source<OI, OO, OT> aroundSource;
	
	public Adaptor(Class<? extends Contract<OI, OO, OT>> around) {
		this.around = around;
	}
	
	public Adaptor(Source<OI, OO, OT> around) {
		this.aroundSource = around;
	}
	
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
	
	public abstract Expenditure[] getCost(I args, Source<OI, OO, OT> around) throws Exception;
	
	public abstract Opinion<O, T> getOpinion(I args, Source<OI, OO, OT> around) throws UnknownException;
	
	public abstract T getTrust(I args, Optional<O> value, Source<OI, OO, OT> around);
	
	private class SrcAdaptor extends Source<I, O, T> {
		
		private Source<OI, OO, OT> around;
		
		public SrcAdaptor(Source<OI, OO, OT> around) {
			this.around = around;
		}
		
		@Override
		public String getName() {
			return Adaptor.this.getClass().getName() + ":" + around.getName();
		}

		@Override
		public Expenditure[] getCost(I args) throws Exception {
			return Adaptor.this.getCost(args, around);
		}

		@Override
		public Opinion<O, T> getOpinion(I args) throws UnknownException {
			return Adaptor.this.getOpinion(args, around);
		}

		@Override
		public T getTrust(I args, Optional<O> value) {
			return Adaptor.this.getTrust(args, value, around);
		}
		
	}
	
}
