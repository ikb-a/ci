package edu.toronto.cs.se.ci;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.data.Cost;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Trust;

public abstract class Adaptor<F, T, OF, OT> {
	
	/**
	 * @return The Contract which the Adaptor wraps around
	 */
	public abstract Class<? extends Contract<OF, OT>> around();
	
	public List<Source<F, T>> provide() {
		List<Source<F, T>> sources = new ArrayList<>();

		for (Source<OF, OT> s : Contracts.discover(around())) {
			sources.add(new SrcAdaptor(s));
		}

		return sources;
	}
	
	public abstract Cost getCost(F args, Source<OF, OT> around) throws Exception;
	
	public abstract Opinion<T> getOpinion(F args, Source<OF, OT> around) throws UnknownException;
	
	public abstract Trust getTrust(F args, Optional<T> value, Source<OF, OT> around);
	
	private class SrcAdaptor extends Source<F, T> {
		
		private Source<OF, OT> around;
		
		public SrcAdaptor(Source<OF, OT> around) {
			this.around = around;
		}
		
		@Override
		public String getName() {
			return Adaptor.this.getClass().getName() + ":" + around.getName();
		}

		@Override
		public Cost getCost(F args) throws Exception {
			return Adaptor.this.getCost(args, around);
		}

		@Override
		public Opinion<T> getOpinion(F args) throws UnknownException {
			return Adaptor.this.getOpinion(args, around);
		}

		@Override
		public Trust getTrust(F args, Optional<T> value) {
			return Adaptor.this.getTrust(args, value, around);
		}
		
	}
	
}
