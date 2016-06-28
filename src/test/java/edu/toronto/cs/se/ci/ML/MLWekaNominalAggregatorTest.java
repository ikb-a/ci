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

public class MLWekaNominalAggregatorTest extends TestCase {

	public MLWekaNominalAggregatorTest() {
		super("MLWekaNominalAggregatorTest");
	}

	public static Test suite() {
		return new TestSuite(MLWekaNominalAggregatorTest.class);
	}

	public void testValidNaiveBayes() throws Exception {
		MLWekaNominalAggregator<String> agg = new MLWekaNominalAggregator<String>(new NoActionConverter(),
				"./vote-consistentNominals.arff", new NaiveBayes());
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
		Result<String, double []> result= resultOpt.get();
		System.out.println(result.getValue());
		System.out.println("Probability Democrat:"+result.getQuality()[0]);
		System.out.println("Probability Republican:"+result.getQuality()[1]);
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
		public String convert(String sourceOutput) {
			return sourceOutput;
		}
	}
}
