package edu.toronto.cs.se.ci.machineLearning.util.training;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import edu.toronto.cs.se.ci.Contract;
import edu.toronto.cs.se.ci.Contracts;
import edu.toronto.cs.se.ci.Provider;
import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.machineLearning.aggregators.MLWekaNominalConverter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class NominalTrainer<I, O> {

	private final ImmutableSet<Source<I, O, Void>> sources;

	public NominalTrainer(Class<? extends Contract<I, O, Void>> contract) {
		this(Contracts.discover(contract));
	}

	public NominalTrainer(Collection<? extends Source<I, O, Void>> sources) {
		if (sources == null) {
			throw new IllegalArgumentException("Null arguments are invalid");
		}
		this.sources = ImmutableSet.copyOf(sources);
	}

	public NominalTrainer(Provider<I, O, Void> provider) {
		if (provider == null) {
			throw new IllegalArgumentException("Null arguments are invalid");
		}
		this.sources = ImmutableSet.copyOf(provider.provide());
	}

	public Instances createNominalTrainingData(Map<String, I[]> trainingData, MLWekaNominalConverter<O> converter,
			String savePath) {
		Set<String> keys = trainingData.keySet();
		// The keys in the map must be the possible nominal output and must
		// match the possible outputs of the MLWekaNominalConverter
		assert keys.size() > 1;

		Map<String, Attribute> mapSourceAttributes = new HashMap<String, Attribute>();
		Attribute classAttribute;
		List<String> classValues = new ArrayList<String>();
		classValues.addAll(keys);

		// For each source create a Nominal named attribute
		for (Source<I, O, Void> s : sources) {
			String sourceName = s.getName();
			mapSourceAttributes.put(sourceName, new Attribute(sourceName, classValues));
		}

		// Create a nominal attribute to function as the Class attribute
		classAttribute = new Attribute("class", classValues);

		/*
		 * defines the feature vector that each instance in the Instances object
		 * will obey
		 */
		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(2);
		featureVector.addAll(mapSourceAttributes.values());
		featureVector.add(classAttribute);

		int numOfInputs = 0;
		// Count # of inputs
		for (String key : keys) {
			I[] inputs = trainingData.get(key);
			numOfInputs += inputs.length;
		}

		/*
		 * Creates the Instances object in which each contained Instance will
		 * obey the feature vector declared above, and which has a capacity
		 * equal to the total number of word bags.
		 */
		Instances trainingDataAsInstances = new Instances("trainingData", featureVector, numOfInputs);
		trainingDataAsInstances.setClass(classAttribute);
		for (String key : keys) {
			I[] inputs = trainingData.get(key);
			for (I input : inputs) {
				Instance i = new DenseInstance(sources.size() + 1);
				i.setDataset(trainingDataAsInstances);
				for (Source<I, O, Void> s : sources) {
					try {
						String finalResult = converter.convert(s.getOpinion(input));
						i.setValue(mapSourceAttributes.get(s.getName()), finalResult);
					} catch (UnknownException e) {
						i.setMissing(mapSourceAttributes.get(s.getName()));
					}
				}
				i.setValue(classAttribute, key);
				trainingDataAsInstances.add(i);
			}
		}

		try {
			ArffSaver saver = new ArffSaver();
			saver.setInstances(trainingDataAsInstances);
			saver.setFile(new File(savePath));
			saver.writeBatch();
		} catch (IOException e) {
			System.err.println("Saving failed. Valid instances will still be returned.");
			e.printStackTrace();
		}

		return trainingDataAsInstances;
	}

}
