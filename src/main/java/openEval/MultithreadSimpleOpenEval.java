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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

//TODO: Re-implement text memoization

/**
 * A simplified version of OpenEval. This class can answer predicates by being
 * given positive and negative examples.
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

	String nameSuffix = "";
	boolean memoizeLinkContents = false;
	Map<String, String> memoizedLinkContents;
	public static final String classAttributeName = "Class_Attribute_For_SimpleOpenEval";
	// TODO: modify so that activating memoization forces the user to give a
	// path
	public static final String linkContentsPath = "./src/main/resources/data/monthData/OpenEval/LinkContents.txt";
	// TODO: eventually change to reading from text file
	public static final int numOfLinkThreads = 8;

	/**
	 * Creates a new SimpleOpenEval. This may take some time, and will most
	 * likely result in some errors being printed to the console as some
	 * webpages will not be readable.
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
				this.memoizedLinkContents = new HashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new HashMap<String, String>();
		}
		search = new GoogleCSESearchJSON();
		classifier = new SMO();
		this.keyword = keyword;
		filter = new StringToWordVector();
		/*
		 * Sets the filter so that it produces the Count of each word, rather
		 * than it's presence
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
	 * Creates a new SimpleOpenEval. This may take some time, and will most
	 * likely result in some errors being printed to the console as some
	 * webpages will not be readable.
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
				this.memoizedLinkContents = new HashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new HashMap<String, String>();
		}
		search = new GoogleCSESearchJSON();
		classifier = new SMO();
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
		// TODO: Remove later
		try {
			saver = new ArffSaver();
			saver.setInstances(this.trainingData);
			saver.setFile(new File("./filteredResults.arff"));
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}

		classifier.buildClassifier(this.trainingData);
	}

	/**
	 * Creates a new SimpleOpenEval. This may take some time, and will most
	 * likely result in some errors being printed to the console as some
	 * webpages will not be readable.
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
				this.memoizedLinkContents = new HashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new HashMap<String, String>();
		}
		this.search = search;
		classifier = new SMO();
		this.keyword = keyword;
		// creates word bag data
		System.out.println("M. Creating Training Data");
		this.unfilteredTrainingData = createTrainingData(positiveExamples, negativeExamples);
		this.unfilteredTrainingData.setClass(this.unfilteredTrainingData.attribute(classAttributeName));
		System.out.println("M. Saving unfiltered data");
		// Save the word bag data
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.unfilteredTrainingData);
		saver.setFile(new File(pathToSaveTrainingData));
		saver.writeBatch();

		System.out.println("M. Filtering data");
		filter = new StringToWordVector();
		String[] options = new String[] { "-C", "-L" };
		filter.setOptions(options);
		this.trainingData = wordBagsToWordFrequencies(this.unfilteredTrainingData);
		this.trainingData.setClass(this.trainingData.attribute(classAttributeName));
		// TODO: Remove later
		System.out.println("M. Saving filtered data");
		try {
			saver = new ArffSaver();
			saver.setInstances(this.trainingData);
			saver.setFile(new File("./filteredResults.arff"));
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("M. Training Classifier");
		classifier.buildClassifier(this.trainingData);
		System.out.println("M. done");
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
				this.memoizedLinkContents = new HashMap<String, String>();
			}
		} else {
			this.memoizedLinkContents = new HashMap<String, String>();
		}
		// search = new BingSearchJSON();
		search = new GoogleCSESearchJSON();
		classifier = new SMO();
		this.keyword = keyword;
		// TODO mark produced training data by search engine?
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

	private Attribute getClassAttribute() {
		List<String> classValues = new ArrayList<String>(2);
		classValues.add("true");
		classValues.add("false");
		return new Attribute(classAttributeName, classValues);
	}

	/**
	 * Given positive and negative examples of the predicate, produces an
	 * Instances with 2 attributes: the String attribute "corpus" and the
	 * Nominal Attribute "class" (values are "true" or "false"). The attribute
	 * "class" is set as the Class Attribute in the Instances object returned.
	 */
	private Instances createTrainingData(List<String> positiveExamples, List<String> negativeExamples) {
		System.out.println("M. Retrieving positive word bags");
		List<String> positiveWordBags = getWordBags(positiveExamples, numOfLinkThreads);
		System.out.println("M. Retrieving negative word bags");
		List<String> negativeWordBags = getWordBags(negativeExamples, numOfLinkThreads);

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

		System.out.println("M. Creating Instance");

		/*
		 * Creates the Instances object in which each contained Instance will
		 * obey the feature vector declared above, and which has a capacity
		 * equal to the total number of word bags.
		 */
		Instances posOrNegWordBags = new Instances("posOrNegWordBags", featureVector,
				positiveWordBags.size() + negativeWordBags.size());

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
	private void addWordBagInstances(Instances posOrNegWordBags, String string, List<String> wordBags,
			ArrayList<Attribute> featureVector) {
		assert (string.equals("true") || string.equals("false"));

		for (String bag : wordBags) {
			Instance toAdd = new DenseInstance(2);
			// set corpus
			toAdd.setValue(featureVector.get(0), bag);
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

	/**
	 * Determine if {@code args} is true or false for this predicate. The
	 * confidence produced is (number of word bags in favor of result)/(total
	 * number of word bags).
	 */
	@Override
	public Opinion<Boolean, Double> getOpinion(String args) throws UnknownException {
		List<String> examples = new ArrayList<String>();
		examples.add(args);
		List<String> wordBags = getWordBags(examples, numOfLinkThreads);

		// Create a corpus attribute of the Weka type string attribute
		List<String> textValues = null;
		Attribute textCorpus = new Attribute("corpus", textValues);

		// Create a nominal attribute to function as the Class attribute
		Attribute classAttribute = getClassAttribute();

		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(2);
		featureVector.add(textCorpus);
		featureVector.add(classAttribute);

		Instances wordBagsAsInstances = new Instances("posOrNegWordBags", featureVector, wordBags.size());

		// TODO: determine if we should use weighted or unweighed vote
		for (String wordBag : wordBags) {
			// Create sparse instance with 2 attributes (corpus & class)
			SparseInstance data = new SparseInstance(2);
			data.setDataset(wordBagsAsInstances);
			data.setValue(textCorpus, wordBag);
			data.setMissing(classAttribute);
			wordBagsAsInstances.add(data);
		}

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
				System.out.println("Failed to classify frequency");
				e.printStackTrace();
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

	public void setClassifier(Classifier classifier) throws Exception {
		this.classifier = classifier;
		classifier.buildClassifier(trainingData);
	}

	public void setNameSuffix(String newSuffix) {
		this.nameSuffix = newSuffix;
	}

	@Override
	public String getName() {
		return super.getName() + this.nameSuffix;
	}

	public void setMemoizeLinkContents(boolean memoize) {
		this.memoizeLinkContents = memoize;
	}

	public boolean getMemoizeLinkContents() {
		return this.memoizeLinkContents;
	}

	public Map<String, String> loadMemoizedContents(String path) throws IOException, ClassNotFoundException {
		File f = new File(path);
		if (!f.exists()) {
			f.createNewFile();
		}

		try (FileInputStream fis = new FileInputStream(path)) {
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			Map<String, String> result = (Map<String, String>) ois.readObject();
			ois.close();
			return result;
		} catch (EOFException e) {
			return new HashMap<String, String>();
		}
	}

	public void saveMemoizedContents() throws IOException {
		try (FileOutputStream fos = new FileOutputStream(linkContentsPath)) {
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.memoizedLinkContents);
			oos.close();
		}
	}

	// TODO
	public List<String> getWordBags(List<String> examples, int numOfLinkThreads) {
		List<String> wordBags = new ArrayList<String>();

		AtomicBoolean SearchDone = new AtomicBoolean(false);
		AtomicBoolean WordDone = new AtomicBoolean(false);
		List<LinkContentsForSearch> cont = new ArrayList<LinkContentsForSearch>();

		List<LinkContentsThread> listT2 = new ArrayList<LinkContentsThread>();
		List<List<SearchResults>> t1Tot2Lists = new ArrayList<List<SearchResults>>();
		List<AtomicBoolean> LinksDone = new ArrayList<AtomicBoolean>();

		for (int x = 0; x < numOfLinkThreads; x++) {
			List<SearchResults> res = new ArrayList<SearchResults>();
			t1Tot2Lists.add(res);

			AtomicBoolean linksDoneBool = new AtomicBoolean(false);
			LinksDone.add(linksDoneBool);

			LinkContentsThread t2 = new LinkContentsThread(res, SearchDone, linksDoneBool, cont);
			t2.setName("_" + x);
			listT2.add(t2);
		}

		SearchThread t1 = new SearchThread(examples, keyword, search, SearchDone, t1Tot2Lists);

		WordProcessingThread t3 = new WordProcessingThread(LinksDone, cont, WordDone, wordBags);

		(new Thread(t1)).start();
		for (LinkContentsThread t2 : listT2) {
			(new Thread(t2)).start();
		}

		(new Thread(t3)).start();

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
		return wordBags;
	}

	private class SearchThread implements Runnable {

		AtomicBoolean amDone;
		List<String> examples;
		String keyword;
		GenericSearchEngine search;
		List<List<SearchResults>> ListSearchResults;
		int currIndex = 0;

		// Note searchResults should be a synchronizedList
		public SearchThread(List<String> examples, String keyword, GenericSearchEngine search,
				AtomicBoolean searchThreadIsDone, List<List<SearchResults>> ListSearchResults) {
			assert (searchThreadIsDone.get() == false);
			assert (!ListSearchResults.isEmpty());

			amDone = searchThreadIsDone;
			this.examples = examples;
			this.search = search;
			this.ListSearchResults = ListSearchResults;
			this.keyword = keyword;
		}

		@Override
		public void run() {
			Iterator<String> itr = examples.iterator();
			while (itr.hasNext()) {
				String ex = itr.next();
				try {
					System.out.println("1. Starting search");
					SearchResults resultsForEx;
					if (keyword.isEmpty()) {
						resultsForEx = search.search(ex);
					} else {
						resultsForEx = search.search(keyword + " " + ex);
					}
					System.out.println("1. Done search");

					/*
					 * As this block and the catch are synchronized, possible
					 * states are: 1) searchResults contains resultsForEx &
					 * amDone = false; 2) searchResults contains resultsForEx &
					 * amDone = true 3) searchResuts may or may not be empty,
					 * but amDone=true
					 */
					List<SearchResults> searchResults = ListSearchResults.get(currIndex);
					currIndex = (currIndex == ListSearchResults.size() - 1) ? 0 : currIndex + 1;
					synchronized (searchResults) {
						// System.out.println("Adding result");
						System.out.println("1. Updating Search Results");
						searchResults.add(resultsForEx);
						// System.out.println("checking if next exists");
						if (!itr.hasNext()) {
							System.out.println("1. setting Done to true");
							amDone.set(true);
							for (List<SearchResults> sr : ListSearchResults) {
								synchronized (sr) {
									sr.notifyAll();
								}
							}
						}
						// System.out.println("notifying");
						searchResults.notifyAll();
					}

				} catch (IOException e) {
					List<SearchResults> searchResults = ListSearchResults.get(currIndex);
					currIndex = ((currIndex + 1) == searchResults.size()) ? 0 : currIndex + 1;
					synchronized (searchResults) {
						System.err.println("Searching " + ex + " falied");
						// e.printStackTrace();
						if (!itr.hasNext()) {
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
	}

	private class LinkContentsThread implements Runnable {
		List<SearchResults> searchResults;
		AtomicBoolean searchDone;
		AtomicBoolean amDone;
		List<LinkContentsForSearch> linkContents;
		String name = "";

		public LinkContentsThread(List<SearchResults> searchResults, AtomicBoolean SearchThreadIsDone,
				AtomicBoolean LinkContentsIsDone, List<LinkContentsForSearch> linkContents) {
			this.searchResults = searchResults;
			searchDone = SearchThreadIsDone;
			amDone = LinkContentsIsDone;
			this.linkContents = linkContents;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public void run() {
			List<SearchResults> toProcess = new ArrayList<SearchResults>();
			while (true) {
				// If there are no more links to process, either get more links,
				// or return.
				if (toProcess.isEmpty()) {
					System.out.println("2." + name + " No results to process");

					// Synchronize on the SearchResults. This means that
					// SearchThread CANNOT have searchResults & searchDone in
					// a contradictory state.
					synchronized (searchResults) {
						// If there are not futher search results
						if (searchResults.isEmpty()) {
							System.out.println("2." + name + " SearchResults is empty");
							// ... and search is done, then this thread is done
							if (searchDone.get()) {
								synchronized (linkContents) {
									System.out.println("2." + name + " SearchThread is done, so am I");
									amDone.set(true);
									linkContents.notifyAll();
									return;
								}
							}
							// ... otherwise wait for more results
							try {
								System.out.println("2." + name + " Waiting on SearchThread");
								searchResults.wait();
								System.out.println("2." + name + " Done Waiting");

								if (searchDone.get() && searchResults.isEmpty()) {
									synchronized (linkContents) {
										System.out
												.println("2." + name + " Done waiting. SearchThread is done, so am I");
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
					LinkContentsForSearch contents = new LinkContentsForSearch(curr.getQuery());
					for (SearchResult link : curr) {
						if (memoizeLinkContents && memoizedLinkContents.containsKey(link.getLink())) {
							contents.add(memoizedLinkContents.get(link.getLink()));
							continue;
						}

						try {
							System.out.println("2." + name + " Reading: " + link);
							String websiteAsString = Jsoup.connect(link.getLink())
									.userAgent(
											"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:48.0) Gecko/20100101 Firefox/48.0")
									.referrer("http://www.google.com").get().text();
							if (!websiteAsString.isEmpty()) {
								System.out.println("2." + name + " Adding result to contents");
								contents.add(websiteAsString);
								if (memoizeLinkContents) {
									memoizedLinkContents.put(link.getLink(), websiteAsString);
								}
							}
						} catch (Exception e) {
							System.err.println("2." + name + " Unable to read " + link.getLink() + " CAUSE: " + e);
						}
					}

					synchronized (linkContents) {
						System.out.println("2." + name + " Updating linkContents");
						linkContents.add(contents);
						linkContents.notifyAll();
						System.out.println("2." + name + " Notified");
					}
				}
			}
		}
	}

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

	private class WordProcessingThread implements Runnable {
		List<AtomicBoolean> linksDone;
		AtomicBoolean amDone;
		List<LinkContentsForSearch> linkContents;
		List<String> generatedWordBags;

		/**
		 * The list of all stop words to be ignored.
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
		public final static int WORD_BAG_SPACING = 15;

		public WordProcessingThread(List<AtomicBoolean> LinkContentsIsDone, List<LinkContentsForSearch> linkContents,
				AtomicBoolean wordProcessingIsDone, List<String> generatedWordBags) {
			linksDone = LinkContentsIsDone;
			amDone = wordProcessingIsDone;
			this.linkContents = linkContents;
			this.generatedWordBags = generatedWordBags;
		}

		@Override
		public void run() {
			List<LinkContentsForSearch> toProcess = new ArrayList<LinkContentsForSearch>();
			while (true) {
				if (toProcess.isEmpty()) {
					System.out.println("3. Need more text");
					synchronized (linkContents) {
						if (linkContents.isEmpty()) {
							System.out.println("3. LinkContent Thread buffer is empty");
							if (allLinkThreadsDone()) {
								synchronized (generatedWordBags) {
									System.out.println("3. LinkContent Thread done, therefore so am I.");
									synchronized (amDone) {
										amDone.set(true);
										amDone.notifyAll();
									}
									generatedWordBags.notifyAll();
								}
								return;
							} else {
								try {
									System.out.println("3. Waiting for more text.");
									linkContents.wait();
									if (linkContents.isEmpty()) {
										synchronized (generatedWordBags) {
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
									}
								} catch (InterruptedException e) {
									return;
								}
							}

						}
						toProcess.addAll(linkContents);
						linkContents.clear();
						System.out.println("3. Added and cleared linkContents");
					}
				}

				for (Iterator<LinkContentsForSearch> itr = toProcess.iterator(); itr.hasNext();) {
					LinkContentsForSearch linkContentsSet = itr.next();
					itr.remove();
					Set<String> searchTermsAndKeywords = new HashSet<String>(
							Arrays.asList(linkContentsSet.getKeywords().toLowerCase().split(" +")));
					for (Iterator<String> itr2 = linkContentsSet.iterator(); itr2.hasNext();) {
						String contents = itr2.next();
						itr2.remove();

						System.out.println("3. starting word bag extraction");
						// TODO: Find faster solution?
						// contents =
						// Jsoup.parse(contents).text().toLowerCase().replaceAll("[^\\w0-9-]",
						// " ");
						contents = contents.toLowerCase().replaceAll("[^\\w0-9-]", " ");
						List<String> textAsList = new ArrayList<String>(Arrays.asList(contents.split("\\s++")));

						List<String> wordBags = textAsListToWordBags(textAsList, searchTermsAndKeywords);
						textAsList = null;

						synchronized (generatedWordBags) {
							System.out.println("3. Adding more word bags");
							generatedWordBags.addAll(wordBags);
							generatedWordBags.notifyAll();
						}

					}
				}

			}
		}

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
		private List<String> textAsListToWordBags(List<String> textAsList, Set<String> neededWords) {
			// we are presently looking at the first word in the text
			int currFirstWord = 0;
			/*
			 * this list contains all the word bags found. The word bags contain
			 * all the neededWords with a max spacing of WORD_BAG_SPACING
			 * between them, plus WORD_BAG_SPACING word before and after the
			 * last found words in neededWords
			 */
			List<String> result = new ArrayList<String>();
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
							result.add(text.toString());
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
}
