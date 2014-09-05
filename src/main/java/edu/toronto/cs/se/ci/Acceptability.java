package edu.toronto.cs.se.ci;

/**
 * Represents the acceptability of a given response. Returned by a {@link Acceptor}.
 * GOOD => No more sources need to be queried, and the answer is acceptable.
 * OK   => If there are no more sources to query, answer is acceptable, but query more if possible.
 * BAD  => If there are no more sources to query, reject the future, as it is unacceptable.
 * 
 * @author Michael Layzell
 */
public enum Acceptability {
	GOOD,
	OK,
	BAD
}
