package edu.toronto.cs.se.ci;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class Opinion<T> {
	// TODO: This should extend from AbstractFuture, and be a future itself (wrapping both value and trust)
	
	private ListenableFuture<Float> trust;
	private ListenableFuture<T> value;
	
	public Opinion(ListenableFuture<T> value, ListenableFuture<Float> trust) {
		this.value = value;
		this.trust = trust;
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
	
	public float getTrust() {
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
	
	public ListenableFuture<Float> getTrustFuture() {
		return trust;
	}

}
