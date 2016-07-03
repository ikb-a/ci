package edu.toronto.cs.se.ci.ML;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNumericConverter;
import edu.toronto.cs.se.ci.machineLearning.util.MLWekaNumericAggregator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.UnsupportedAttributeTypeException;

public class MLWekaNumericAggregatorTest extends TestCase {

	public MLWekaNumericAggregatorTest() {
		super("MLWekaNumericAggregatorTest");
	}

	public static Test suite() {
		return new TestSuite(MLWekaNumericAggregatorTest.class);
	}

	public void testGetClassifier() throws Exception {
		MLWekaNumericAggregator<Integer> agg = new MLWekaNumericAggregator<Integer>(new IntegerToDoubleConverter(),
				"./cpu.arff", new LinearRegression());
		assertNotNull(agg);

		Classifier copy = agg.getClassifier();
		assertNotNull(copy);
		assertFalse(copy instanceof NaiveBayes);
		assertTrue(copy instanceof LinearRegression);
	}

	public void testValidLinearRegression() throws Exception {
		MLWekaNumericAggregator<Integer> agg = new MLWekaNumericAggregator<Integer>(new IntegerToDoubleConverter(),
				"./cpu.arff", new LinearRegression());
		assertNotNull(agg);

		// removed instance from training data:
		// 23,16000,64000,64,16,32,636
		int[] values = new int[] { 23, 16000, 64000, 64, 16, 32 };
		String[] opinionNames = new String[] { "MYCT", "MMIN", "MMAX", "CACH", "CHMIN", "CHMAX" };
		List<Opinion<Integer, Void>> instance = arraysToOpinions(values, opinionNames);

		Optional<Result<Double, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals(636, result.getValue(), 20);
		assertEquals(1, quality.length);
		assertEquals(quality[0], result.getValue(), 0.001); // TODO: Current
															// issue with
															// NumericAggregator
	}

	public void testValidMultilayerPerceptron() throws Exception {
		MLWekaNumericAggregator<Integer> agg = new MLWekaNumericAggregator<Integer>(new IntegerToDoubleConverter(),
				"./cpu.arff", new MultilayerPerceptron());
		assertNotNull(agg);

		// removed instance from training data:
		// 23,16000,64000,64,16,32,636
		int[] values = new int[] { 23, 16000, 64000, 64, 16, 32 };
		String[] opinionNames = new String[] { "MYCT", "MMIN", "MMAX", "CACH", "CHMIN", "CHMAX" };
		List<Opinion<Integer, Void>> instance = arraysToOpinions(values, opinionNames);

		Optional<Result<Double, double[]>> resultOpt = agg.aggregate(instance);
		assertTrue(resultOpt.isPresent());
		Result<Double, double[]> result = resultOpt.get();

		double[] quality = result.getQuality();
		assertEquals(636, result.getValue(), 300);
		assertEquals(1, quality.length);
		assertEquals(quality[0], result.getValue(), 0.001);
	}

	// TODO: Determine if this works for all invalid classifiers, and if
	// increasing userfriendliness is desired. (Regression classifiers do not
	// have their own class/interface)
	public void testValidNaiveBayes() throws Exception {
		try {
			MLWekaNumericAggregator<Integer> agg = new MLWekaNumericAggregator<Integer>(new IntegerToDoubleConverter(),
					"./cpu.arff", new NaiveBayes());
			fail(agg.toString()
					+ " Should not have been created, as a bayes classifier cannot support a numeric datatype");
		} catch (UnsupportedAttributeTypeException e) {
		}

	}

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
}
