package edu.toronto.cs.se.ci.machineLearning.util;

import java.util.Enumeration;
import java.util.List;

import com.google.common.base.Optional;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.data.Result;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLNominalWekaAggregator;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNominalConverter;

/**
 * 
 * @author Ian Berlot-Attwell
 * @author wginsberg (Parts of Will's code for PlanIt was refactored and reused)
 *
 * @param <O>
 */
public class MLWekaNominalAggregator<O> implements MLNominalWekaAggregator<O, String> {
	private MLWekaNominalConverter<O> converter;
	private Classifier classifier;
	private Instances trainingData;

	public MLWekaNominalAggregator(MLWekaNominalConverter<O> nominalConverter, String inputFilePath,
			Classifier classifier) throws Exception {
		this.converter = nominalConverter;
		this.classifier = classifier;
		this.trainingData = MLUtility.fileToInstances(inputFilePath);
		classifier.buildClassifier(trainingData);
	}

	public MLWekaNominalAggregator(MLWekaNominalConverter<O> nominalConverter, Instances trainingData,
			Classifier classifier) throws Exception {
		this.converter = nominalConverter;
		this.classifier = classifier;
		this.trainingData = trainingData;
		classifier.buildClassifier(trainingData);
	}

	// TODO: Refactor into MLUtility?
	private DenseInstance convertOpinionsToDenseInstance(List<Opinion<O, Void>> opinions) {
		DenseInstance instance = new DenseInstance(opinions.size());
		instance.setDataset(trainingData);
		for (Opinion<O, Void> opinion : opinions) {
			Enumeration<Attribute> attributes = instance.enumerateAttributes();
			while (attributes.hasMoreElements()) {
				Attribute attribute = attributes.nextElement();
				if (attribute.name().equals(opinion.getName())) {
					instance.setValue(attribute, converter.convert(opinion.getValue()));
				}
			}
		}
		return instance;
	}

	@Override
	public Optional<Result<String, double[]>> aggregate(List<Opinion<O, Void>> opinions) {
		DenseInstance opinionsAsInstance = convertOpinionsToDenseInstance(opinions);
		double[] distribution = null;
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
		String responseAsString = trainingData.classAttribute().value((int) classification);
		return Optional.of(new Result<String, double[]>(responseAsString, distribution));
	}

}
