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

/**
 * This class accepts training data and sources which produce a nominal output,
 * and uses them to produce a .arff file that can be used to train a nominal ML
 * aggregator for a CI.
 * 
 * @author Ian Berlot-Attwell
 *
 * @param <I>
 *            The input type of the sources that will be used to create the
 *            training data
 * @param <O>
 *            The output type of the sources that will be used to create the
 *            training data
 */
public class NominalTrainer<I, O> {

	/**
	 * The sources that will be queried to produce training data.
	 */
	private final ImmutableSet<Source<I, O, Void>> sources;

	private boolean verbose = true;

	/**
	 * Create a new NominalTrainer by using source discovery on {@code contract}
	 * 
	 * @param contract
	 *            A contract to be used to find the sources for the creation of
	 *            training data.
	 */
	public NominalTrainer(Class<? extends Contract<I, O, Void>> contract) {
		this(Contracts.discover(contract));
	}

	/**
	 * Create a new NominalTrainer using the sources in {@code sources} to
	 * produce training data.
	 * 
	 * @param sources
	 *            A non-{@code null} collection of sources to be queried.
	 */
	public NominalTrainer(Collection<? extends Source<I, O, Void>> sources) {
		if (sources == null) {
			throw new IllegalArgumentException("Null arguments are invalid");
		}
		this.sources = ImmutableSet.copyOf(sources);
	}

	/**
	 * Create a new NominalTrainer using the sources provided by
	 * {@code provider} to produce training data.
	 * 
	 * @param provider
	 *            A provider to produces sources to be queried.
	 */
	public NominalTrainer(Provider<I, O, Void> provider) {
		if (provider == null) {
			throw new IllegalArgumentException("Null arguments are invalid");
		}
		this.sources = ImmutableSet.copyOf(provider.provide());
	}

	/**
	 * Given the examples in {@code trainingData} and the sources given during
	 * construction, return an Instances object containing the expected value of
	 * each example as well as the output of each source, and also save this
	 * object as an .arff to {@code savePath}.
	 * <p>
	 * Depending on the speed of the sources and the number of examples, this
	 * method may take minutes to hours to complete.
	 * <p>
	 * Note that the name of the class attribute produced by this method is
	 * "class", so none of the sources' getName() methods can return "class".
	 * 
	 * @param trainingData
	 *            The training data on which to produce the output. Together,
	 *            the keys of the map should be every valid nominal output of
	 *            the sources represented as a String (i.e. If each source
	 *            returns a boolean, and {@code converter} converts true to "T"
	 *            and false to "F", then the keys of the map must be "T" and "F"
	 *            respectively). The values of the map should be an array of
	 *            inputs that should produce the key as the output. For example,
	 *            continuing the example where the sources return boolean
	 *            outputs, if the sources all answer the question
	 *            "Is the item x blue?" then "T" might map to ["blueberry",
	 *            "ocean", "sky"] whereas "F" may map to ["brick", "concrete",
	 *            "grass"].
	 * @param converter
	 *            The converter used to convert from the type O returned by the
	 *            sources, to the String nominal value used as the keys in
	 *            {@code trainingData}, and as the output values in the training
	 *            data produced.
	 * @param savePath
	 *            The path to save the produced training data.
	 * @return The produces training data as an Instances object. Each source is
	 *         it's own attribute, with the value of the source's getName()
	 *         method being the name of it's attribute.
	 */
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
				if (verbose)
					System.out.println("New Input: " + input);

				Instance i = new DenseInstance(sources.size() + 1);
				i.setDataset(trainingDataAsInstances);
				for (Source<I, O, Void> s : sources) {
					try {
						String finalResult = converter.convert(s.getOpinion(input));
						i.setValue(mapSourceAttributes.get(s.getName()), finalResult);
						if (verbose)
							System.out.println("Source: " + s.getName() + ": " + finalResult);

					} catch (UnknownException e) {
						i.setMissing(mapSourceAttributes.get(s.getName()));
						if (verbose)
							System.out.println("Source: " + s.getName() + ": ?");
					}
				}
				i.setValue(classAttribute, key);
				trainingDataAsInstances.add(i);
			}
		}

		// Save the produced instances object
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
