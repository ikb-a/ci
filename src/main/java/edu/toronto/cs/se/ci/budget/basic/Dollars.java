package edu.toronto.cs.se.ci.budget.basic;

import java.math.BigDecimal;

import edu.toronto.cs.se.ci.budget.DecimalDepletable;

/**
 * Represents a cost in Dollars.
 * 
 * @author Michael Layzell
 *
 */
public class Dollars extends DecimalDepletable {

	public Dollars(BigDecimal quantity) {
		super(quantity);
	}

}
