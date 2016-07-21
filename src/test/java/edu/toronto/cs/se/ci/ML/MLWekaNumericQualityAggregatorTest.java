package edu.toronto.cs.se.ci.ML;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.primitives.Doubles;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNumericConverter;
import edu.toronto.cs.se.ci.machineLearning.util.MLWekaNominalAggregator;
import edu.toronto.cs.se.ci.machineLearning.util.MLWekaNumericAggregator;
import edu.toronto.cs.se.ci.machineLearning.util.MLWekaNumericQualityAggregator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.meta.RegressionByDiscretization;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveByName;

public class MLWekaNumericQualityAggregatorTest extends TestCase {
	List<Opinion<Integer, Void>> instance;

	public MLWekaNumericQualityAggregatorTest() {
		super("MLWekaNumericQualityAggregatorTest");
	}

	public static Test suite() {
		return new TestSuite(MLWekaNumericQualityAggregatorTest.class);
	}

	/**
	 * Creates an instace for use in tests. Specifically it is an instance with
	 * the following values: MYCT: 23 MMIN: 16000 MMAX: 64000 CACH 64 CHMIN 16
	 * CHMAX 32. This instance was removed from the training data, and
	 * originally had a class value of 636.
	 */
	@Override
	public void setUp() {
		// removed instance from training data:
		// 23,16000,64000,64,16,32,636
		int[] values = new int[] { 23, 16000, 64000, 64, 16, 32 };
		String[] opinionNames = new String[] { "MYCT", "MMIN", "MMAX", "CACH", "CHMIN", "CHMAX" };
		instance = arraysToOpinions(values, opinionNames);
	}

	// TODO:Fix serialization problem
	/**
	 * Tests that the getClassifier method returns the classifier being used by
	 * the Aggregator.
	 * 
	 * Has a seriliazation exception. Must check
	 * https://github.com/fommil/matrix-toolkits-java/tree/master/src/test/java/
	 * no/uib/cipr/matrix
	 * 
	 * @throws Exception
	 */
	public void TOFIXtestGetClassifier() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		assertNotNull(agg);

		Classifier copy = agg.getClassifier();
		assertNotNull(copy);
		assertFalse(copy instanceof NaiveBayes);
		assertTrue(copy instanceof GaussianProcesses);
	}

	/**
	 * Tests that the getClassifier method returns the classifier being used by
	 * the Aggregator.
	 * 
	 * @throws Exception
	 */
	public void testGetClassifier2() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new RegressionByDiscretization(),
				0.95);
		assertNotNull(agg);

		Classifier copy = agg.getClassifier();
		assertNotNull(copy);
		assertFalse(copy instanceof NaiveBayes);
		assertTrue(copy instanceof RegressionByDiscretization);
	}

	/**
	 * Tests that {@link MLWekaNumericAggregator #testClassifier(String)}
	 * returns a correct {@link Evaluation} object.
	 * 
	 * @throws Exception
	 */
	public void testTestClassifier() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		assertNotNull(agg);

		Evaluation eval = agg.testClassifier("./target/test-classes/cpuTest.arff");
		// TODO: determine if Weka is SUPPOSED to have this behaviour.
		try {
			eval.confusionMatrix();
			fail();
		} catch (NullPointerException e) {
		}
		assertEquals(0.8756, eval.correlationCoefficient(), 0.0001);
	}

	/**
	 * Tests that {@link MLWekaNumericAggregator #testClassifier(String)}
	 * returns a correct {@link Evaluation} object.
	 * 
	 * @throws Exception
	 */
	public void testTestClassifier2() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new RegressionByDiscretization(),
				0.95);
		assertNotNull(agg);

		Evaluation eval = agg.testClassifier("./target/test-classes/cpuTest.arff");
		// TODO: determine if Weka is SUPPOSED to have this behaviour.
		try {
			eval.confusionMatrix();
			fail();
		} catch (NullPointerException e) {
		}
		assertEquals(0.9342, eval.correlationCoefficient(), 0.0001);
	}

	/**
	 * Creates an {@link MLWekaNumericAggregator #testClassifier(String)} and
	 * uses it to classifier {@link #instance} using a {@link GaussianProcesses}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testValidGaussianProcesses() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		assertNotNull(agg);

		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();

		double[][] quality = result.getQuality();
		assertEquals(423.8, result.getValue(), 20);
		assertEquals(1, quality.length);
	}

	/**
	 * Creates an {@link MLWekaNumericAggregator #testClassifier(String)} and
	 * uses it to classifier {@link #instance} using a
	 * {@link RegressionByDiscretization}.
	 * 
	 * @throws Exception
	 */
	public void testValidRegressionByDiscretization() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new RegressionByDiscretization(),
				0.95);
		assertNotNull(agg);

		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();

		double[][] quality = result.getQuality();
		assertEquals(1066.6, result.getValue(), 300);
		// TODO: Determine how to determine confidence intervals, so as to
		// complete these tests
		assertEquals(4, quality.length);
		System.out.println(arrayToListOfList(quality));

	}

	/**
	 * Confirms that using a classifier that cannot be used for regression on a
	 * dataset where the class value is numeric produces an error. Specifically
	 * it tries using {@link NaiveBayes}.
	 * 
	 * @throws Exception
	 */
	public void testValidNaiveBayes() throws Exception {
		try {
			MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
					new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new NaiveBayes(), 0.95);
			fail(agg.toString()
					+ " Should not have been created, as a bayes classifier does not implement IntervalEstimator");
		} catch (IllegalArgumentException e) {
		}

	}

	/**
	 * Converts arrays of integer values and String opinion names into a List of
	 * opinions, where for opinion n on the list, the Opinion's name is
	 * {@code opinionName[n]} and the opinion's value is {@code value[n]}.
	 */
	private List<Opinion<Integer, Void>> arraysToOpinions(int[] value, String[] opinionName) {
		List<Opinion<Integer, Void>> instance = new ArrayList<Opinion<Integer, Void>>();
		assertTrue("helper method used wrong", value.length == opinionName.length);
		for (int x = 0; x < value.length; x++) {
			instance.add(new Opinion<Integer, Void>(value[x], null, opinionName[x]));
		}
		return instance;
	}

	public class IntegerToDoubleConverter implements MLWekaNumericConverter<Integer> {
		@Override
		public Double convert(Opinion<Integer, Void> sourceOutput) {
			return sourceOutput.getValue().doubleValue();
		}
	}

	/**
	 * Tests the {@link MLWekaNumericAggregator #addFilter(Filter)} method by
	 * adding a single remove filter, and checking that the aggregation suffers
	 * as a result.
	 * 
	 * @throws Exception
	 */
	public void testSingleFilter() throws Exception {
		// creates the aggregator
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new RegressionByDiscretization(),
				0.95);
		assertNotNull(agg);

		// checks the expected value witout the filter
		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(1069.6, result.getValue(), 0.1);

		// creates removal filter
		Filter remove = new Remove();
		String[] options = new String[] { "-R", "2-6" };
		remove.setOptions(options);

		// add remove filter which removed all attributes except for
		// "MYCT" and class
		agg.addFilter(remove);

		// check the quality of the regression has decreased
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		result = resultOpt.get();
		assertEquals(330.8, result.getValue(), 0.1);
	}

	/**
	 * Tests the {@link MLWekaNumericAggregator #addFilter(Filter)} obeys the
	 * correct order of filtering, by removing and then adding an attribute.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterOrder() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		assertNotNull(agg);
		// does not throw an exception
		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);

		// Creates filters
		Filter removeMyct = new Remove();
		removeMyct.setOptions(new String[] { "-R", "1" });

		Filter add = new Add();
		add.setOptions(new String[] { "-T", "NUM", "-C", "first", "-N", "MYCT" });

		// Next checks that adding the add filter results in an error as the
		// MYCT attribute already exists
		try {
			agg.addFilter(add);
			fail("Add filter should result in duplicate attribute");
		} catch (IllegalArgumentException e) {
		}

		// creates new aggregator and again checks that it works correctly
		agg = new MLWekaNumericQualityAggregator<Integer>(new IntegerToDoubleConverter(),
				"./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(423.8, result.getValue(), 0.1);

		// adds remove filter
		agg.addFilter(removeMyct);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);
		// adds MYCT attribute back
		agg.addFilter(add);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);
	}

	/**
	 * Tests the {@link MLWekaNumericAggregator #addFilter(Filter)} correctly
	 * adds filters, by adding 2 filters.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilter() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);

		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(423.8, result.getValue(), 0.1);

		Filter remove1 = new RemoveByName();
		remove1.setOptions(new String[] { "-E", "MYCT" });

		Filter remove2 = new RemoveByName();
		remove2.setOptions(new String[] { "-E", "MMIN" });

		// removes MYCT attribute
		agg.addFilter(remove1);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);
		// removes MMIN attribute
		agg.addFilter(remove2);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(371.8, result.getValue(), 0.1);
	}

	/**
	 * The same test as {@link #testSingleFilter()}, except using
	 * {@link MLWekaNumericAggregator #addFilters(List)} method.
	 * 
	 * @throws Exception
	 */
	public void testSingleFilterAsList() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new RegressionByDiscretization(),
				0.95);
		assertNotNull(agg);

		// checks the expected value witout the filter
		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(1069.6, result.getValue(), 0.1);

		// creates removal filter
		Filter remove = new Remove();
		String[] options = new String[] { "-R", "2-6" };
		remove.setOptions(options);
		List<Filter> allFilters = new ArrayList<Filter>();
		allFilters.add(remove);

		// add remove filter which removed all attributes except for
		// "MYCT" and class
		agg.addFilters(allFilters);

		// check the quality of the regression has decreased
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		result = resultOpt.get();
		assertEquals(330.8, result.getValue(), 0.1);
	}

	/**
	 * The same test as {@link #testTwoFilterOrder()}, except using
	 * {@link MLWekaNumericAggregator #addFilters(List)} method.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterOrderAsListOf1Element() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		assertNotNull(agg);
		// does not throw an exception
		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);

		// Creates filters
		Filter removeMyct = new Remove();
		removeMyct.setOptions(new String[] { "-R", "1" });

		Filter add = new Add();
		add.setOptions(new String[] { "-T", "NUM", "-C", "first", "-N", "MYCT" });

		// Next checks that adding the add filter results in an error as the
		// MYCT attribute already exists
		List<Filter> allFilters = new ArrayList<Filter>();
		allFilters.add(add);
		try {
			agg.addFilters(allFilters);
			fail("Add filter should result in duplicate attribute");
		} catch (IllegalArgumentException e) {
		}

		// creates new aggregator and again checks that it works correctly
		agg = new MLWekaNumericQualityAggregator<Integer>(new IntegerToDoubleConverter(),
				"./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(423.8, result.getValue(), 0.1);

		// adds remove filter
		allFilters = new ArrayList<Filter>();
		allFilters.add(removeMyct);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);
		// adds MYCT attribute back
		allFilters = new ArrayList<Filter>();
		allFilters.add(add);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);
	}

	/**
	 * Similar test to {@link #testTwoFilterOrder()}, except it tests a valid
	 * order and an invalid order of filters as a list of 2 filters using
	 * {@link MLWekaNumericAggregator #addFilters(List)}.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterOrderAsListOf2Elements() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);

		Filter removeMyct = new Remove();
		removeMyct.setOptions(new String[] { "-R", "1" });

		Filter add = new Add();
		add.setOptions(new String[] { "-T", "NUM", "-C", "first", "-N", "MYCT" });

		// Checks that both filters could have been added simultaneously
		List<Filter> allFilters = new ArrayList<Filter>();
		allFilters.add(removeMyct);
		allFilters.add(add);
		agg.addFilters(allFilters);
		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);

		// creates new aggregator and again checks that it works correctly
		agg = new MLWekaNumericQualityAggregator<Integer>(new IntegerToDoubleConverter(),
				"./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());

		// Checks that the filters being placed in the wrong order results in an
		// error
		allFilters = new ArrayList<Filter>();
		allFilters.add(add);
		allFilters.add(removeMyct);
		try {
			agg.addFilters(allFilters);
			fail("Add filter should result in duplicate attribute");
		} catch (IllegalArgumentException e) {
		}
	}

	/**
	 * The same test as {@link #testTwoFilter()}, except using
	 * {@link MLWekaNumericAggregator #addFilters(List)} method.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterAsListOfSingleElement() throws Exception {
		// first checks expected behaviour holds
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);

		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(423.8, result.getValue(), 0.1);

		// Then begins tests with filters
		Filter remove1 = new RemoveByName();
		remove1.setOptions(new String[] { "-E", "MYCT" });

		Filter remove2 = new RemoveByName();
		remove2.setOptions(new String[] { "-E", "MMIN" });

		List<Filter> allFilters = new ArrayList<Filter>();

		// adds remove filter for MYCT
		allFilters.add(remove1);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(386.8, result.getValue(), 0.1);

		// adds remove filter for MMIN
		allFilters = new ArrayList<Filter>();
		allFilters.add(remove2);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(371.8, result.getValue(), 0.1);
	}

	/**
	 * The same test as {@link #testTwoFilter()}, except using
	 * {@link MLWekaNominalAggregator #addFilters(List)} method with lists of 2
	 * Filters.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterAsListOfTwoElements() throws Exception {
		// First checks that expected behaviour of unaltered aggregator holds
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);

		Optional<Result<Double, double[][]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[][]> result = resultOpt.get();
		assertEquals(423.8, result.getValue(), 0.1);

		// Then adds filters and checks if behaviour changes as expected.
		Filter remove1 = new RemoveByName();
		remove1.setOptions(new String[] { "-E", "MYCT" });

		Filter remove2 = new RemoveByName();
		remove2.setOptions(new String[] { "-E", "MMIN" });

		List<Filter> allFilters = new ArrayList<Filter>();

		// Checks adding both filters at the same time, in both orders
		agg = new MLWekaNumericQualityAggregator<Integer>(new IntegerToDoubleConverter(),
				"./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		allFilters = new ArrayList<Filter>();
		allFilters.add(remove1);
		allFilters.add(remove2);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(371.8, result.getValue(), 0.1);

		agg = new MLWekaNumericQualityAggregator<Integer>(new IntegerToDoubleConverter(),
				"./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);
		allFilters = new ArrayList<Filter>();
		allFilters.add(remove2);
		allFilters.add(remove1);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		assertEquals(371.8, result.getValue(), 0.1);
	}

	// TODO: Fix serialization problem
	/**
	 * Serialization Exception. Check
	 * https://github.com/fommil/matrix-toolkits-java/tree/master/src/test/java/
	 * no/uib/cipr/matrix
	 * 
	 * @throws Exception
	 */
	public void TOFIXtestNFoldCrossValidate() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new GaussianProcesses(), 0.95);

		Evaluation result = agg.nFoldCrossValidate(10);

		// TODO: Confirm the following are expected Weka behaviours
		assertEquals(0, result.correct(), .1);
		assertEquals(0, result.incorrect(), .1);

		try {
			result.confusionMatrix();
			fail();
		} catch (NullPointerException e) {
		}

		try {
			result.kappa();
			fail();
		} catch (NullPointerException e) {
		}

		assertEquals(0.89, result.correlationCoefficient(), 0.1);
	}

	public void testNFoldCrossValidate2() throws Exception {
		MLWekaNumericQualityAggregator<Integer> agg = new MLWekaNumericQualityAggregator<Integer>(
				new IntegerToDoubleConverter(), "./target/test-classes/cpu.arff", new RegressionByDiscretization(),
				0.95);

		Evaluation result = agg.nFoldCrossValidate(10);

		// TODO: Confirm the following are expected Weka behaviours
		assertEquals(0, result.correct(), .1);
		assertEquals(0, result.incorrect(), .1);

		try {
			result.confusionMatrix();
			fail();
		} catch (NullPointerException e) {
		}

		try {
			result.kappa();
			fail();
		} catch (NullPointerException e) {
		}

		assertEquals(0.76, result.correlationCoefficient(), 0.1);
	}

	private ArrayList<ArrayList<Double>> arrayToListOfList(double[][] array) {
		assertNotNull(array);
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (double[] distribution : array) {
			result.add(new ArrayList<Double>(Doubles.asList(distribution)));
		}
		return result;
	}

}
