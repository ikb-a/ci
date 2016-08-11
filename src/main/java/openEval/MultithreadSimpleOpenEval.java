package openEval;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.utils.searchEngine.GenericSearchEngine;
import edu.toronto.cs.se.ci.utils.searchEngine.GoogleCSESearchJSON;
import edu.toronto.cs.se.ci.utils.searchEngine.SearchResult;
import edu.toronto.cs.se.ci.utils.searchEngine.SearchResults;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.supervised.instance.ClassBalancer;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 * A simplified version of OpenEval. This class can answer predicates by being
 * given positive and negative examples. To increase speed, this class uses
 * multiple threads so as to request information from websites while waiting for
 * the response from the search engine.
 * 
 * @author Ian Berlot-Attwell.
 *
 */
public class MultithreadSimpleOpenEval extends Source<String, Boolean, Double> {
	/**
	 * The search engine being used. This search engine will search the keyword
	 * followed by the arguments to the predicate, and the produced links are
	 * used. This variable cannot be {@code null}.
	 */
	GenericSearchEngine search;
	/**
	 * The keyword being used to prepend the arguments to the predicate. Must be
	 * a single word. Cannot be {@code null}. Can be {@code ""} (the empty
	 * string).
	 */
	String keyword;
	/**
	 * The WEKA filter being used to convert the word bags extracted from the
	 * text corpus (which in turn was extracted from the links produced by the
	 * {@link #search}) into word frequencies (specifically the number of
	 * occurrences of each word in the word bag).
	 */
	StringToWordVector filter;
	/**
	 * The training data before it has been passed to the filter. The instance
	 * should have 2 attributes: "corpus" (a Weka String attribute) and "class"
	 * (a Weka Nominal attribute with values of either "true" or "false"). The
	 * attribute called "class" should be the WEKA class attribute, and as a
	 * result should be the last attribute in the instance (and in the
	 * corresponding .arff file).
	 */
	Instances unfilteredTrainingData;
	/**
	 * The training data after it has been passed through {@link #filter}. Batch
	 * filtering must be used with this filter so that the training data and the
	 * test/actual data have the same dictionary of words.
	 */
	Instances trainingData;
	/**
	 * The classifier being used to classify the frequencies of words in word
	 * bags into either positive or negative word bags.
	 */
	Classifier classifier;

	/**
	 * The number of pages of results to check using {@link search}. As
	 * different search engines have different limits to the number of pages
	 * that can be requested, this variable must be between 1 and 10 inclusive.
	 */
	int pagesToCheck = 1;

	/**
	 * This suffix is appended to the name of the SimpleOpenEval. When creating
	 * a CI with a ML aggregator, each SimpleOpenEval must have a distinct name
	 * suffix.
	 */
	String nameSuffix = "";

	/**
	 * Whether or not to memoize link contents
	 */
	boolean memoizeLinkContents = false;

	/**
	 * Whether or not to print debug messages
	 */
	boolean verbose = false;

	/**
	 * Map from link name to link contents, only used if
	 * {@link #memoizeLinkContents} is true.
	 */
	ConcurrentHashMap<String, String> memoizedLinkContents;

	/**
	 * The name of the class attribute in the Weka data (both in the training
	 * data, and in the Weka instance objects used in evaluation).
	 */
	public static final String classAttributeName = "Class_Attribute_For_SimpleOpenEval";

	/**
	 * The path to which any memoized data is saved
	 */
	private String linkContentsPath;

	/**
	 * The number of link-content reading threads to create. Increasing the
	 * number of link reading threads may accelerate training and evaluation,
	 * but it is limited by available memory, and ultimately by internet speed
	 * and bandwidth.
	 */
	public static final int numOfLinkThreads = 8;

	/**
	 * Creates a new MultithreadSimpleOpenEval. This may take some time.
	 * 
	 * @param positiveExamples
	 *            A list of positive arguments for the predicate being defined.
	 *            For example, if this SimpleOpenEval is meant to represent the
	 *            predicate isBlue(item) then some examples may be "water",
	 *            "sky", "ocean", "saphire", etc...
	 * @param negativeExamples
	 *            A list of negative examples for the predicate being defined.
	 *            Following from the above IsBlue(item) predicate example, some
	 *            negative examples might include "grass", "sun", "mayonnaise",
	 *            "mushroom", "pavement", etc...
	 * @param keyword
	 *            The keyword to be used in searching. This value cannot be
	 *            {@code null}, but it can be the empty string. It also must be
	 *            a single word. For example, the predicate IsBlue(item) might
	 *            use "colour" as a keyword.
	 * @throws Exception
	 *             An exception can be thrown if: WEKA could not set the options
	 *             on the {@link #filter}, WEKA could not apply the filter on
	 *             the word bags, or if WEKA could not train the classifier on
	 *             the produced word frequency data.
	 */
	public MultithreadSimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword)
			throws Exception {
		// search = new BingSearchJSON();

		if (this.memoizeLinkContents) {
			try {
				this.memoizedLinkContents = loadMemoizedContents(linkContentsPath);
			} catch (EOFException e) {
				this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
		}
		search = new GoogleCSESearchJSON();

		// Creates a SMO classifier
		SMO smo = new SMO();
		// sets the classifier to give meaningful probabilities
		String[] SMOOptions = new String[] { "-M" };
		smo.setOptions(SMOOptions);
		// Adds a class balancing filter which will balance the
		// weight of classes for training
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(smo);
		fc.setFilter(new ClassBalancer());
		classifier = fc;

		this.keyword = keyword;
		filter = new StringToWordVector();
		/*
		 * Sets the filter so that it produces the Count of each word, rather
		 * than it's presence (-C). Also sets the filter to consider all words
		 * in Lower case (-L).
		 */
		String[] options = new String[] { "-C", "-L" };
		filter.setOptions(options);
		/*
		 * Uses the positive and negative training data to create an instances
		 * object where each instance is a word bag and whether the word bag was
		 * derived from a positive or negative example
		 */
		this.unfilteredTrainingData = createTrainingData(positiveExamples, negativeExamples);
		this.unfilteredTrainingData.setClass(this.unfilteredTrainingData.attribute(classAttributeName));
		// convert the instance of word bags to instances of word counts
		this.trainingData = wordBagsToWordFrequencies(this.unfilteredTrainingData);
		this.trainingData.setClass(this.trainingData.attribute(classAttributeName));
		classifier.buildClassifier(this.trainingData);
	}

	/**
	 * Creates a new SimpleOpenEval. This may take some time.
	 * 
	 * @param positiveExamples
	 *            A list of positive arguments for the predicate being defined.
	 *            For example, if this SimpleOpenEval is meant to represent the
	 *            predicate isBlue(item) then some examples may be "water",
	 *            "sky", "ocean", "saphire", etc...
	 * @param negativeExamples
	 *            A list of negative examples for the predicate being defined.
	 *            Following from the above IsBlue(item) predicate example, some
	 *            negative examples might include "grass", "sun", "mayonnaise",
	 *            "mushroom", "pavement", etc...
	 * @param keyword
	 *            The keyword to be used in searching. This value cannot be
	 *            {@code null}, but it can be the empty string. It also must be
	 *            a single word. For example, the predicate IsBlue(item) might
	 *            use "colour" as a keyword.
	 * @param pathToSaveTrainingData
	 *            This is a path to save the word bag data. This file can then
	 *            be used with {@link #SimpleOpenEval(Instances, String)} to
	 *            circumvent the need to search all of examples again.
	 * @throws Exception
	 *             An exception can be thrown if: WEKA could not set the options
	 *             on the {@link #filter}, WEKA could not save the word bag
	 *             data, WEKA could not apply the filter on the word bags, or if
	 *             WEKA could not train the classifier on the produced word
	 *             frequency data.
	 */
	public MultithreadSimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword,
			String pathToSaveTrainingData) throws Exception {
		// search = new BingSearchJSON();
		if (this.memoizeLinkContents) {
			try {
				this.memoizedLinkContents = loadMemoizedContents(linkContentsPath);
			} catch (EOFException e) {
				this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
		}
		search = new GoogleCSESearchJSON();

		SMO smo = new SMO();
		String[] SMOOptions = new String[] { "-M" };
		smo.setOptions(SMOOptions);
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(smo);
		fc.setFilter(new ClassBalancer());
		classifier = fc;

		this.keyword = keyword;
		// creates word bag data
		this.unfilteredTrainingData = createTrainingData(positiveExamples, negativeExamples);
		this.unfilteredTrainingData.setClass(this.unfilteredTrainingData.attribute(classAttributeName));

		// Save the word bag data
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.unfilteredTrainingData);
		saver.setFile(new File(pathToSaveTrainingData));
		saver.writeBatch();

		filter = new StringToWordVector();
		String[] options = new String[] { "-C", "-L" };
		filter.setOptions(options);
		this.trainingData = wordBagsToWordFrequencies(this.unfilteredTrainingData);
		this.trainingData.setClass(this.trainingData.attribute(classAttributeName));

		classifier.buildClassifier(this.trainingData);
	}

	/**
	 * Creates a new SimpleOpenEval. This may take some time.
	 * 
	 * @param positiveExamples
	 *            A list of positive arguments for the predicate being defined.
	 *            For example, if this SimpleOpenEval is meant to represent the
	 *            predicate isBlue(item) then some examples may be "water",
	 *            "sky", "ocean", "saphire", etc...
	 * @param negativeExamples
	 *            A list of negative examples for the predicate being defined.
	 *            Following from the above IsBlue(item) predicate example, some
	 *            negative examples might include "grass", "sun", "mayonnaise",
	 *            "mushroom", "pavement", etc...
	 * @param keyword
	 *            The keyword to be used in searching. This value cannot be
	 *            {@code null}, but it can be the empty string. It also must be
	 *            a single word. For example, the predicate IsBlue(item) might
	 *            use "colour" as a keyword.
	 * @param pathToSaveTrainingData
	 *            This is a path to save the word bag data. This file can then
	 *            be used with {@link #SimpleOpenEval(Instances, String)} to
	 *            circumvent the need to search all of examples again.
	 * @param search
	 *            This search engine is used in place of the default search
	 *            engine
	 * @throws Exception
	 *             An exception can be thrown if: WEKA could not set the options
	 *             on the {@link #filter}, WEKA could not save the word bag
	 *             data, WEKA could not apply the filter on the word bags, or if
	 *             WEKA could not train the classifier on the produced word
	 *             frequency data.
	 */
	public MultithreadSimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword,
			String pathToSaveTrainingData, GenericSearchEngine search) throws Exception {
		if (this.memoizeLinkContents) {
			try {
				this.memoizedLinkContents = loadMemoizedContents(linkContentsPath);
			} catch (EOFException e) {
				this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
		}
		this.search = search;

		SMO smo = new SMO();
		String[] SMOOptions = new String[] { "-M" };
		smo.setOptions(SMOOptions);
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(smo);
		fc.setFilter(new ClassBalancer());
		classifier = fc;

		this.keyword = keyword;
		// creates word bag data
		if (verbose)
			System.out.println("M. Creating Training Data");
		this.unfilteredTrainingData = createTrainingData(positiveExamples, negativeExamples);
		this.unfilteredTrainingData.setClass(this.unfilteredTrainingData.attribute(classAttributeName));
		if (verbose)
			System.out.println("M. Saving unfiltered data");
		// Save the word bag data
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.unfilteredTrainingData);
		saver.setFile(new File(pathToSaveTrainingData));
		saver.writeBatch();

		if (verbose)
			System.out.println("M. Filtering data");
		filter = new StringToWordVector();
		String[] options = new String[] { "-C", "-L" };
		filter.setOptions(options);
		this.trainingData = wordBagsToWordFrequencies(this.unfilteredTrainingData);
		this.trainingData.setClass(this.trainingData.attribute(classAttributeName));

		if (verbose)
			System.out.println("M. Training Classifier");
		classifier.buildClassifier(this.trainingData);
		if (verbose)
			System.out.println("M. done");
	}

	/**
	 * Creates a new SimpleOpenEval. This constructor enables the memoization of
	 * link contents. This may take some time.
	 * 
	 * @param positiveExamples
	 *            A list of positive arguments for the predicate being defined.
	 *            For example, if this SimpleOpenEval is meant to represent the
	 *            predicate isBlue(item) then some examples may be "water",
	 *            "sky", "ocean", "saphire", etc...
	 * @param negativeExamples
	 *            A list of negative examples for the predicate being defined.
	 *            Following from the above IsBlue(item) predicate example, some
	 *            negative examples might include "grass", "sun", "mayonnaise",
	 *            "mushroom", "pavement", etc...
	 * @param keyword
	 *            The keyword to be used in searching. This value cannot be
	 *            {@code null}, but it can be the empty string. It also must be
	 *            a single word. For example, the predicate IsBlue(item) might
	 *            use "colour" as a keyword.
	 * @param pathToSaveTrainingData
	 *            This is a path to save the word bag data. This file can then
	 *            be used with {@link #SimpleOpenEval(Instances, String)} to
	 *            circumvent the need to search all of examples again.
	 * @param search
	 *            This search engine is used in place of the default search
	 *            engine
	 * @param pathForLinkContentMemoization
	 *            The path to which memoized link content data will be saved.
	 * @throws Exception
	 *             An exception can be thrown if: WEKA could not set the options
	 *             on the {@link #filter}, WEKA could not save the word bag
	 *             data, WEKA could not apply the filter on the word bags, or if
	 *             WEKA could not train the classifier on the produced word
	 *             frequency data.
	 */
	public MultithreadSimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword,
			String pathToSaveTrainingData, GenericSearchEngine search, String pathForLinkContentMemoization)
			throws Exception {
		memoizeLinkContents = true;
		this.linkContentsPath = pathForLinkContentMemoization;
		try {
			this.memoizedLinkContents = loadMemoizedContents(linkContentsPath);
		} catch (EOFException e) {
			this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
		}
		this.search = search;

		SMO smo = new SMO();
		String[] SMOOptions = new String[] { "-M" };
		smo.setOptions(SMOOptions);
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(smo);
		fc.setFilter(new ClassBalancer());
		classifier = fc;

		this.keyword = keyword;
		// creates word bag data
		if (verbose)
			System.out.println("M. Creating Training Data");
		this.unfilteredTrainingData = createTrainingData(positiveExamples, negativeExamples);
		this.unfilteredTrainingData.setClass(this.unfilteredTrainingData.attribute(classAttributeName));
		if (verbose)
			System.out.println("M. Saving unfiltered data");
		// Save the word bag data
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.unfilteredTrainingData);
		saver.setFile(new File(pathToSaveTrainingData));
		saver.writeBatch();

		if (verbose)
			System.out.println("M. Filtering data");
		filter = new StringToWordVector();
		String[] options = new String[] { "-C", "-L" };
		filter.setOptions(options);
		this.trainingData = wordBagsToWordFrequencies(this.unfilteredTrainingData);
		this.trainingData.setClass(this.trainingData.attribute(classAttributeName));

		if (verbose)
			System.out.println("M. Training Classifier");
		classifier.buildClassifier(this.trainingData);
		if (verbose)
			System.out.println("M. done");
	}

	/**
	 * Uses a .arff file such as those produced by
	 * {@link #SimpleOpenEval(Instances, String)} and creates a new
	 * SimpleOpenEval. This constructor uses no API calls, unlike the other
	 * constructors.
	 * <p>
	 * TODO fix: Presently this constructor does not copy {@link wordBags}, so
	 * this could lead to a mutability issue if the Instances is modified.
	 * 
	 * @param wordBags
	 *            This .arff file must contain 2 attributes: "corpus" a text
	 *            attribute, and "class" a Nominal Attribute with values of
	 *            "true" or "false". Each instance in the .arff file is a word
	 *            bag with the words separated by spaces, and whether the word
	 *            bag was produced from a positive or negative example.
	 * @param keyword
	 *            The keyword to use when searching. Ideally should match the
	 *            keyword used to produce {@code wordBags}. Same retrictions of
	 *            being a single word and non-{@code null} apply.
	 * @throws Exception
	 *             Thrown if WEKA cannot filter or train on {@code wordBags}.
	 */
	public MultithreadSimpleOpenEval(Instances wordBags, String keyword) throws Exception {
		if (this.memoizeLinkContents) {
			try {
				this.memoizedLinkContents = loadMemoizedContents(linkContentsPath);
			} catch (EOFException e) {
				this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
		}
		// search = new BingSearchJSON();
		search = new GoogleCSESearchJSON();

		SMO smo = new SMO();
		String[] SMOOptions = new String[] { "-M" };
		smo.setOptions(SMOOptions);
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(smo);
		fc.setFilter(new ClassBalancer());
		classifier = fc;

		this.keyword = keyword;

		filter = new StringToWordVector();
		// set the option so that word count rather than word presence is used.
		String[] options = new String[] { "-C", "-L" };
		try {
			filter.setOptions(options);
		} catch (Exception e) {
			e.printStackTrace();
			// Should not happen
			throw new RuntimeException(e);
		}
		// TODO: check how to copy word bag
		this.unfilteredTrainingData = wordBags;
		this.unfilteredTrainingData.setClass(this.unfilteredTrainingData.attribute(classAttributeName));
		this.trainingData = wordBagsToWordFrequencies(wordBags);
		this.trainingData.setClass(this.trainingData.attribute(classAttributeName));
		classifier.buildClassifier(this.trainingData);
	}

	/**
	 * Takes an Instances object where each instance is a String attribute
	 * called "corpus" that contains the word bag as a String of space seperated
	 * words, and a Nominal Attribute which is either "true" or "false".
	 * <p>
	 * This method also sets the filter's input format to that of the wordBags.
	 * This forces WEKA to do Batch Filtering, meaning that future Instances
	 * filtered by {@link #filter} will use the same dictionary, and will
	 * therefore be classifiable by {@link #classifier}.
	 * 
	 * @throws Exception
	 *             WEKA was unable to convert the word bags to word frequencies.
	 */
	private Instances wordBagsToWordFrequencies(Instances wordBags) throws Exception {
		assert (filter != null);

		filter.setInputFormat(wordBags);
		return Filter.useFilter(wordBags, filter);
	}

	/**
	 * Creates and returns a nominal Attribute with the values of "true" or
	 * "false", and the name of {@link #classAttributeName}
	 */
	private Attribute getClassAttribute() {
		List<String> classValues = new ArrayList<String>(2);
		classValues.add("true");
		classValues.add("false");
		return new Attribute(classAttributeName, classValues);
	}

	/**
	 * Saves the word-frequency data as a .arff file which can be opened using
	 * Weka. This method should only be used for debugging and development
	 * purposes.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void saveFilteredTrainingData(String path) throws IOException {
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.trainingData);
		saver.setFile(new File(path));
		saver.writeBatch();
	}

	/**
	 * Given positive and negative examples of the predicate, produces an
	 * Instances with 2 attributes: the String attribute "corpus" and the
	 * Nominal Attribute "class" (values are "true" or "false"). The attribute
	 * "class" is set as the Class Attribute in the Instances object returned.
	 */
	private Instances createTrainingData(List<String> positiveExamples, List<String> negativeExamples) {
		if (verbose)
			System.out.println("M. Retrieving positive word bags");
		List<WordBagAndArg> positiveWordBags = getWordBags(positiveExamples, numOfLinkThreads);
		if (verbose)
			System.out.println("M. Retrieving negative word bags");
		List<WordBagAndArg> negativeWordBags = getWordBags(negativeExamples, numOfLinkThreads);

		if (verbose)
			System.out.println("M. Creating attributes");
		// Create a corpus attribute of the Weka type string attribute
		List<String> textValues = null;
		Attribute textCorpus = new Attribute("corpus", textValues);

		// Create a nominal attribute to function as the Class attribute
		Attribute classAttribute = getClassAttribute();

		/*
		 * defines the feature vector that each instance in the Instances object
		 * will obey
		 */
		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(2);
		featureVector.add(textCorpus);
		featureVector.add(classAttribute);

		if (verbose)
			System.out.println("M. Creating Instance");

		/*
		 * Creates the Instances object in which each contained Instance will
		 * obey the feature vector declared above, and which has a capacity
		 * equal to the total number of word bags.
		 */
		Instances posOrNegWordBags = new Instances("posOrNegWordBags", featureVector,
				positiveWordBags.size() + negativeWordBags.size());

		if (verbose)
			System.out.println("M. Adding bags to Instance");
		/*
		 * Adds positive and negative word bags to the Instances. Positive word
		 * bags are given a "class" value of "true", negative word bags are
		 * given a "class" value of "false".
		 */
		addWordBagInstances(posOrNegWordBags, "true", positiveWordBags, featureVector);
		addWordBagInstances(posOrNegWordBags, "false", negativeWordBags, featureVector);

		/*
		 * Sets the class attribute to "class"
		 */
		posOrNegWordBags.setClass(classAttribute);
		return posOrNegWordBags;
	}

	/**
	 * Adds the {@code wordBags} given to {@code posOrNegWordBags}. Each word
	 * bag in {@code wordBags} will be it's own instance, with the "corpus"
	 * attribute being the word bag, and the "class" attribute being
	 * {@code string}.
	 * 
	 * @param posOrNegWordBags
	 *            The Instances object to which new Instance objects will be
	 *            added to. It must have sufficient capacity.
	 * @param string
	 *            The value for the "class" attribute. Must be "true" or
	 *            "false".
	 * @param wordBags
	 *            The list of word List<SearchResults> searchResultsbags to be
	 *            added
	 * @param featureVector
	 */
	private void addWordBagInstances(Instances posOrNegWordBags, String string, List<WordBagAndArg> wordBags,
			ArrayList<Attribute> featureVector) {
		assert (string.equals("true") || string.equals("false"));

		for (WordBagAndArg bag : wordBags) {
			Instance toAdd = new DenseInstance(2);
			// set corpus
			toAdd.setValue(featureVector.get(0), bag.getWordBag());
			// set class value
			toAdd.setValue(featureVector.get(1), string);
			posOrNegWordBags.add(toAdd);
		}
	}

	@Override
	public Expenditure[] getCost(String args) throws Exception {
		// TODO add cost in time, if possible add cost in API calls
		return new Expenditure[] {};
	}
	
	public void setVerbose(boolean verb){
		this.verbose = verb;
	}
	
	public boolean getVerbose(){
		return this.verbose;
	}

	/**
	 * Determine if {@code args} is true or false for this predicate. The
	 * confidence produced is (number of word bags in favor of result)/(total
	 * number of word bags).
	 */
	@Override
	public Opinion<Boolean, Double> getOpinion(String args) throws UnknownException {
		List<String> examples = new ArrayList<String>();
		examples.add(args);
		List<WordBagAndArg> wordBags = getWordBags(examples, numOfLinkThreads);

		// Create a corpus attribute of the Weka type string attribute
		List<String> textValues = null;
		Attribute textCorpus = new Attribute("corpus", textValues);

		// Create a nominal attribute to function as the Class attribute
		Attribute classAttribute = getClassAttribute();

		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(2);
		featureVector.add(textCorpus);
		featureVector.add(classAttribute);

		Instances wordBagsAsInstances = new Instances("posOrNegWordBags", featureVector, wordBags.size());

		for (WordBagAndArg wordBag : wordBags) {
			// Create sparse instance with 2 attributes (corpus & class)
			SparseInstance data = new SparseInstance(2);
			data.setDataset(wordBagsAsInstances);
			data.setValue(textCorpus, wordBag.getWordBag());
			data.setMissing(classAttribute);
			wordBagsAsInstances.add(data);
		}
		wordBags = null;

		// convert the word bags to word frequencies
		Instances wordFrequenciesAsInstances = null;
		try {
			wordFrequenciesAsInstances = Filter.useFilter(wordBagsAsInstances, filter);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UnknownException(e);
		}
		Enumeration<Instance> wordFrequencies = wordFrequenciesAsInstances.enumerateInstances();
		int positiveWordBags = 0;
		int negativeWordBags = 0;
		// classify each word frequency, depending on the result add to positive
		// or negative word bags counter
		while (wordFrequencies.hasMoreElements()) {
			Instance wordFrequency = wordFrequencies.nextElement();
			try {
				double[] distribution = classifier.distributionForInstance(wordFrequency);
				double classification = classifier.classifyInstance(wordFrequency);

				assert (distribution.length == 2);
				String responseAsString = trainingData.classAttribute().value((int) classification);
				if (responseAsString.equals("true")) {
					positiveWordBags++;
				} else if (responseAsString.equals("false")) {
					negativeWordBags++;
				} else {
					throw new RuntimeException("Invalid value for classification: " + responseAsString);
				}

			} catch (Exception e) {
				if (verbose) {
					System.out.println("Failed to classify frequency");
					e.printStackTrace();
				}
			}
		}

		if (positiveWordBags == negativeWordBags) {
			// if the unweighed vote is a tie, throw unknown
			throw new UnknownException(
					"Positive Word Bags: " + positiveWordBags + " Negative Word Bags " + negativeWordBags);
		} else if (positiveWordBags > negativeWordBags) {
			double confidence = ((double) positiveWordBags) / (positiveWordBags + negativeWordBags);
			return new Opinion<Boolean, Double>(true, confidence, this);
		} else {
			double confidence = ((double) negativeWordBags) / (positiveWordBags + negativeWordBags);
			return new Opinion<Boolean, Double>(false, confidence, this);
		}
	}

	/**
	 * Given a collection of strings, evaluate each and return the results. This
	 * method is quicker than {@link #getOpinion(String)}. If {@code args} is a
	 * list, then the returned {@code List<Opinion<Boolean, Double>>} will be in
	 * the same order. In the returned list, {@code null} signifies unknown,
	 * otherwise the opinion is present.
	 * 
	 * @param args
	 *            A list of Strings to be evaluated as being true or false
	 *            according to the trained open eval
	 * @return A list of opinions, one for each String if {@code args}. If the
	 *         value for a String is unknown, than {@code null} is returned as
	 *         an element in the list in the place of an opinion.
	 */
	public List<Opinion<Boolean, Double>> getOpinions(Collection<String> args) {
		List<WordBagAndArg> allWordBags = getWordBags(new ArrayList<String>(args), numOfLinkThreads);
		List<Opinion<Boolean, Double>> result = new ArrayList<Opinion<Boolean, Double>>(allWordBags.size());

		// Create a corpus attribute of the Weka type string attribute
		List<String> textValues = null;
		Attribute textCorpus = new Attribute("corpus", textValues);

		// Create a nominal attribute to function as the Class attribute
		Attribute classAttribute = getClassAttribute();

		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(2);
		featureVector.add(textCorpus);
		featureVector.add(classAttribute);

		for (String arg : args) {
			// collect word bags whose creating argument matches arg
			List<String> wordBags = new ArrayList<String>();
			for (WordBagAndArg bag : allWordBags) {
				if (arg.equalsIgnoreCase(bag.getArg())) {
					wordBags.add(bag.getWordBag());
				}
			}

			if (wordBags.isEmpty()) {
				result.add(null);
			} else {
				Instances wordBagsAsInstances = new Instances("posOrNegWordBags", featureVector, wordBags.size());

				for (String wordBag : wordBags) {
					// Create sparse instance with 2 attributes (corpus & class)
					SparseInstance data = new SparseInstance(2);
					data.setDataset(wordBagsAsInstances);
					data.setValue(textCorpus, wordBag);
					data.setMissing(classAttribute);
					wordBagsAsInstances.add(data);
				}
				wordBags = null;

				// convert the word bags to word frequencies
				Instances wordFrequenciesAsInstances = null;
				try {
					wordFrequenciesAsInstances = Filter.useFilter(wordBagsAsInstances, filter);
				} catch (Exception e) {
					e.printStackTrace();
					result.add(null);
					continue;
				}
				Enumeration<Instance> wordFrequencies = wordFrequenciesAsInstances.enumerateInstances();
				int positiveWordBags = 0;
				int negativeWordBags = 0;
				// classify each word frequency, depending on the result add to
				// positive
				// or negative word bags counter
				while (wordFrequencies.hasMoreElements()) {
					Instance wordFrequency = wordFrequencies.nextElement();
					try {
						double[] distribution = classifier.distributionForInstance(wordFrequency);
						double classification = classifier.classifyInstance(wordFrequency);

						// distribution should have 2 elements. The probability
						// of arg being "true" and the probability of arg being
						// "false" according to the classifier.
						assert (distribution.length == 2);
						String responseAsString = trainingData.classAttribute().value((int) classification);
						if (responseAsString.equals("true")) {
							positiveWordBags++;
						} else if (responseAsString.equals("false")) {
							negativeWordBags++;
						} else {
							throw new RuntimeException("Invalid value for classification: " + responseAsString);
						}

					} catch (Exception e) {
						if (verbose) {
							System.out.println("Failed to classify frequency");
							e.printStackTrace();
						}
						throw new RuntimeException(e);
					}
				}

				if (positiveWordBags == negativeWordBags) {
					result.add(null);
				} else if (positiveWordBags > negativeWordBags) {
					double confidence = ((double) positiveWordBags) / (positiveWordBags + negativeWordBags);
					result.add(new Opinion<Boolean, Double>(true, confidence, this));
				} else {
					double confidence = ((double) negativeWordBags) / (positiveWordBags + negativeWordBags);
					result.add(new Opinion<Boolean, Double>(false, confidence, this));
				}

			}
		}
		return result;
	}

	@Override
	public Double getTrust(String args, Optional<Boolean> value) {
		return null;
	}

	/**
	 * Returns the number of pages of search results that SimpleOpenEval checks
	 * for each argument to the predicate.
	 */
	public int getPagesToCheck() {
		return pagesToCheck;
	}

	/**
	 * Sets the number of pages of search results that SimpleOpenEval checks for
	 * each argument to the predicate. The number of pages must be between 1 and
	 * 10 inclusive.
	 */
	public void setPagesToCheck(int numOfPages) {
		if (numOfPages < 0 || numOfPages > 10) {
			throw new IllegalArgumentException();
		}
		this.pagesToCheck = numOfPages;
	}

	public GenericSearchEngine getSearch() {
		return search;
	}

	public void setSearch(GenericSearchEngine search) {
		this.search = search;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	/**
	 * Changes the classifier used to the classifier provided, and retrains.
	 * Note that the class-balancing filter will not be applied during training.
	 * If you want to apply the class balancing filter, or other filters to
	 * training/evaluation, then pass a filteredClassifier to this method.
	 * 
	 * @param classifier
	 * @throws Exception
	 */
	public void setClassifier(Classifier classifier) throws Exception {
		this.classifier = classifier;
		classifier.buildClassifier(trainingData);
	}

	/**
	 * Sets the suffix that will be appended to the name of this class. The
	 * suffix is used to make the name of this openeval unique, allowing for
	 * multiple open eval objects to be used in a CI using a ML aggregator.
	 * 
	 * @param newSuffix
	 */
	public void setNameSuffix(String newSuffix) {
		this.nameSuffix = newSuffix;
	}

	@Override
	public String getName() {
		return super.getName() + this.nameSuffix;
	}

	public String getKeyword() {
		return keyword;
	}

	/**
	 * Disables memoization of link contents and removes all saved link contents
	 * from memory
	 */
	public void setMemoizeLinkContentsOff() {
		this.memoizeLinkContents = false;
		this.memoizedLinkContents = null;
	}

	/**
	 * Enables memoization. If {@code pathToMemoizationFile} does not exist but
	 * the folder does, then the file will be created. If the file does exist,
	 * than it will be loaded to memory.
	 * 
	 * @param pathToMemoizationFile
	 *            Path to where memoized link information should be stored. If
	 *            the file exists then it should contain a serialized HashMap
	 *            that maps from String link names to String link contents.
	 * @throws IOException
	 *             If the file that pathToMemoizationFile cannot be
	 *             created/read.
	 */
	public void setMemoizeLinkContentsOn(String pathToMemoizationFile) throws IOException {
		try {
			this.memoizedLinkContents = loadMemoizedContents(pathToMemoizationFile);
		} catch (EOFException e) {
			this.memoizedLinkContents = new ConcurrentHashMap<String, String>();
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
		this.linkContentsPath = pathToMemoizationFile;
		this.memoizeLinkContents = true;
	}

	public boolean getMemoizeLinkContents() {
		return this.memoizeLinkContents;
	}

	/**
	 * Loads the Map of link name to link contents. If the file does not exist,
	 * but the directory does, then the file is created.
	 * 
	 * @param path
	 *            The path containing the serialized map
	 * @return Map from link name to link contents
	 * @throws IOException
	 *             If path is an invalid path, or if the file cannot be read
	 *             from.
	 * @throws ClassNotFoundException
	 *             If the serialized object is not a known object. This should
	 *             not occur unless an invalid file is given.
	 */
	private ConcurrentHashMap<String, String> loadMemoizedContents(String path)
			throws IOException, ClassNotFoundException {
		File f = new File(path);
		if (!f.exists()) {
			f.createNewFile();
			return new ConcurrentHashMap<String, String>();
		}

		try (FileInputStream fis = new FileInputStream(path)) {
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			ConcurrentHashMap<String, String> result = (ConcurrentHashMap<String, String>) ois.readObject();
			ois.close();
			return result;
		} catch (EOFException e) {
			return new ConcurrentHashMap<String, String>();
		}
	}

	/**
	 * Saves {@link #memoizedLinkContents} to {@link #linkContentsPath}.
	 * 
	 * @throws IOException
	 *             If {@link #linkContentsPath} is an invalid path, or if the
	 *             file cannot be written to.
	 */
	public void saveMemoizedContents() throws IOException {
		try (FileOutputStream fos = new FileOutputStream(linkContentsPath)) {
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.memoizedLinkContents);
			oos.close();
		}
	}

	/**
	 * Converts the list of inputs to the open eval {@link examples} into word
	 * bags using {@link #keyword}.
	 * 
	 * @param examples
	 *            The inputs for which word bags should be generated
	 * @param numOfLinkThreads
	 *            The number of {@link LinkContentsThread} to use
	 * @return a list of word bags, and the arguments used to create them. The
	 *         arguments may be in lower case.
	 */
	private List<WordBagAndArg> getWordBags(List<String> examples, int numOfLinkThreads) {
		List<WordBagAndArg> wordBags = new ArrayList<WordBagAndArg>();

		// whether the thread in charge of making queries to the search engine
		// is done
		AtomicBoolean SearchDone = new AtomicBoolean(false);
		// whether the thread in charge of converting webpage contents to word
		// bags is done
		AtomicBoolean WordDone = new AtomicBoolean(false);
		// List of website contents, and the query used to find them
		List<LinkContentsForSearch> cont = new ArrayList<LinkContentsForSearch>();

		// List of threads in charge of getting website contents from links
		List<LinkContentsThread> listT2 = new ArrayList<LinkContentsThread>();
		/*
		 * List of Lists of lists of website links by the query that created
		 * them. Each List<SearchResults> is used to send links from Thread1
		 * (the thread in charge of querying the search engine) to one of the
		 * threads in listT2.
		 */
		List<List<SearchResults>> t1Tot2Lists = new ArrayList<List<SearchResults>>();

		/*
		 * Each AtomicBoolean in the list corresponds to one of the threads in
		 * listT2. Each represents whether the thread in listT2 has completed
		 * execution.
		 */
		List<AtomicBoolean> LinksDone = new ArrayList<AtomicBoolean>();

		// for each link-reading thread, as dictated by the method's argument
		// numOfLinkThreads:
		for (int x = 0; x < numOfLinkThreads; x++) {
			// Create the list used to send links from the SearchThread t1 to
			// one of the threads in listT2.
			List<SearchResults> res = new ArrayList<SearchResults>();
			t1Tot2Lists.add(res);

			// Create the AtomicBoolean that will be used to indicated whether
			// one of the threads in listT2 has completed
			AtomicBoolean linksDoneBool = new AtomicBoolean(false);
			LinksDone.add(linksDoneBool);

			// Create a new thread object using the list and boolean from above
			LinkContentsThread t2 = new LinkContentsThread(res, SearchDone, linksDoneBool, cont);
			// give it a unique name for debugging
			t2.setName("_" + x);
			// Add the thread to listT2
			listT2.add(t2);
		}

		SearchThread t1 = new SearchThread(examples, search, SearchDone, t1Tot2Lists);

		WordProcessingThread t3 = new WordProcessingThread(LinksDone, cont, WordDone, wordBags);

		// Start all of the threads.
		(new Thread(t1)).start();
		for (LinkContentsThread t2 : listT2) {
			(new Thread(t2)).start();
		}

		(new Thread(t3)).start();

		// Wait for the word processing thread to be done placing word bags into
		// the list named wordBags
		synchronized (WordDone) {
			if (WordDone.get() != true) {
				try {
					WordDone.wait();
				} catch (InterruptedException e) {
					// TODO: Determine behaviour
					e.printStackTrace();
				}
			}
		}
		// return wordBags, it should be filled by the thread t3
		return wordBags;
	}

	/**
	 * This Runnable searches words, and returns the resulting links.
	 * 
	 * @author Ian Berlot-Attwell
	 */
	private class SearchThread implements Runnable {

		/**
		 * Whether this thread has finished searching and returning links.
		 * Should be false until the {@link #run()} method has been called no
		 * further SearchResults will be added to any of the lists in
		 * {@link #ListSearchResults}.
		 */
		AtomicBoolean amDone;

		/**
		 * A list of words to be searched with
		 * {@link MultithreadSimpleOpenEval.keyword}. As words are searched they
		 * are removed from the list. This thread does not write to examples.
		 */
		List<String> examples;

		/**
		 * The search engine to use to make queries.
		 */
		GenericSearchEngine search;

		/**
		 * When search results are produced by searching a word in
		 * {@link #examples}, the links are divided amongst each {@code List
		 * <SearchResults>} in this list. This class only writes to these lists,
		 * it does not read any of them.
		 */
		List<List<SearchResults>> ListSearchResults;

		/**
		 * Create a SearchThread that, when run, takes a String for
		 * {@code examples}, searches it using {@code search} preceded with
		 * {@link MultithreadSimpleOpenEval.keyword}, divides the links into
		 * {@code ListSearchResults.size()} roughly equal parts and places each
		 * part into a list in {@code ListSearchResults}. Once there are no more
		 * words in {@code examples} and all of the links have been distributed
		 * amongst the lists in {@code ListSearchResults}, then
		 * {@code searchThreadIsDone} is set to true.
		 * 
		 * @param examples
		 *            The strings to be searched
		 * @param search
		 *            The search engine to search the Strings in
		 *            {@code examples}.
		 * @param searchThreadIsDone
		 *            Whether the thread is finished searching words and
		 *            distributing the links. Must be false during contruction.
		 * @param ListSearchResults
		 *            The lists amongst which the found links are distributed.
		 *            Cannot be empty or {@code null}.
		 */
		public SearchThread(List<String> examples, GenericSearchEngine search, AtomicBoolean searchThreadIsDone,
				List<List<SearchResults>> ListSearchResults) {
			assert (searchThreadIsDone.get() == false);
			assert (!ListSearchResults.isEmpty());

			amDone = searchThreadIsDone;
			this.examples = examples;
			this.search = search;
			this.ListSearchResults = ListSearchResults;
		}

		/**
		 * For each String in {@code examples}, searches it using {@code search}
		 * preceded with {@link MultithreadSimpleOpenEval.keyword}, divides the
		 * links into {@code ListSearchResults.size()} roughly equal parts and
		 * places each part into a list in {@code ListSearchResults}. Once there
		 * are no more words in {@code examples} and all of the links have been
		 * distributed amongst the lists in {@code ListSearchResults}, then
		 * {@code searchThreadIsDone} is set to true.
		 */
		@Override
		public void run() {
			Iterator<String> itr = examples.iterator();
			// iterates through each String in examples
			while (itr.hasNext()) {
				String ex = itr.next();
				try {
					if (verbose)
						System.out.println("1. Starting search");
					SearchResults resultsForEx;
					if (keyword.isEmpty()) {
						resultsForEx = search.search(ex);
					} else {
						resultsForEx = search.search(keyword + " " + ex);
					}
					if (verbose)
						System.out.println("1. Done search");

					List<SearchResults> splitResults = splitNWay(resultsForEx, ListSearchResults.size());

					// places a unique part of the results in list in
					// ListSearchResults, and notifies the sublist.
					for (int x = 0; x < ListSearchResults.size(); x++) {
						List<SearchResults> r = ListSearchResults.get(x);
						synchronized (r) {
							if (verbose)
								System.out.println("1. Updating Search Results #" + x);
							r.add(splitResults.get(x));
							r.notifyAll();
						}
					}

					// If there are no further Strings in example, sets amDone
					// to true and notifies all sublists in ListSearchResults
					Object lock = new Object();
					synchronized (lock) {
						if (!itr.hasNext()) {
							if (verbose)
								System.out.println("1. setting Done to true");
							amDone.set(true);
							for (List<SearchResults> sr : ListSearchResults) {
								synchronized (sr) {
									sr.notifyAll();
								}
							}
						}
					}

				} catch (IOException e) {
					Object lock = new Object();
					synchronized (lock) {
						if (verbose)
							System.err.println("Searching " + ex + " falied");

						/*
						 * If there are no further Strings in example, sets
						 * amDone to true and notifies all sublists in
						 * ListSearchResults
						 */
						if (!itr.hasNext()) {
							if (verbose)
								System.out.println("1. setting Done to true (last search failed)");
							amDone.set(true);

							for (List<SearchResults> sr : ListSearchResults) {
								synchronized (sr) {
									sr.notifyAll();
								}
							}
						}
					}
				}
			}
		}

		/**
		 * Takes a SearchResults (which is a list of links), and splits it into
		 * n SearchResults, each of roughly equal size. {@code n} must be
		 * {@code >0}.
		 */
		public List<SearchResults> splitNWay(SearchResults toSplit, int n) {
			assert (n > 0);

			List<SearchResults> result = new ArrayList<SearchResults>(n);

			// retrive non-link information stored in toSplit
			int hits = toSplit.getHits();
			String query = toSplit.getQuery();
			int pageNumber = toSplit.getPageNumber();

			// Create n empty SearchResults each containing the same non-link
			// information
			for (int x = 0; x < n; x++) {
				List<SearchResult> temp = new ArrayList<SearchResult>();
				result.add(new SearchResults(hits, temp, query, pageNumber));
			}

			// Distributes links amongst the n empty SearchResults evenly.
			int index = 0;
			while (index + n <= toSplit.size()) {
				for (int x = 0; x < n; x++) {
					result.get(x).add(toSplit.get(index + x));
				}
				index += n;
			}

			// Places the remaining links (0 <= remaining links < n) one in each
			// SearchResults until there are no further links
			for (; index < toSplit.size(); index++) {
				result.get(index % n).add(toSplit.get(index));
			}

			return result;
		}
	}

	/**
	 * A Runnable that, when run, removes SearchResults objects from a list,
	 * searches the links stored within, the passes the link's contents along
	 * with the query that produced the link to a separate list.
	 * 
	 * @author Ian Berlot-Attwell
	 */
	private class LinkContentsThread implements Runnable {
		/**
		 * List of SearchResults objects. This list should be the output of a
		 * {@link MultithreadSimpleOpenEval.SearchThread}
		 */
		List<SearchResults> searchResults;

		/**
		 * Whether the {@link MultithreadSimpleOpenEval.SearchThread} adding new
		 * links to {@link #searchResults} has no further links to add.
		 */
		AtomicBoolean searchDone;

		/**
		 * Whether {@link #searchResults} is empty and {@link #searchDone} is
		 * true.
		 */
		AtomicBoolean amDone;

		/**
		 * List of LinkContentsForSearch objects. Each of these objects contains
		 * the text body of a link, along with the search query that produced
		 * the link.
		 */
		List<LinkContentsForSearch> linkContents;

		/**
		 * The name for this thread. The name is used in the debugging print
		 * statements to differentiate between the different
		 * {@link MultithreadSimpleOpenEval.SearchThread} threads running.
		 */
		String name = "";

		/**
		 * Creates a new Runnable that, when run, removes SearchResults objects
		 * from a {@code searchResuts}, searches the links stored within, and
		 * then passes the link's contents along with the query that produced
		 * the link to {@code linkContents}. This behaviour continues until
		 * {@code searchResuts} is empty and and {@code SearchThreadIsDone} is
		 * true, at which point {@code LinkContentsIsDone} is set to true.
		 * <p>
		 * Note that whenever a new element is added to {@code linkContents}, or
		 * when the thread is finished executing, notifyAll is called on
		 * {@code linkContents}.
		 * 
		 * @param searchResults
		 *            List of SearchResults objects. This list should be the
		 *            output of a {@link MultithreadSimpleOpenEval.SearchThread}
		 *            . This list should have notifyAll() called upon it
		 *            whenever new elements are added to it, or when
		 *            {@code SearchThreadIsDone} is set to true.
		 * @param SearchThreadIsDone
		 *            Whether the {@link MultithreadSimpleOpenEval.SearchThread}
		 *            adding new links to {@code searchResults} has no further
		 *            links to add.
		 * @param LinkContentsIsDone
		 *            Whether {@code searchResults} is empty and
		 *            {@code SearchThreadIsDone} is true.
		 * @param linkContents
		 *            List of LinkContentsForSearch objects. Each of these
		 *            objects contains the text body of a link, along with the
		 *            search query that produced the link. This list is notified
		 *            whenever this thread adds to it, or is finished executing.
		 */
		public LinkContentsThread(List<SearchResults> searchResults, AtomicBoolean SearchThreadIsDone,
				AtomicBoolean LinkContentsIsDone, List<LinkContentsForSearch> linkContents) {
			this.searchResults = searchResults;
			searchDone = SearchThreadIsDone;
			amDone = LinkContentsIsDone;
			this.linkContents = linkContents;
		}

		/**
		 * Sets the name used in debug messages to differentiate this thread
		 * from other instances of the same class.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Removes SearchResults from {@link #searchResults} until the list is
		 * empty and {@link #searchDone} is true. For each SearchResult removed
		 * from {@link #searchResults}, the links are read and their contents
		 * are added to {@link #linkContents}, at which point notifyAll() is
		 * called on {@link #linkContents}.
		 * <p>
		 * When {@link #searchResults} is empty and {@link #searchDone} is true
		 * and there are no futher links that need to be read, then
		 * {@link #amDone} is set to true and notifyAll is called on
		 * {@link #linkContents}.
		 */
		@Override
		public void run() {
			List<SearchResults> toProcess = new ArrayList<SearchResults>();
			while (true) {
				// If there are no more links to process, either get more links,
				// or return.
				if (toProcess.isEmpty()) {
					if (verbose)
						System.out.println("2." + name + " No results to process");

					// Synchronize on the SearchResults. This means that
					// SearchThread CANNOT have searchResults & searchDone in
					// a contradictory state.
					synchronized (searchResults) {
						// If there are not futher search results
						if (searchResults.isEmpty()) {
							if (verbose)
								System.out.println("2." + name + " SearchResults is empty");
							// ... and search is done, then this thread is done
							if (searchDone.get()) {
								synchronized (linkContents) {
									if (verbose)
										System.out.println("2." + name + " SearchThread is done, so am I");
									amDone.set(true);
									linkContents.notifyAll();
									return;
								}
							}
							// ... otherwise wait for more results
							try {
								if (verbose)
									System.out.println("2." + name + " Waiting on SearchThread");
								searchResults.wait();
								if (verbose)
									System.out.println("2." + name + " Done Waiting");

								if (searchDone.get() && searchResults.isEmpty()) {
									synchronized (linkContents) {
										if (verbose)
											System.out.println(
													"2." + name + " Done waiting. SearchThread is done, so am I");
										amDone.set(true);
										linkContents.notifyAll();
										return;
									}
								}
								assert (!searchResults.isEmpty());
							} catch (InterruptedException e) {
								// TODO: Determine correct behaviour
								return;
							}
						}

						toProcess.addAll(searchResults);
						searchResults.clear();
					}
				}
				assert (!toProcess.isEmpty());

				for (Iterator<SearchResults> itr = toProcess.iterator(); itr.hasNext();) {
					SearchResults curr = itr.next();
					itr.remove();
					// Creates a object that represents the contents of the
					// link, and also records that the link was found by
					// searching curr.getQuery()
					LinkContentsForSearch contents = new LinkContentsForSearch(curr.getQuery());
					for (SearchResult link : curr) {
						// if link memoization is being used, and the links
						// contents are known, then add the known results and
						// continue to the next link
						if (memoizeLinkContents && link.getLink() != null
								&& memoizedLinkContents.containsKey(link.getLink())) {
							String savedContents = (memoizedLinkContents.get(link.getLink()));
							if (!savedContents.isEmpty()) {
								contents.add(savedContents);
							}
							continue;
						}

						try {
							if (verbose)
								System.out.println("2." + name + " Reading: " + link);
							// read the contents of the website
							// TODO: set jsoup so as to reject any website that
							// request authentication
							String websiteAsString = Jsoup.connect(link.getLink())
									.userAgent(
											"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:48.0) Gecko/20100101 Firefox/48.0")
									.referrer("http://www.google.com").get().text();
							if (!websiteAsString.isEmpty()) {
								if (verbose)
									System.out.println("2." + name + " Adding result to contents");
								contents.add(websiteAsString);
								if (memoizeLinkContents) {
									memoizedLinkContents.put(link.getLink(), websiteAsString);
								}
							}
						} catch (Exception e) {
							if (verbose)
								System.err.println("2." + name + " Unable to read " + link.getLink() + " CAUSE: " + e);
							if (memoizeLinkContents) {
								memoizedLinkContents.put(link.getLink(), "");
							}
						}
					}

					// once all the links in the list toProccess are completed,
					// update and notify the linkContents list.
					synchronized (linkContents) {
						if (verbose)
							System.out.println("2." + name + " Updating linkContents");
						linkContents.add(contents);
						linkContents.notifyAll();
						if (verbose)
							System.out.println("2." + name + " Notified");
					}
				}
			}
		}
	}

	/**
	 * This class represents the contents of the links produced by a query, as
	 * well as the query used to find the links. Each element in the set is the
	 * contents of a website found by searching {@link #keywords}.
	 * 
	 * @author Ian Berlot-Attwell
	 *
	 */
	@SuppressWarnings("serial")
	private static class LinkContentsForSearch extends HashSet<String> {
		final String keywords;

		public LinkContentsForSearch(String keywords) {
			this.keywords = keywords;
		}

		public String getKeywords() {
			return keywords;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof LinkContentsForSearch)) {
				return false;
			}

			LinkContentsForSearch lc = (LinkContentsForSearch) o;
			return (super.equals((HashSet<String>) o) && this.getKeywords().equals(lc.getKeywords()));
		}

		@Override
		public String toString() {
			return keywords + ": " + super.toString();
		}

	}

	/**
	 * This thread converts the contents of links along with the queries used to
	 * find said links; and converts them into word bags.
	 * 
	 * @author Ian Berlot-Attwell
	 *
	 */
	private class WordProcessingThread implements Runnable {
		/**
		 * Each element in this list is whether a distinct LinkContentsThread
		 * has finished adding new elements to {@link #linkContents}.
		 */
		List<AtomicBoolean> linksDone;

		/**
		 * Whether all of the elements in {@link #linksDone} are true, and this
		 * thread has no further wordBags to add to {@link #generatedWordBags}.
		 */
		AtomicBoolean amDone;

		/**
		 * The list from which the thread takes the contents of websites so that
		 * they may be converted to wordBags.
		 */
		List<LinkContentsForSearch> linkContents;

		/**
		 * The list to which the thread adds wordBags created.
		 */
		List<WordBagAndArg> generatedWordBags;

		/**
		 * The list of all stop words to be removed from word bags.
		 */
		List<String> stopWords = Arrays.asList(new String[] { "a", "about", "above", "across", "after", "again",
				"against", "all", "almost", "alone", "along", "already", "also", "although", "always", "among", "an",
				"and", "another", "any", "anybody", "anyone", "anything", "anywhere", "are", "area", "areas", "around",
				"as", "ask", "asked", "asking", "asks", "at", "away", "b", "back", "backed", "backing", "backs", "be",
				"became", "because", "become", "becomes", "been", "before", "began", "behind", "being", "beings",
				"best", "better", "between", "big", "both", "but", "by", "c", "came", "can", "cannot", "case", "cases",
				"certain", "certainly", "clear", "clearly", "come", "could", "d", "did", "differ", "different",
				"differently", "do", "does", "done", "down", "down", "downed", "downing", "downs", "during", "e",
				"each", "early", "either", "end", "ended", "ending", "ends", "enough", "even", "evenly", "ever",
				"every", "everybody", "everyone", "everything", "everywhere", "f", "face", "faces", "fact", "facts",
				"far", "felt", "few", "find", "finds", "first", "for", "four", "from", "full", "fully", "further",
				"furthered", "furthering", "furthers", "g", "gave", "general", "generally", "get", "gets", "give",
				"given", "gives", "go", "going", "good", "goods", "got", "great", "greater", "greatest", "group",
				"grouped", "grouping", "groups", "h", "had", "has", "have", "having", "he", "her", "here", "herself",
				"high", "high", "high", "higher", "highest", "him", "himself", "his", "how", "however", "i", "if",
				"important", "in", "interest", "interested", "interesting", "interests", "into", "is", "it", "its",
				"itself", "j", "just", "k", "keep", "keeps", "kind", "knew", "know", "known", "knows", "l", "large",
				"largely", "last", "later", "latest", "least", "less", "let", "lets", "like", "likely", "long",
				"longer", "longest", "m", "made", "make", "making", "man", "many", "may", "me", "member", "members",
				"men", "might", "more", "most", "mostly", "mr", "mrs", "much", "must", "my", "myself", "n", "necessary",
				"need", "needed", "needing", "needs", "never", "new", "new", "newer", "newest", "next", "no", "nobody",
				"non", "noone", "not", "nothing", "now", "nowhere", "number", "numbers", "o", "of", "off", "often",
				"old", "older", "oldest", "on", "once", "one", "only", "open", "opened", "opening", "opens", "or",
				"order", "ordered", "ordering", "orders", "other", "others", "our", "out", "over", "p", "part",
				"parted", "parting", "parts", "per", "perhaps", "place", "places", "point", "pointed", "pointing",
				"points", "possible", "present", "presented", "presenting", "presents", "problem", "problems", "put",
				"puts", "q", "quite", "r", "rather", "really", "right", "right", "room", "rooms", "s", "said", "same",
				"saw", "say", "says", "second", "seconds", "see", "seem", "seemed", "seeming", "seems", "sees",
				"several", "shall", "she", "should", "show", "showed", "showing", "shows", "side", "sides", "since",
				"small", "smaller", "smallest", "so", "some", "somebody", "someone", "something", "somewhere", "state",
				"states", "still", "still", "such", "sure", "t", "take", "taken", "than", "that", "the", "their",
				"them", "then", "there", "therefore", "these", "they", "thing", "things", "think", "thinks", "this",
				"those", "though", "thought", "thoughts", "three", "through", "thus", "to", "today", "together", "too",
				"took", "toward", "turn", "turned", "turning", "turns", "two", "u", "under", "until", "up", "upon",
				"us", "use", "used", "uses", "v", "very", "w", "want", "wanted", "wanting", "wants", "was", "way",
				"ways", "we", "well", "wells", "went", "were", "what", "when", "where", "whether", "which", "while",
				"who", "whole", "whose", "why", "will", "with", "within", "without", "work", "worked", "working",
				"works", "would", "x", "y", "year", "years", "yet", "you", "young", "younger", "youngest", "your",
				"yours", "z" });

		/**
		 * The maximum allowable distance between any 2 needed words (arguments
		 * to the open eval and the keyword) in a word bag.
		 */
		public final static int WORD_BAG_SPACING = 15;

		/**
		 * Creates a thread that reads LinkContentsForSearch from
		 * {@code linkContents} and converts them into word bags which are saved
		 * to {@code generatedWordBags}. This process continues until all
		 * elements of {@code LinkContentsIsDone} are true, the list
		 * {@code linkContents} is emtpy (from this thread removing the list's
		 * elements), and the thread has no further word bags to add to
		 * {@code generatedWordBags}. At this point {@code wordProcessingIsDone}
		 * is set to true.
		 * 
		 * @param LinkContentsIsDone
		 *            When all the elements are true, this signals to this
		 *            thread that no further elements will be added to
		 *            {@code linkContents}.
		 * @param linkContents
		 *            The contents of links along with the query used to find
		 *            the links.
		 * @param wordProcessingIsDone
		 *            True when all elements of {@code LinkContentsIsDone} are
		 *            true, the list {@code linkContents} is emtpy (from this
		 *            thread removing the list's elements), and the thread has
		 *            no further word bags to add to {@code generatedWordBags}.
		 * @param generatedWordBags
		 *            The list to which word bags extracted from the
		 *            LinkContentsForSearch in {@code linkContents} are added.
		 */
		public WordProcessingThread(List<AtomicBoolean> LinkContentsIsDone, List<LinkContentsForSearch> linkContents,
				AtomicBoolean wordProcessingIsDone, List<WordBagAndArg> generatedWordBags) {
			linksDone = LinkContentsIsDone;
			amDone = wordProcessingIsDone;
			this.linkContents = linkContents;
			this.generatedWordBags = generatedWordBags;
		}

		/**
		 * Converts LinkContentsForSearch in {@link #linkContents} into word
		 * bags which are added to {@link #generatedWordBags}. Each time word
		 * bags are added, notifyAll() is called on {@link #generatedWordBags}.
		 * Once all elements of {@link #linksDone} are true, the list
		 * {@link #linkContents} is empty (from this thread removing the list's
		 * elements), and the thread has no further word bags to add to
		 * {@link #generatedWordBags}; then {@link #amDone} is set to true.
		 */
		@Override
		public void run() {
			/*
			 * The list of LinkContentsForSearch that need to be converted into
			 * word bags
			 */
			List<LinkContentsForSearch> toProcess = new ArrayList<LinkContentsForSearch>();
			while (true) {
				// if there are no link contents to process ...
				if (toProcess.isEmpty()) {
					if (verbose)
						System.out.println("3. Need more text");
					synchronized (linkContents) {
						/*
						 * If linkContents contains more LinkContentsForSearch
						 * then copy them and continue. Otherwise...
						 */
						if (linkContents.isEmpty()) {
							if (verbose)
								System.out.println("3. LinkContent Thread buffer is empty");
							// If all of the elements in linksDone are true,
							// stop execution.
							if (allLinkThreadsDone()) {
								synchronized (generatedWordBags) {
									if (verbose)
										System.out.println("3. LinkContent Thread done, therefore so am I.");
									synchronized (amDone) {
										amDone.set(true);
										amDone.notifyAll();
									}
									generatedWordBags.notifyAll();
								}
								return;
								// Otherwise, wait for more text
							} else {
								try {
									if (verbose)
										System.out.println("3. Waiting for more text.");
									linkContents.wait();
									/*
									 * If more text is available, copy it and
									 * continue processing. Otherwise, check if
									 * all of the text generating threads have
									 * completed.
									 */
									if (linkContents.isEmpty()) {

										if (allLinkThreadsDone()) {
											synchronized (generatedWordBags) {
												if (verbose)
													System.out.println(
															"3. LinkContent Thread has no more data therefore I am done.");
												synchronized (amDone) {
													amDone.set(true);
													amDone.notifyAll();
												}
												generatedWordBags.notifyAll();
											}
											assert (allLinkThreadsDone());
											return;
										} else {
											continue;
										}
									}
								} catch (InterruptedException e) {
									return;
								}
							}

						}
						// empty linkContents and more the text into
						// linkContents
						toProcess.addAll(linkContents);
						linkContents.clear();
						if (verbose)
							System.out.println("3. Added and cleared linkContents");
					}
				}

				// Iterates through each LinkContentsForSearch object
				for (Iterator<LinkContentsForSearch> itr = toProcess.iterator(); itr.hasNext();) {
					// Retrieve collection of link contents for a search
					LinkContentsForSearch linkContentsSet = itr.next();
					itr.remove();
					// extract the search terms and the keyword from the
					// keywords value of linkContentsSet
					Set<String> searchTermsAndKeywords = new HashSet<String>(Arrays.asList(
							linkContentsSet.getKeywords().toLowerCase().replaceAll("[^\\w0-9'-]", " ").split(" +")));
					// Iterates through each String link content in the
					// LinkContentsForSearch object
					for (Iterator<String> itr2 = linkContentsSet.iterator(); itr2.hasNext();) {
						String contents = itr2.next();
						itr2.remove();

						if (verbose)
							System.out.println("3. starting word bag extraction");
						// conver the text into a list of lowercase words made
						// of letters, numbers, hyphens, single quotes and
						// underscores.
						contents = contents.toLowerCase().replaceAll("[^\\w0-9'-]", " ");
						List<String> textAsList = new ArrayList<String>(Arrays.asList(contents.split("\\s++")));

						List<WordBagAndArg> wordBags = textAsListToWordBags(textAsList, searchTermsAndKeywords,
								linkContentsSet.getKeywords().toLowerCase());
						textAsList = null;

						synchronized (generatedWordBags) {
							if (verbose)
								System.out.println("3. Adding more word bags");
							generatedWordBags.addAll(wordBags);
							generatedWordBags.notifyAll();
						}

					}
				}

			}
		}

		/**
		 * Whether all the LinkContentsThread objects are done running
		 */
		private boolean allLinkThreadsDone() {
			synchronized (linkContents) {
				for (AtomicBoolean a : linksDone) {
					if (a.get() == false) {
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * Converts a list of words into a list of word bags in which each word
		 * is seperated by " ". Specifically: for each area in
		 * {@code textAsList} where all of the {@code neededWords} are within
		 * {@link #WORD_BAG_SPACING} of eachother, the following is done: 1) The
		 * area along with {@link #WORD_BAG_SPACING} words before and after are
		 * extracted. 2) All words in {@link #stopWords} along with the words in
		 * {@code neededWords} are removed. 3) The result is concatenated
		 * together with " " as a spacer between words. This is a single word
		 * bag.
		 * 
		 * @param textAsList
		 *            The text to be converted into word bags. Each individual
		 *            word should be an element in the list.
		 * @param neededWords
		 *            The words that must be present in the text from which the
		 *            word bag is extracted.
		 * @return A list of word bags.
		 */
		private List<WordBagAndArg> textAsListToWordBags(List<String> textAsList, Set<String> neededWords,
				String query) {
			// we are presently looking at the first word in the text
			int currFirstWord = 0;
			/*
			 * this list contains all the word bags found. The word bags contain
			 * all the neededWords with a max spacing of WORD_BAG_SPACING
			 * between them, plus WORD_BAG_SPACING word before and after the
			 * last found words in neededWords
			 */
			List<WordBagAndArg> result = new ArrayList<WordBagAndArg>();
			for (; currFirstWord < textAsList.size(); currFirstWord++) {
				/*
				 * Checks that the first word in the block of text being
				 * examined is one of the neededWords
				 */
				if (neededWords.contains(textAsList.get(currFirstWord))) {
					// marks which word has been found
					Set<String> foundNeededWords = new HashSet<String>();
					foundNeededWords.add(textAsList.get(currFirstWord));
					// the index at which the last neededWord has been found
					int lastWordFound = currFirstWord;

					// looks at the next word in the text
					int currWord = currFirstWord + 1;
					while (currWord < textAsList.size()) {
						/*
						 * whether another needed word has been found within
						 * WORD_BAG_SPACING of the last needed word found
						 */
						Boolean wordFound = false;
						for (; currWord < lastWordFound + WORD_BAG_SPACING
								&& currWord < textAsList.size(); currWord++) {
							// a needed word not previously found
							if (neededWords.contains(textAsList.get(currWord))
									&& !foundNeededWords.contains(textAsList.get(currWord))) {
								wordFound = true;
								lastWordFound = currWord;
								foundNeededWords.add(textAsList.get(currWord));
								break;
							}
						}
						// if all words have been found, add the word bag to the
						// list
						if (foundNeededWords.equals(neededWords)) {
							int firstIndex = currFirstWord - WORD_BAG_SPACING;
							if (firstIndex < 0) {
								firstIndex = 0;
							}
							int lastIndex = lastWordFound + WORD_BAG_SPACING;
							if (lastIndex >= textAsList.size()) {
								lastIndex = textAsList.size() - 1;
							}

							StringBuilder text = new StringBuilder();
							for (; firstIndex <= lastIndex; firstIndex++) {
								String word = textAsList.get(firstIndex);
								if (!stopWords.contains(word) && !neededWords.contains(word)) {
									text.append(word);
									text.append(" ");
								}
							}

							if (keyword.isEmpty()) {
								result.add(new WordBagAndArg(text.toString(), query));
							} else {
								result.add(new WordBagAndArg(text.toString(), query.substring(keyword.length() + 1)));
							}
							break;
						} else if (wordFound == false) {
							break;
						}
					}
				}
			}
			return result;
		}
	}

	/**
	 * Represents a word bag as a String of words seperated by " ", as well as
	 * the arguments (excluding {@link MultithreadSimpleOpenEval.keyword}) to
	 * the MultithreadSimpleOpenEval which created it. The arguments may be in
	 * lowercase.
	 * 
	 * @author Ian Berlot-Attwell
	 *
	 */
	private class WordBagAndArg {
		final String wordBag;
		final String arg;

		/**
		 * Construct WordBagAndArg.
		 * 
		 * @param wordBag
		 *            a word bag as a String of words seperated by " "
		 * @param arg
		 *            the arguments to to the MultithreadSimpleOpenEval which
		 *            created {@code wordBag}.
		 */
		WordBagAndArg(String wordBag, String arg) {
			this.wordBag = wordBag;
			this.arg = arg;
		}

		/**
		 * Return a word bag as a String of words seperated by " "
		 */
		public String getWordBag() {
			return wordBag;
		}

		/**
		 * Return the arguments to MultithreadSimpleOpenEval which created the
		 * word bag represented by this object.
		 */
		public String getArg() {
			return arg;
		}

	}
}
