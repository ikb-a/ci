package edu.toronto.cs.se.ci.aggregators;

import java.util.ArrayList;
import java.util.List;

import edu.toronto.cs.se.ci.Opinion;
import edu.toronto.cs.se.ci.Result;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ProbabalisticAggregatorTest extends TestCase {
	
	public ProbabalisticAggregatorTest() {
		super("ProbabalisticAggregatorTest");
	}
	
	public static Test suite() {
		return new TestSuite( ProbabalisticAggregatorTest.class );
	}
	
	public static void assertApprox(double result, double expected, double error) {
		assertTrue(Math.abs(result - expected) < error);
	}

	public void testBoolean() {
		ProbabalisticAggregator<Boolean> aggregator = new ProbabalisticAggregator<>(2);
		
		List<Opinion<Boolean>> opinions = new ArrayList<>();
		opinions.add(new Opinion<Boolean>(true, 0.6));
		opinions.add(new Opinion<Boolean>(true, 0.2));
		opinions.add(new Opinion<Boolean>(false, 0.8));

		Result<Boolean> result = aggregator.aggregate(opinions);
		assertEquals(result.getValue(), Boolean.FALSE);
		assertApprox(result.getQuality(), 0.4532, 0.0001);
	}
	
	public void testNumbers() {
		ProbabalisticAggregator<Integer> aggregator = new ProbabalisticAggregator<>(-1);
		
		List<Opinion<Integer>> opinions = new ArrayList<>();
		opinions.add(new Opinion<Integer>(3, 0.4));
		opinions.add(new Opinion<Integer>(3, 0.2));
		opinions.add(new Opinion<Integer>(6, 0.7));
		opinions.add(new Opinion<Integer>(5, 0.4));
		opinions.add(new Opinion<Integer>(5, 0.3));
		
		Result<Integer> result = aggregator.aggregate(opinions);
		assertEquals(result.getValue(), new Integer(6));
		assertApprox(result.getQuality(), 0.2796, 0.0001);
	}

}
