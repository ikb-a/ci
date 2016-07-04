package edu.toronto.cs.se.ci.machineLearning.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLNominalWekaAggregator;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNominalConverter;

/**
 * This class is a
 * {@link edu.toronto.cs.se.ci.machineLearning.aggregators.MLNominalWekaAggregator}
 * which aggregates nominal values, and returns the {@code String} name of the
 * nominal class which it believes the list of Opinions to be.
 * 
 * @author Ian Berlot-Attwell
 * @author wginsberg (Parts of Will's code for PlanIt was refactored and reused)
 *
 * @param <O>
 *            The output type of the sources being aggregated.
 */
public class MLWekaNominalAggregator<O> implements MLNominalWekaAggregator<O, String> {
	// The converter that converts from O to a String
	private MLWekaNominalConverter<O> converter;
	/*
	 * The Weka classifier given. If there are no filters, it should be the
	 * original classifier. Otherwise it should be a FilteredClassifier, with
	 * the original classifier as the classifier, and a Multifilter of all the
	 * filters as the filter.
	 */
	private Classifier classifier;
	// The training data as an Instances object.
	private Instances trainingData;

	private List<Filter> filters;

	/**
	 * Constructs the aggregator using {@code classifier} as the internal
	 * Classifier, and the file located at {@code inputFilePath} as training
	 * data. Note that a classifier that classifies into nominal values (NOT a
	 * Weka classifier which does Regression, and produces a numeric value) must
	 * be used.
	 * 
	 * @param nominalConverter
	 *            The converter to be used to turn values of type O into nominal
	 *            String
	 * @param inputFilePath
	 *            The path of the file containing training data
	 * @param classifier
	 *            The Weka classifier to be trained and to be used in
	 *            classifying (aggregating) the opinions given.
	 * @throws Exception
	 */
	public MLWekaNominalAggregator(MLWekaNominalConverter<O> nominalConverter, String inputFilePath,
			Classifier classifier) throws Exception {
		this(nominalConverter, MLUtility.fileToInstances(inputFilePath), classifier);
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
	public MLWekaNominalAggregator(MLWekaNominalConverter<O> nominalConverter, Instances trainingData,
			Classifier classifier) throws Exception {
		this.converter = nominalConverter;
		this.classifier = classifier;
		this.trainingData = trainingData;
		classifier.buildClassifier(trainingData);
		filters = new ArrayList<Filter>();
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
	// cannot be refactored into MLUtility without copying verbatim, as the
	// setValue method will only accept specific datatypes
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
	public Optional<Result<String, double[]>> aggregate(List<Opinion<O, Void>> opinions) {
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

		// convert the double classification into the String name of the class
		String responseAsString = trainingData.classAttribute().value((int) classification);
		// return the result
		return Optional.of(new Result<String, double[]>(responseAsString, distribution));
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

	//TODO: Determine how to deep copy filters to prevent mutability
	/**
	 * Operating on convention that rebuilding the classifier will have the same
	 * effect as building
	 * 
	 * @param filter
	 * @throws Exception 
	 */
	@Override
	public void addFilter(Filter filter) throws Exception {
		FilteredClassifier fc = getFC();

		filters.add(filter);
		MultiFilter mf = new MultiFilter();
		mf.setFilters(filters.toArray(new Filter[]{}));
		fc.setFilter(mf);
		fc.buildClassifier(trainingData);
		this.classifier = fc;
	}

	@Override
	public void addFilters(List<Filter> filters) throws Exception {
		FilteredClassifier fc = getFC();

		this.filters.addAll(filters);
		MultiFilter mf = new MultiFilter();
		mf.setFilters(this.filters.toArray(new Filter[]{}));
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
}
