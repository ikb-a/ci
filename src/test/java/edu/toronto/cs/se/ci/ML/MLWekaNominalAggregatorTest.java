package edu.toronto.cs.se.ci.ML;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNominalConverter;
import edu.toronto.cs.se.ci.machineLearning.util.MLWekaNominalAggregator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.J48;
import weka.core.UnsupportedAttributeTypeException;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveByName;

public class MLWekaNominalAggregatorTest extends TestCase {
	List<Opinion<String, Void>> instance;

	public MLWekaNominalAggregatorTest() {
		super("MLWekaNominalAggregatorTest");
	}

	public static Test suite() {
		return new TestSuite(MLWekaNominalAggregatorTest.class);
	}

	@Override
	public void setUp() {
		// removed instance from training data:
		// 'republican','democrat','republican','democrat','democrat','democrat','republican','republican','republican','republican','democrat','democrat',?,'democrat','republican','republican','democrat'
		// 15 attributes (the last is the classification, and one is missing)
		String[] values = new String[] { "republican", "democrat", "republican", "democrat", "democrat", "democrat",
				"republican", "republican", "republican", "republican", "democrat", "democrat", "democrat",
				"republican", "republican" };
		String[] opinionNames = new String[] { "handicapped-infants", "water-project-cost-sharing",
				"adoption-of-the-budget-resolution", "physician-fee-freeze", "el-salvador-aid",
				"religious-groups-in-schools", "anti-satellite-test-ban", "aid-to-nicaraguan-contras", "mx-missile",
				"immigration", "synfuels-corporation-cutback", "education-spending", "crime", "duty-free-exports",
				"export-administration-act-south-africa" };
		instance = arraysToOpinions(values, opinionNames);
	}

	public void testGetClassifier() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());
		assertNotNull(agg);

		Classifier copy = agg.getClassifier();
		assertNotNull(copy);
		assertTrue(copy instanceof NaiveBayes);
		assertFalse(copy instanceof J48);
		assertFalse(copy instanceof GaussianProcesses);
	}

	public void testTestClassifier() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());
		assertNotNull(agg);

		Evaluation eval = agg.testClassifier("./vote-consistentNominalsTest.arff");
		// System.out.println(eval.toSummaryString());

		// asserts 3 instances were classified correctly
		assertEquals(3, eval.correct(), 0.0001);
		// assert that one of the 4 instances was misclassified or unclassified
		assertEquals(1, eval.incorrect(), 0.0001);
		/*
		 * first value in matrix is the row (corresponds to actual
		 * classification; 0 being democrat and 1 being republican, due to the
		 * order they are stated in in the arff file); the second value is the
		 * column (corresponding to what they were classified as by the
		 * classifier. Same meaning for 1 and 0 apply).
		 */
		double[][] confusionMatrix = eval.confusionMatrix();
		assertEquals(2, confusionMatrix.length);
		assertEquals(2, confusionMatrix[0].length);
		assertEquals(2, confusionMatrix[1].length);
		// asserts 2 democrats were classified as democrats
		assertEquals(2, confusionMatrix[0][0], 0.00001);
		// asserts 1 republican was classified as a republican
		assertEquals(1, confusionMatrix[1][1], 0.00001);
		// asserts one democrat was classified as a republican
		assertEquals(1, confusionMatrix[0][1], 0.00001);
		// asserts no republicans where classified as democrats
		assertEquals(0, confusionMatrix[1][0], 0.00001);
	}

	public void testValidNaiveBayes() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());
		assertNotNull(agg);

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		assertTrue(quality[0] > .999);
	}

	public void testValidJ48() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());
		assertNotNull(agg);

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		assertTrue(quality[0] >= .985);
		assertTrue(quality[0] < .986);
	}

	// TODO: Determine if this works for all invalid classifiers, and if
	// increasing userfriendliness is desired. (Regression classifiers do not
	// have their own class/interface)
	public void testValidLinearRegression() throws Exception {
		try {
			MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
					"./vote-consistentNominalsTrain.arff", new LinearRegression());
			fail(agg.toString()
					+ " Should not have been created, as a Regression classifier cannot support a nominal datatype");
		} catch (UnsupportedAttributeTypeException e) {
		}

	}

	/**
	 * Tests the {@link MLWekaNominalAggregator #addFilter(Filter)} method by
	 * adding a single remove filter, and checking that the aggregation suffers
	 * as a result.
	 * 
	 * @throws Exception
	 */
	public void testSingleFilter() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());

		Filter remove = new Remove();
		remove.setDebug(true);
		String[] options = new String[] { "-R", "1,3-16" };
		remove.setOptions(options);

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		assertTrue(quality[0] > .999);

		// add remove filter which removed all attributes except for
		// "water-project-cost-sharing" and class
		agg.addFilter(remove);

		// check the quality of the aggregation has decreased
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		assertEquals(0.61, quality[0], 0.01);
	}

	/**
	 * Tests the {@link MLWekaNominalAggregator #addFilter(Filter)} obeys the
	 * correct order of filtering, by removing and then adding an attribute.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterOrder() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());

		Filter removePhysician = new Remove();
		removePhysician.setOptions(new String[] { "-R", "4" });

		Filter add = new Add();
		add.setOptions(new String[] { "-T", "NOM", "-C", "first", "-N", "physician-fee-freeze" });

		// does not throw an exception
		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);

		// Next checks that adding the add filter for an existing attribute
		// results in an error
		try {
			agg.addFilter(add);
			fail("Add filter should result in duplicate attribute");
		} catch (IllegalArgumentException e) {
		}

		// creates new aggregator and again checks that it works correctly
		agg = new MLWekaNominalAggregator<String>(new NoActionConverter(), "./vote-consistentNominalsTrain.arff",
				new J48());
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();
		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.985, quality[0], 0.001);

		// adds remove filter
		agg.addFilter(removePhysician);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);
		// adds physician-fee-freeze attribute back
		agg.addFilter(add);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);
	}

	/**
	 * Tests the {@link MLWekaNominalAggregator #addFilter(Filter)} correctly
	 * adds filters, by adding 2 filters.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilter() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());

		Filter remove1 = new RemoveByName();
		remove1.setOptions(new String[] { "-E", "physician-fee-freeze" });

		Filter remove2 = new RemoveByName();
		remove2.setOptions(new String[] { "-E", "crime" });

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();
		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.985, quality[0], 0.001);

		// adds remove filter for physician-fee-freeze
		agg.addFilter(remove1);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);
		// adds remove filter for crime
		agg.addFilter(remove2);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.972, quality[0], 0.001);
	}

	/**
	 * The same test as {@link #testSingleFilter()}, except using
	 * {@link MLWekaNominalAggregator #addFilters(List)} method.
	 * 
	 * @throws Exception
	 */
	public void testSingleFilterAsList() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());

		Filter remove = new Remove();
		remove.setDebug(true);
		remove.setOptions(new String[] { "-R", "1,3-16" });

		List<Filter> allFilters = new ArrayList<Filter>();
		allFilters.add(remove);

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		assertTrue(quality[0] > .999);

		// add remove filter which removed all attributes except for
		// "water-project-cost-sharing" and class
		agg.addFilters(allFilters);

		// check the quality of the aggregation has decreased
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		assertEquals(0.61, quality[0], 0.01);
	}

	/**
	 * The same test as {@link #testTwoFilterOrder()}, except using
	 * {@link MLWekaNominalAggregator #addFilters(List)} method.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterOrderAsListOf1Element() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());

		Filter removePhysician = new Remove();
		removePhysician.setOptions(new String[] { "-R", "4" });

		Filter add = new Add();
		add.setOptions(new String[] { "-T", "NOM", "-C", "first", "-N", "physician-fee-freeze" });

		// does not throw an exception
		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);

		// Next checks that adding the add filter for an existing attribute
		// results in an error
		List<Filter> allFilters = new ArrayList<Filter>();
		allFilters.add(add);
		try {
			agg.addFilters(allFilters);
			fail("Add filter should result in duplicate attribute");
		} catch (IllegalArgumentException e) {
		}

		// creates new aggregator and again checks that it works correctly
		agg = new MLWekaNominalAggregator<String>(new NoActionConverter(), "./vote-consistentNominalsTrain.arff",
				new J48());
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();
		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.985, quality[0], 0.001);

		// adds remove filter
		allFilters = new ArrayList<Filter>();
		allFilters.add(removePhysician);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);

		// allFilters has not changed
		assertEquals(1, allFilters.size());
		assertTrue(allFilters.get(0) instanceof Remove);

		// adds physician-fee-freeze attribute back
		allFilters = new ArrayList<Filter>();
		allFilters.add(add);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);
	}

	/**
	 * Similar test to {@link #testTwoFilterOrder()}, except it tests a valid
	 * order and an invalid order of filters as a list of 2 filters using
	 * {@link MLWekaNominalAggregator #addFilters(List)}.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterOrderAsListOf2Elements() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());

		Filter removePhysician = new Remove();
		removePhysician.setOptions(new String[] { "-R", "4" });

		Filter add = new Add();
		add.setOptions(new String[] { "-T", "NOM", "-C", "first", "-N", "physician-fee-freeze" });

		// Checks that both filters could have been added simultaneously
		List<Filter> allFilters = new ArrayList<Filter>();
		allFilters.add(removePhysician);
		allFilters.add(add);
		agg.addFilters(allFilters);
		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		Result<String, double[]> result = resultOpt.get();
		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);

		// creates new aggregator and again checks that it works correctly
		agg = new MLWekaNominalAggregator<String>(new NoActionConverter(), "./vote-consistentNominalsTrain.arff",
				new J48());
		resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());

		// Checks that the filters being placed in the wrong order results in an
		// error
		allFilters = new ArrayList<Filter>();
		allFilters.add(add);
		allFilters.add(removePhysician);
		try {
			agg.addFilters(allFilters);
			fail("Add filter should result in duplicate attribute");
		} catch (IllegalArgumentException e) {
		}
	}

	/**
	 * The same test as {@link #testTwoFilter()}, except using
	 * {@link MLWekaNominalAggregator #addFilters(List)} method.
	 * 
	 * @throws Exception
	 */
	public void testTwoFilterAsListOfSingleElement() throws Exception {
		// first checks expected behaviour holds
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();
		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.985, quality[0], 0.001);

		// Then begins tests with filters
		Filter remove1 = new RemoveByName();
		remove1.setOptions(new String[] { "-E", "physician-fee-freeze" });

		Filter remove2 = new RemoveByName();
		remove2.setOptions(new String[] { "-E", "crime" });

		List<Filter> allFilters = new ArrayList<Filter>();

		// adds remove1 filter (physician fee freeze)
		allFilters.add(remove1);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.99, quality[0], 0.001);

		// adds remove1 filter (crime)
		allFilters = new ArrayList<Filter>();
		allFilters.add(remove2);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.972, quality[0], 0.001);
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
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new J48());

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();
		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.985, quality[0], 0.001);

		// Then adds filters and checks if behaviour changes as expected.
		Filter remove1 = new RemoveByName();
		remove1.setOptions(new String[] { "-E", "physician-fee-freeze" });

		Filter remove2 = new RemoveByName();
		remove2.setOptions(new String[] { "-E", "crime" });

		List<Filter> allFilters = new ArrayList<Filter>();

		// Checks adding both filters at the same time, in both orders
		agg = new MLWekaNominalAggregator<String>(new NoActionConverter(), "./vote-consistentNominalsTrain.arff",
				new J48());
		allFilters = new ArrayList<Filter>();
		allFilters.add(remove1);
		allFilters.add(remove2);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.972, quality[0], 0.001);

		agg = new MLWekaNominalAggregator<String>(new NoActionConverter(), "./vote-consistentNominalsTrain.arff",
				new J48());
		allFilters = new ArrayList<Filter>();
		allFilters.add(remove2);
		allFilters.add(remove1);
		agg.addFilters(allFilters);
		resultOpt = agg.aggregate(instance);
		result = resultOpt.get();
		quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(0.972, quality[0], 0.001);
	}

	public void testNFoldCrossValidate() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());

		Evaluation result = agg.nFoldCrossValidate(10);
		// some variation occurs as the cros validation has a random seed
		assertEquals(390, result.correct(), 10);
		assertEquals(44, result.incorrect(), 10);
		double[][] confusionMatrix = result.confusionMatrix();
		assertEquals(236, confusionMatrix[0][0], 10);
		assertEquals(154, confusionMatrix[1][1], 10);
		assertEquals(30, confusionMatrix[0][1], 10);
		assertEquals(14, confusionMatrix[1][0], 10);
		assertEquals(0.79, result.kappa(), 0.1);
	}

	private List<Opinion<String, Void>> arraysToOpinions(String[] value, String[] opinionName) {
		List<Opinion<String, Void>> instance = new ArrayList<Opinion<String, Void>>();
		assertTrue("helper method used wrong", value.length == opinionName.length);
		for (int x = 0; x < value.length; x++) {
			instance.add(new Opinion<String, Void>(value[x], null, opinionName[x]));
		}
		return instance;
	}

	public class NoActionConverter implements MLWekaNominalConverter<String> {
		@Override
		public String convert(Opinion<String, Void> sourceOutput) {
			return sourceOutput.getValue();
		}
	}
}
