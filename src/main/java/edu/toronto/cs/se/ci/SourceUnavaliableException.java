package edu.toronto.cs.se.ci;

public class SourceUnavaliableException extends Exception {
	
	public SourceUnavaliableException() {
		super();
	}
	
	public SourceUnavaliableException(String message) {
		super(message);
	}
	
	public SourceUnavaliableException(Throwable err) {
		super(err);
	}
	
	public SourceUnavaliableException(String message, Throwable err) {
		super(message, err);
	}
	
	public SourceUnavaliableException(String message, Throwable err, boolean enableSuppression, boolean writableStackTrace) {
		super(message, err, enableSuppression, writableStackTrace);
	}

	private static final long serialVersionUID = 1L;

}
