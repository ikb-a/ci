package edu.toronto.cs.se.ci;

/**
 * This exception is thrown by a source when it doesn't have an opinion.
 * 
 * @author Michael Layzell
 *
 */
public class UnknownException extends Exception {
	
	public UnknownException() {
		super();
	}
	
	public UnknownException(String message) {
		super(message);
	}
	
	public UnknownException(Throwable err) {
		super(err);
	}
	
	public UnknownException(String message, Throwable err) {
		super(message, err);
	}
	
	public UnknownException(String message, Throwable err, boolean enableSuppression, boolean writableStackTrace) {
		super(message, err, enableSuppression, writableStackTrace);
	}

	private static final long serialVersionUID = 1L;

}
