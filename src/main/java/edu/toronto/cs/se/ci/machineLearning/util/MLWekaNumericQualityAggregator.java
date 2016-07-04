package edu.toronto.cs.se.ci.machineLearning.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLNumericWekaAggregatorInt;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNumericConverter;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.IntervalEstimator;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.MultiFilter;

//TODO: Refactor this and MLWekaNumericAggregator
public class MLWekaNumericQualityAggregator<O> implements MLNumericWekaAggregatorInt<O, Double, double[][]> {
	// The converter that converts from O to a String
	private MLWekaNumericConverter<O> converter;
	// The Weka classifier given
	private Classifier classifier;
	// The training data as an Instances object.
	private Instances trainingData;
	// all the filter in the order in which they are to be applied
	private List<Filter> filters;
	// The fiter
	private MultiFilter filter;

	private double confidenceLevel;

	/**
	 * Constructs the aggregator using {@code classifier} as the internal
	 * Classifier, and the file located at {@code inputFilePath} as training
	 * data. Note that a classifier that classifies into numeric values (a Weka
	 * classifier which does Regression, NOT a classifier that classifies into a
	 * nominal class) must be used.
	 * 
	 * @param numericConverter
	 *            The converter to be used to turn values of type O into numeric
	 *            Double
	 * @param inputFilePath
	 *            The path of the file containing training data
	 * @param classifier
	 *            The Weka classifier to be trained and to be used in
	 *            classifying (aggregating) the opinions given. Must implement
	 *            {@link IntervalEstimator}
	 * @param confidenceLevel
	 *            The confidence level for the confidence intervals produced as
	 *            output
	 * @throws Exception
	 */
	public MLWekaNumericQualityAggregator(MLWekaNumericConverter<O> numericConverter, String inputFilePath,
			Classifier classifier, double confidenceLevel) throws Exception {
		this(numericConverter, MLUtility.fileToInstances(inputFilePath), classifier, confidenceLevel);
	}

	/**
	 * Constructs the aggregator using {@code classifier} as the internal
	 * Classifier, and the Instances object {@code trainingData} as training
	 * data.
	 * 
	 * @param nominalConverter
	 *            The converter to be used to turn values of type O into nominal
	 *            String
	 * @param trainingData
	 *            The Instances to train the classifier on.
	 * @param classifier
	 *            The Weka classifier to be trained and to be used in
	 *            classifying (aggregating) the opinions given. Must implement
	 *            {@link IntervalEstimator}
	 * @param confidenceLevel
	 *            The confidence level for the confidence intervals produced as
	 *            output
	 * @throws Exception
	 */
	public MLWekaNumericQualityAggregator(MLWekaNumericConverter<O> numericConverter, Instances trainingData,
			Classifier classifier, double confidenceLevel) throws Exception {
		if (numericConverter == null || trainingData == null || classifier == null) {
			throw new IllegalArgumentException("null arguments to the constructor are not acceptable.");
		}
		if (!(classifier instanceof IntervalEstimator)) {
			throw new IllegalArgumentException();
		}

		this.converter = numericConverter;
		this.classifier = classifier;
		this.trainingData = trainingData;
		this.classifier.buildClassifier(trainingData);
		this.filters = new ArrayList<Filter>();
		this.confidenceLevel = confidenceLevel;
		this.filter = new MultiFilter();
		this.filter.setInputFormat(trainingData);
	}

	/**
	 * Converts {@code opinions} into a single {@link weka.core.DenseInstance}.
	 * Each individual opinion in {@code opinions} is considered to be a single
	 * attribute, whose name is retrieved using
	 * {@link edu.toronto.cs.se.ci.data.Opinion #getName()}
	 * 
	 * @param opinions
	 * @return {@code opinions} as an {@link weka.core.Instance}.
	 */
	private DenseInstance convertOpinionsToDenseInstance(List<Opinion<O, Void>> opinions) {
		DenseInstance instance = new DenseInstance(trainingData.numAttributes());
		/*
		 * The dataset of instance is set to the training data, so that the name
		 * and types (in this case all should be nominal String) of all
		 * attributes are known.
		 */
		instance.setDataset(trainingData);
		for (Opinion<O, Void> opinion : opinions) {
			Enumeration<Attribute> attributes = instance.enumerateAttributes();
			while (attributes.hasMoreElements()) {
				Attribute attribute = attributes.nextElement();
				/*
				 * For all attributes in the training data whose name matches
				 * the name of the opinion, the value of the opinion is added
				 * into the Instance as the matching attribute.
				 */
				if (attribute.name().equals(opinion.getName())) {
					instance.setValue(attribute, converter.convert(opinion));
					break;
				}
			}
		}
		return instance;
	}

	@Override
	public Optional<Result<Double, double[][]>> aggregate(List<Opinion<O, Void>> opinions) {
		// The class distribution (probability of the instance belonging to a
		// specific class)
		double[] distribution = null;
		// The classification determined by the Classifier
		double classification = 0d;
		// Confidence intervals
		double[][] confidenceInterval = null;

		try {
			DenseInstance opinionsAsInstance = convertOpinionsToDenseInstance(opinions);
			Instances toFilter = new Instances(trainingData, 1);
			toFilter.add(0, opinionsAsInstance);
			Instances filteredOpinionsAsInstances = Filter.useFilter(toFilter, this.filter);
			Instance filteredOpinionsAsInstance = filteredOpinionsAsInstances.get(0);

			distribution = classifier.distributionForInstance(filteredOpinionsAsInstance);
			classification = classifier.classifyInstance(filteredOpinionsAsInstance);
			IntervalEstimator estimator = (IntervalEstimator) classifier;
			confidenceInterval = estimator.predictIntervals(filteredOpinionsAsInstance, confidenceLevel);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (distribution == null || distribution.length < 1) {
			return Optional.absent();
		}

		// return the result
		return Optional.of(new Result<Double, double[][]>(classification, confidenceInterval));
	}

	/**
	 * Returns the classifier, not the filter.
	 */
	@Override
	public Classifier getClassifier() throws Exception {
		return AbstractClassifier.makeCopy(classifier);
	}

	@Override
	public Evaluation testClassifier(String filePath) throws Exception {
		return testClassifier(MLUtility.fileToInstances(filePath));
	}

	@Override
	public Evaluation testClassifier(Instances instances) throws Exception {
		Instances filteredInstances = Filter.useFilter(instances, this.filter);
		Evaluation result = new Evaluation(filteredInstances);
		result.evaluateModel(classifier, filteredInstances);
		return result;
	}

	@Override
	public void addFilter(Filter filter) throws Exception {
		filters.add(filter);
		MultiFilter mf = new MultiFilter();
		mf.setFilters(filters.toArray(new Filter[] {}));
		mf.setInputFormat(trainingData);
		this.filter = mf;
		retrain(trainingData);
	}

	@Override
	public void addFilters(List<Filter> filters) throws Exception {
		this.filters.addAll(filters);
		MultiFilter mf = new MultiFilter();
		mf.setFilters(this.filters.toArray(new Filter[] {}));
		mf.setInputFormat(trainingData);
		this.filter = mf;
		retrain(trainingData);
	}

	@Override
	public Evaluation nFoldCrossValidate(int n) throws Exception {
		Instances filteredTrainingData = Filter.useFilter(this.trainingData, this.filter);
		Evaluation result = new Evaluation(filteredTrainingData);
		result.crossValidateModel(classifier, filteredTrainingData, n, new Random(1));
		return result;
	}

	@Override
	public void retrain(String filePathToTrainingData) throws Exception {
		retrain(MLUtility.fileToInstances(filePathToTrainingData));
	}

	@Override
	public void retrain(Instances trainingData) throws Exception {
		this.trainingData = trainingData;
		Instances filteredTrainingData = Filter.useFilter(this.trainingData, this.filter);
		classifier.buildClassifier(filteredTrainingData);
	}
}
