package edu.toronto.cs.se.ci;

import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class Opinion<T> extends AbstractFuture<Opinion<T>> {
	private final ListenableFuture<Double> trust;
	private final ListenableFuture<T> value;
	
	public Opinion(ListenableFuture<T> value, ListenableFuture<Double> trust) {
		this.value = value;
		this.trust = trust;
		
		// Resolve the Opinion when both the trust and the value have resolved
		ResolveCallback cb = new ResolveCallback();
		Futures.addCallback(trust, cb);
		Futures.addCallback(value, cb);
	}
	
	public boolean isDone() {
		return value.isDone() && trust.isDone();
	}
	
	public T getValue() {
		try {
			return value.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public double getTrust() {
		try {
			return trust.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public ListenableFuture<T> getValueFuture() {
		return value;
	}
	
	public ListenableFuture<Double> getTrustFuture() {
		return trust;
	}
	
	private class ResolveCallback implements FutureCallback<Object> {

		@Override
		public void onSuccess(Object result) {
			if (value.isDone() && trust.isDone())
				set(Opinion.this);
		}

		@Override
		public void onFailure(Throwable t) {
			setException(t);
		}
		
	}

}
