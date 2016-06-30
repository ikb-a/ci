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
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.J48;
import weka.core.UnsupportedAttributeTypeException;

public class MLWekaNominalAggregatorTest extends TestCase {

	public MLWekaNominalAggregatorTest() {
		super("MLWekaNominalAggregatorTest");
	}

	public static Test suite() {
		return new TestSuite(MLWekaNominalAggregatorTest.class);
	}

	public void testValidNaiveBayes() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominalsTrain.arff", new NaiveBayes());
		assertNotNull(agg);

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
		List<Opinion<String, Void>> instance = arraysToOpinions(values, opinionNames);

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
		List<Opinion<String, Void>> instance = arraysToOpinions(values, opinionNames);

		Optional<Result<String, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<String, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals("democrat", result.getValue());
		assertEquals(2, quality.length);
		assertEquals(1.0, quality[0] + quality[1]);
		System.out.println(quality[0]);
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
