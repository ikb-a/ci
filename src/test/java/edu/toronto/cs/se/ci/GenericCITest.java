package edu.toronto.cs.se.ci;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.primitives.Doubles;

import edu.toronto.cs.se.ci.budget.Allowance;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.budget.basic.Dollars;
import edu.toronto.cs.se.ci.budget.basic.Time;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.MLBasicSource;
import edu.toronto.cs.se.ci.machineLearning.MLSource;
import edu.toronto.cs.se.ci.machineLearning.MLToCIContract;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNominalConverter;
import edu.toronto.cs.se.ci.machineLearning.util.MLNominalThresholdAcceptor;
import edu.toronto.cs.se.ci.machineLearning.util.MLWekaNominalAggregator;
import edu.toronto.cs.se.ci.selectors.AllSelector;
import edu.toronto.cs.se.ci.utils.BasicSource;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.classifiers.bayes.NaiveBayes;

/**
 * Unit test for simple App.
 */
public class GenericCITest extends TestCase {
	/**
	 * Create the test case
	 */
	public GenericCITest() {
		super("GenericCITEst");
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(GenericCITest.class);
	}

	@Override
	public void setUp() {
		Contracts.clear();
	}

	/**
	 * Tests that a CI with a Time Budget that is too short stops; and throws an
	 * exception if the current estimate has an acceptability of
	 * Acceptability.BAD. (Run using
	 * {@link edu.toronto.cs.se.ci.GenericCI #applySync(Object, Allowance[])})
	 * 
	 * @throws Exception
	 */
	public void testCI1() throws Exception {
		System.out.println("CI1");
		MLNominalThresholdAcceptor<String> acc = new MLNominalThresholdAcceptor<String>(0.75, 0.95);
		AllSelector<Boolean, String, Void> sel = new AllSelector<Boolean, String, Void>();
		Contract<Boolean, String, Void> contract = new MLToCIContract<Boolean, String>(RepublicanOrDemocrat.class);
		MLWekaNominalConverter<String> conv = new NoActionConverter();
		GenericAggregator<String, String, Void, double[]> agg = new MLWekaNominalAggregator<String>(conv,
				"./vote-consistentNominalsCITrain .arff", new NaiveBayes());
		// TODO: MLToCIContract CANNOT be used as a contract to a CI. Create a
		// new interface (i.e. provider) and fix this so that a CI can accept a
		// provider; OR a contract; OR a list of sources.

		Contracts.register(new Adoption());
		Contracts.register(new Crime());
		Contracts.register(new DutyFree());
		Contracts.register(new Education());
		Contracts.register(new Handicap());
		Contracts.register(new Immigration());
		Contracts.register(new Missile());
		Contracts.register(new Nicaraguan());
		Contracts.register(new Physician());
		Contracts.register(new Religous());
		Contracts.register(new Salvador());
		Contracts.register(new Satellite());
		Contracts.register(new Superfund());
		Contracts.register(new Synfuel());
		Contracts.register(new WaterProject());
		GenericCI<Boolean, String, String, Void, double[]> test1 = new GenericCI<Boolean, String, String, Void, double[]>(
				contract.provide(), agg, sel, acc);

		// TODO: Improve this; right now it times out (as it should), resulting
		// in an undecipherable stack trace as the estimate is of
		// Acceptability.BAD

		try {
			Allowance[] budget = new Allowance[] { new Dollars(new BigDecimal(5)), new Time(1, TimeUnit.NANOSECONDS) };
			Result<String, double[]> resultResult = test1.applySync(true, budget);
			fail(resultResult.toString() + " Either has acceptability that is not BAD, or there is something wrong");
		} catch (ExecutionException e) {
		}
	}

	/**
	 * Testing the CI giving it no sources, when the result is considered
	 * Acceptability.OK; but not Acceptability.GOOD (Run using
	 * {@link edu.toronto.cs.se.ci.GenericCI #applySync(Object, Allowance[])})
	 * 
	 * @throws Exception
	 */
	public void testCI2() throws Exception {
		System.out.println("CI2");
		MLNominalThresholdAcceptor<String> acc = new MLNominalThresholdAcceptor<String>(0.5, 0.95);
		AllSelector<Boolean, String, Void> sel = new AllSelector<Boolean, String, Void>();
		Contract<Boolean, String, Void> contract = new MLToCIContract<Boolean, String>(RepublicanOrDemocrat.class);
		MLWekaNominalConverter<String> conv = new NoActionConverter();
		GenericAggregator<String, String, Void, double[]> agg = new MLWekaNominalAggregator<String>(conv,
				"./vote-consistentNominalsCITrain .arff", new NaiveBayes());

		GenericCI<Boolean, String, String, Void, double[]> test1 = new GenericCI<Boolean, String, String, Void, double[]>(
				contract.provide(), agg, sel, acc);

		Allowance[] budget = new Allowance[] { new Dollars(new BigDecimal(5)) };

		// Estimate<String, double[]> resultEstimate = test1.apply(true,
		// budget);
		// Result<String, double[]> resultResult= resultEstimate.get();
		Result<String, double[]> resultResult = test1.applySync(true, budget);
		System.out.println(resultResult);
		double[] quality = resultResult.getQuality();
		List<Double> list = Doubles.asList(quality);
		System.out.println(list);

		assertEquals("democrat", resultResult.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.61, quality[0], 0.1);
	}

	/**
	 * Testing that the CI throws an exception when it's Result is below
	 * Acceptability.BAD (no sources). (Run using
	 * {@link edu.toronto.cs.se.ci.GenericCI #applySync(Object, Allowance[])})
	 * 
	 * @throws Exception
	 */
	public void testCI4() throws Exception {
		System.out.println("CI4");
		MLNominalThresholdAcceptor<String> acc = new MLNominalThresholdAcceptor<String>(0.75, 0.95);
		AllSelector<Boolean, String, Void> sel = new AllSelector<Boolean, String, Void>();
		Contract<Boolean, String, Void> contract = new MLToCIContract<Boolean, String>(RepublicanOrDemocrat.class);
		MLWekaNominalConverter<String> conv = new NoActionConverter();
		GenericAggregator<String, String, Void, double[]> agg = new MLWekaNominalAggregator<String>(conv,
				"./vote-consistentNominalsCITrain .arff", new NaiveBayes());
		// TODO: fix documentation, MLToCIContract CANNOT be used as a contract
		// to a CI.
		GenericCI<Boolean, String, String, Void, double[]> test1 = new GenericCI<Boolean, String, String, Void, double[]>(
				contract.provide(), agg, sel, acc);

		// TODO: Improve this; right now it times out (as it should), resulting
		// in an undecipherable stack trace as the estimate is of
		// Acceptability.BAD
		Allowance[] budget = new Allowance[] { new Dollars(new BigDecimal(5)) };

		// Estimate<String, double[]> resultEstimate = test1.apply(true,
		// budget);
		// Result<String, double[]> resultResult= resultEstimate.get();

		try {
			Result<String, double[]> resultResult = test1.applySync(true, budget);
			fail(resultResult.toString() + " Either has acceptability that is not BAD, or there is something wrong");
		} catch (ExecutionException e) {
		}
	}

	/**
	 * Tests the CI when Acceptability.GOOD is reached before all sources all
	 * called; using (Run using
	 * {@link edu.toronto.cs.se.ci.GenericCI #applySync(Object, Allowance[])})
	 * 
	 * @throws Exception
	 */
	public void testCI3() throws Exception {
		System.out.println("CI3");
		MLNominalThresholdAcceptor<String> acc = new MLNominalThresholdAcceptor<String>(0.5, 0.99);
		AllSelector<Boolean, String, Void> sel = new AllSelector<Boolean, String, Void>();
		Contract<Boolean, String, Void> contract = new MLToCIContract<Boolean, String>(RepublicanOrDemocrat.class);
		MLWekaNominalConverter<String> conv = new NoActionConverter();
		GenericAggregator<String, String, Void, double[]> agg = new MLWekaNominalAggregator<String>(conv,
				"./vote-consistentNominalsCITrain .arff", new NaiveBayes());

		Contracts.register(new Adoption());
		Contracts.register(new Crime());
		Contracts.register(new DutyFree());
		Contracts.register(new Education());
		Contracts.register(new Handicap());
		Contracts.register(new Immigration());
		Contracts.register(new Missile());
		Contracts.register(new Nicaraguan());
		Contracts.register(new Physician());
		Contracts.register(new Religous());
		Contracts.register(new Salvador());
		Contracts.register(new Satellite());
		Contracts.register(new Superfund());
		Contracts.register(new Synfuel());
		Contracts.register(new WaterProject());
		GenericCI<Boolean, String, String, Void, double[]> test1 = new GenericCI<Boolean, String, String, Void, double[]>(
				contract.provide(), agg, sel, acc);

		Allowance[] budget = new Allowance[] { new Dollars(new BigDecimal(5)) };

		Result<String, double[]> resultResult = test1.applySync(true, budget);
		System.out.println(resultResult);
		double[] quality = resultResult.getQuality();
		List<Double> list = Doubles.asList(quality);
		System.out.println(list);

		assertEquals("democrat", resultResult.getValue());
		assertEquals(2, quality.length);
		assertTrue(quality[0] >= .99);

		resultResult = test1.applySync(false, budget);
		assertEquals("republican", resultResult.getValue());
		quality = resultResult.getQuality();
		assertTrue(quality[1] >= .99);
	}

	/**
	 * Test of CI (using
	 * {@link edu.toronto.cs.se.ci.GenericCI #applySync(Object, Allowance[])})In
	 * which it is impossible to get an acceptability of GOOD; only BAD or OK;
	 * and the final result is considered to be Acceptability.GOOD
	 * 
	 * @throws Exception
	 */
	public void testCI6() throws Exception {
		System.out.println("CI6");
		// A GOOD threshold of 2 cannot be reached
		MLNominalThresholdAcceptor<String> acc = new MLNominalThresholdAcceptor<String>(0.5, 2);
		AllSelector<Boolean, String, Void> sel = new AllSelector<Boolean, String, Void>();
		Contract<Boolean, String, Void> contract = new MLToCIContract<Boolean, String>(RepublicanOrDemocrat.class);
		MLWekaNominalConverter<String> conv = new NoActionConverter();
		GenericAggregator<String, String, Void, double[]> agg = new MLWekaNominalAggregator<String>(conv,
				"./vote-consistentNominalsCITrain .arff", new NaiveBayes());

		Contracts.register(new Adoption());
		Contracts.register(new Crime());
		Contracts.register(new DutyFree());
		Contracts.register(new Education());
		Contracts.register(new Handicap());
		Contracts.register(new Immigration());
		Contracts.register(new Missile());
		Contracts.register(new Nicaraguan());
		Contracts.register(new Physician());
		Contracts.register(new Religous());
		Contracts.register(new Salvador());
		Contracts.register(new Satellite());
		Contracts.register(new Superfund());
		Contracts.register(new Synfuel());
		Contracts.register(new WaterProject());
		GenericCI<Boolean, String, String, Void, double[]> test1 = new GenericCI<Boolean, String, String, Void, double[]>(
				contract.provide(), agg, sel, acc);

		// TODO: Improve this; right now it times out (as it should), resulting
		// in an undecipherable stack trace as the estimate is of
		// Acceptability.BAD
		Allowance[] budget = new Allowance[] { new Dollars(new BigDecimal(5)) };

		// Estimate<String, double[]> resultEstimate = test1.apply(true,
		// budget);
		// Result<String, double[]> resultResult= resultEstimate.get();
		Result<String, double[]> resultResult = test1.applySync(true, budget);
		System.out.println(resultResult);
		double[] quality = resultResult.getQuality();
		List<Double> list = Doubles.asList(quality);
		System.out.println(list);

		assertEquals("democrat", resultResult.getValue());
		assertEquals(2, quality.length);
		assertTrue(quality[0] >= .99);

		resultResult = test1.applySync(false, budget);
		assertEquals("republican", resultResult.getValue());
		quality = resultResult.getQuality();
		assertTrue(quality[1] >= .99);
	}

	/**
	 * Same test as {@link #testCI3()}, but using
	 * {@link edu.toronto.cs.se.ci.GenericCI #apply(Object, Allowance[])}
	 * 
	 * @throws Exception
	 */
	public void testCI5() throws Exception {
		System.out.println("CI5");
		MLNominalThresholdAcceptor<String> acc = new MLNominalThresholdAcceptor<String>(0.5, 0.99);
		AllSelector<Boolean, String, Void> sel = new AllSelector<Boolean, String, Void>();
		Contract<Boolean, String, Void> contract = new MLToCIContract<Boolean, String>(RepublicanOrDemocrat.class);
		MLWekaNominalConverter<String> conv = new NoActionConverter();
		GenericAggregator<String, String, Void, double[]> agg = new MLWekaNominalAggregator<String>(conv,
				"./vote-consistentNominalsCITrain .arff", new NaiveBayes());

		Contracts.register(new Adoption());
		Contracts.register(new Crime());
		Contracts.register(new DutyFree());
		Contracts.register(new Education());
		Contracts.register(new Handicap());
		Contracts.register(new Immigration());
		Contracts.register(new Missile());
		Contracts.register(new Nicaraguan());
		Contracts.register(new Physician());
		Contracts.register(new Religous());
		Contracts.register(new Salvador());
		Contracts.register(new Satellite());
		Contracts.register(new Superfund());
		Contracts.register(new Synfuel());
		Contracts.register(new WaterProject());
		// System.out.println(contract.provide());
		GenericCI<Boolean, String, String, Void, double[]> test1 = new GenericCI<Boolean, String, String, Void, double[]>(
				contract.provide(), agg, sel, acc);

		// TODO: Improve this; right now it times out (as it should), resulting
		// in an undecipherable stack trace as the estimate is of
		// Acceptability.BAD
		Allowance[] budget = new Allowance[] { new Dollars(new BigDecimal(5)) };

		Estimate<String, double[]> resultEstimate = test1.apply(true, budget);
		Result<String, double[]> resultResult = resultEstimate.get();
		System.out.println(resultResult);
		double[] quality = resultResult.getQuality();
		List<Double> list = Doubles.asList(quality);
		System.out.println(list);

		assertEquals("democrat", resultResult.getValue());
		assertEquals(2, quality.length);
		assertTrue(quality[0] >= .99);

		resultEstimate = test1.apply(false, budget);
		resultResult = resultEstimate.get();
		assertEquals("republican", resultResult.getValue());
		quality = resultResult.getQuality();
		assertTrue(quality[1] >= .99);
	}

	public class Handicap extends BasicSource<Boolean, String, Integer> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}

		@Override
		public Integer getTrust(Boolean args, Optional<String> value) {
			return 7;
		}
	}

	public class WaterProject extends MLSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}

		@Override
		public Opinion<String, Void> getOpinion(Boolean input) throws UnknownException {
			if (input) {
				return new Opinion<String, Void>("democrat", null, this);
			}
			return new Opinion<String, Void>("republican", null, this);
		}
	}

	public class Adoption extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Physician extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "democrat";
			}
			return "republican";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Salvador extends Source<Boolean, String, Double> implements RepublicanOrDemocrat {
		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}

		@Override
		public Opinion<String, Double> getOpinion(Boolean input) throws UnknownException {
			if (input) {
				return new Opinion<String, Double>("democrat", 5.62, this);
			}
			return new Opinion<String, Double>("republican", 5.62, this);
		}

		@Override
		public Double getTrust(Boolean args, Optional<String> value) {
			return 5.36;
		}
	}

	public class Religous extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "democrat";
			}
			return "republican";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Satellite extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Nicaraguan extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Missile extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Immigration extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Synfuel extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "democrat";
			}
			return "republican";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Education extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "democrat";
			}
			return "republican";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Superfund extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			fail("The Superfund source has a cost of $9999 which should exceed test funds");
			return null;
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[] { new Dollars(new BigDecimal(9999)) };
		}
	}

	public class Crime extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "democrat";
			}
			return "republican";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class DutyFree extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class Export extends MLBasicSource<Boolean, String> implements RepublicanOrDemocrat {
		@Override
		public String getResponse(Boolean input) throws UnknownException {
			if (input) {
				return "republican";
			}
			return "democrat";
		}

		@Override
		public Expenditure[] getCost(Boolean args) throws Exception {
			return new Expenditure[0];
		}
	}

	public class NoActionConverter implements MLWekaNominalConverter<String> {
		@Override
		public String convert(Opinion<String, Void> sourceOutput) {
			return sourceOutput.getValue();
		}
	}
}
