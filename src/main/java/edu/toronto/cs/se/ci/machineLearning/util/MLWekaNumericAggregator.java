package edu.toronto.cs.se.ci.machineLearning.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import com.google.common.base.Optional;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLNumericWekaAggregatorInt;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNumericConverter;

/**
 * This class is a
 * {@link edu.toronto.cs.se.ci.machineLearning.aggregators.MLNominalWekaAggregatorInt}
 * which aggregates nominal values, and returns the {@code String} name of the
 * nominal class which it believes the list of Opinions to be. As most Weka
 * classifiers that do regression do not have a means of deriving confidence,
 * Void is the Quality type. If Quality is absolutely needed, then {@link MLWekaNumericQualityAggregator} should
 * be used.
 * 
 * @author Ian Berlot-Attwell
 * @author wginsberg (Parts of Will's code for PlanIt was refactored and reused)
 *
 * @param <O>
 *            The output type of the sources being aggregated.
 */
public class MLWekaNumericAggregator<O> implements MLNumericWekaAggregatorInt<O, Double, Void> {
	// The converter that converts from O to a String
	private MLWekaNumericConverter<O> converter;
	// The Weka classifier given
	private Classifier classifier;
	// The training data as an Instances object.
	private Instances trainingData;
	// all the filter in the order in which they are to be applied
	private List<Filter> filters;

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
	 *            classifying (aggregating) the opinions given.
	 * @throws Exception
	 */
	public MLWekaNumericAggregator(MLWekaNumericConverter<O> numericConverter, String inputFilePath,
			Classifier classifier) throws Exception {
		this(numericConverter, MLUtility.fileToInstances(inputFilePath), classifier);
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
	 *            classifying (aggregating) the opinions given.
	 * @throws Exception
	 */
	public MLWekaNumericAggregator(MLWekaNumericConverter<O> numericConverter, Instances trainingData,
			Classifier classifier) throws Exception {
		if (numericConverter == null || trainingData == null || classifier == null) {
			throw new IllegalArgumentException("null arguments to the constructor are not acceptable.");
		}

		this.converter = numericConverter;
		this.classifier = classifier;
		this.trainingData = trainingData;
		this.classifier.buildClassifier(trainingData);
		this.filters = new ArrayList<Filter>();
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
	public Optional<Result<Double, Void>> aggregate(List<Opinion<O, Void>> opinions) {
		DenseInstance opinionsAsInstance = convertOpinionsToDenseInstance(opinions);
		// The class distribution (probability of the instance belonging to a
		// specific class)
		double[] distribution = null;
		// The classification determined by the Classifier
		double classification = 0d;

		try {
			distribution = classifier.distributionForInstance(opinionsAsInstance);
			classification = classifier.classifyInstance(opinionsAsInstance);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (distribution == null || distribution.length < 1) {
			return Optional.absent();
		}

		// return the result
		return Optional.of(new Result<Double, Void>(classification, null));
	}

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
		Evaluation result = new Evaluation(trainingData);
		result.evaluateModel(classifier, instances);
		return result;
	}

	// TODO: Try to refactor shared code of Numeric & Nominal aggregators.
	@Override
	public void addFilter(Filter filter) throws Exception {
		FilteredClassifier fc = getFC();

		filters.add(filter);
		MultiFilter mf = new MultiFilter();
		mf.setFilters(filters.toArray(new Filter[] {}));
		fc.setFilter(mf);
		fc.buildClassifier(trainingData);
		this.classifier = fc;

	}

	@Override
	public void addFilters(List<Filter> filters) throws Exception {
		FilteredClassifier fc = getFC();

		this.filters.addAll(filters);
		MultiFilter mf = new MultiFilter();
		mf.setFilters(this.filters.toArray(new Filter[] {}));
		fc.setFilter(mf);
		fc.buildClassifier(trainingData);
		this.classifier = fc;
	}

	/**
	 * If there are no filters, returns a filtered classifier containing the
	 * original {@link #classifier}. Otherwise it returns {@link #classifier}
	 * which should be a FilteredClassifier.
	 */
	private FilteredClassifier getFC() {
		FilteredClassifier fc;
		if (filters.size() == 0) {
			fc = new FilteredClassifier();
			fc.setClassifier(classifier);
		} else {
			assert (classifier instanceof FilteredClassifier);
			fc = (FilteredClassifier) classifier;
		}
		return fc;
	}

	@Override
	public Evaluation nFoldCrossValidate(int n) throws Exception {
		Evaluation result = new Evaluation(trainingData);
		result.crossValidateModel(classifier, trainingData, n, new Random(1));
		return result;
	}

	@Override
	public void retrain(String filePathToTrainingData) throws Exception {
		retrain(MLUtility.fileToInstances(filePathToTrainingData));
	}

	@Override
	public void retrain(Instances trainingData) throws Exception {
		this.trainingData = trainingData;
		classifier.buildClassifier(this.trainingData);
	}
}
