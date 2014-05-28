package edu.toronto.cs.se.ci;

public final class Opinion<T> {

	private final double trust;
	private final T value;
	
	public Opinion(T value, double trust) {
		this.value = value;
		this.trust = trust;
	}
	
	public T getValue() {
		return value;
	}
	
	public double getTrust() {
		return trust;
	}

}
