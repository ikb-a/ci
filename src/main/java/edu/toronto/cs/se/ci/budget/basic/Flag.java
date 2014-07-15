package edu.toronto.cs.se.ci.budget.basic;

import edu.toronto.cs.se.ci.budget.Allowance;

/**
 * This represents a single flag. Flags are non-depletable requirements.
 * They are either required (see {@link RequiredFlag}), or restricted(see {@link RestrictedFlag}).
 * 
 * @author Michael Layzell
 *
 */
public class Flag implements Allowance {
	
	private String name;
	
	public Flag(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
