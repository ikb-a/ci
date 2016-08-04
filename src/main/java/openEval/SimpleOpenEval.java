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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.machineLearning.util.MLUtility;
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

/**
 * A simplified version of OpenEval. This class can answer predicates by being
 * given positive and negative examples.
 * 
 * @author Ian Berlot-Attwell.
 *
 */
public class SimpleOpenEval extends Source<String, Boolean, Double> {
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
	boolean memoizeLinkContents = true;
	Map<String, String> memoizedLinkContents;
	public static final String classAttributeName = "Class_Attribute_For_SimpleOpenEval";
	// TODO: modify so that activating memoization forces the user to give a
	// path
	public static final String linkContentsPath = "./src/main/resources/data/monthData/OpenEval/LinkContents.txt";
	// TODO: eventually change to reading from text file
	/**
	 * The list of all stop words to be ignored.
	 */
	List<String> stopWords = Arrays.asList(new String[] { "a", "about", "above", "across", "after", "again", "against",
			"all", "almost", "alone", "along", "already", "also", "although", "always", "among", "an", "and", "another",
			"any", "anybody", "anyone", "anything", "anywhere", "are", "area", "areas", "around", "as", "ask", "asked",
			"asking", "asks", "at", "away", "b", "back", "backed", "backing", "backs", "be", "became", "because",
			"become", "becomes", "been", "before", "began", "behind", "being", "beings", "best", "better", "between",
			"big", "both", "but", "by", "c", "came", "can", "cannot", "case", "cases", "certain", "certainly", "clear",
			"clearly", "come", "could", "d", "did", "differ", "different", "differently", "do", "does", "done", "down",
			"down", "downed", "downing", "downs", "during", "e", "each", "early", "either", "end", "ended", "ending",
			"ends", "enough", "even", "evenly", "ever", "every", "everybody", "everyone", "everything", "everywhere",
			"f", "face", "faces", "fact", "facts", "far", "felt", "few", "find", "finds", "first", "for", "four",
			"from", "full", "fully", "further", "furthered", "furthering", "furthers", "g", "gave", "general",
			"generally", "get", "gets", "give", "given", "gives", "go", "going", "good", "goods", "got", "great",
			"greater", "greatest", "group", "grouped", "grouping", "groups", "h", "had", "has", "have", "having", "he",
			"her", "here", "herself", "high", "high", "high", "higher", "highest", "him", "himself", "his", "how",
			"however", "i", "if", "important", "in", "interest", "interested", "interesting", "interests", "into", "is",
			"it", "its", "itself", "j", "just", "k", "keep", "keeps", "kind", "knew", "know", "known", "knows", "l",
			"large", "largely", "last", "later", "latest", "least", "less", "let", "lets", "like", "likely", "long",
			"longer", "longest", "m", "made", "make", "making", "man", "many", "may", "me", "member", "members", "men",
			"might", "more", "most", "mostly", "mr", "mrs", "much", "must", "my", "myself", "n", "necessary", "need",
			"needed", "needing", "needs", "never", "new", "new", "newer", "newest", "next", "no", "nobody", "non",
			"noone", "not", "nothing", "now", "nowhere", "number", "numbers", "o", "of", "off", "often", "old", "older",
			"oldest", "on", "once", "one", "only", "open", "opened", "opening", "opens", "or", "order", "ordered",
			"ordering", "orders", "other", "others", "our", "out", "over", "p", "part", "parted", "parting", "parts",
			"per", "perhaps", "place", "places", "point", "pointed", "pointing", "points", "possible", "present",
			"presented", "presenting", "presents", "problem", "problems", "put", "puts", "q", "quite", "r", "rather",
			"really", "right", "right", "room", "rooms", "s", "said", "same", "saw", "say", "says", "second", "seconds",
			"see", "seem", "seemed", "seeming", "seems", "sees", "several", "shall", "she", "should", "show", "showed",
			"showing", "shows", "side", "sides", "since", "small", "smaller", "smallest", "so", "some", "somebody",
			"someone", "something", "somewhere", "state", "states", "still", "still", "such", "sure", "t", "take",
			"taken", "than", "that", "the", "their", "them", "then", "there", "therefore", "these", "they", "thing",
			"things", "think", "thinks", "this", "those", "though", "thought", "thoughts", "three", "through", "thus",
			"to", "today", "together", "too", "took", "toward", "turn", "turned", "turning", "turns", "two", "u",
			"under", "until", "up", "upon", "us", "use", "used", "uses", "v", "very", "w", "want", "wanted", "wanting",
			"wants", "was", "way", "ways", "we", "well", "wells", "went", "were", "what", "when", "where", "whether",
			"which", "while", "who", "whole", "whose", "why", "will", "with", "within", "without", "work", "worked",
			"working", "works", "would", "x", "y", "year", "years", "yet", "you", "young", "younger", "youngest",
			"your", "yours", "z" });
	public final static int WORD_BAG_SPACING = 15;

	// TODO: remove main method/move test into test folder.
	/**
	 * Quick and dirty test.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// try to create fictional character identifier
		// String[] posNames = new String[] { "Martha Wayne", "Bruno Walton",
		// "Mayra Wickransingh", "Cliff Kammash",
		// "Pierre Aronnax", "Alan Grant", "Count Dracula", "Clark Kent",
		// "Artemis Fowl", "Harry Potter",
		// "Tom Servo", "Arnold Rimmer", "Jamie McCrimmon", "Han Solo" };
		// List<String> pos = new ArrayList<String>(Arrays.asList(posNames));

		// String[] negNames = new String[] { "Ian Berlot-Attwell", "Janelle
		// Stalder", "Sam Neill", "Bela Lugosi",
		// "Craig Kenny", "Ian Marter", "Herbert Wells", "David Liu", "Linda
		// Triantafillou", "Issac Asimov",
		// "Isaac Newton", "Harrison Ford" };
		// List<String> neg = new ArrayList<String>(Arrays.asList(negNames));
		// SimpleOpenEval bob = new SimpleOpenEval(pos, neg, "",
		// "./Fictional3.arff");

		Instances data = MLUtility.fileToInstances("./Fictional1.arff");
		SimpleOpenEval bob = new SimpleOpenEval(data, "");
		System.out.println(bob.getOpinion("fsdlkj lsjfls"));
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
	 * @throws Exception
	 *             An exception can be thrown if: WEKA could not set the options
	 *             on the {@link #filter}, WEKA could not apply the filter on
	 *             the word bags, or if WEKA could not train the classifier on
	 *             the produced word frequency data.
	 */
	public SimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword)
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
	public SimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword,
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
	public SimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword,
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
	public SimpleOpenEval(Instances wordBags, String keyword) throws Exception {
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
		/*
		 * these maps point from examples given, to a list of articles found
		 * about said example
		 */
		Map<String, List<String>> positiveExampleText = new HashMap<String, List<String>>();
		Map<String, List<String>> negativeExampleText = new HashMap<String, List<String>>();

		/*
		 * Maps the text of the example to a List<String>. The List<String> is
		 * produced by 1) searching "{@link keyord} {@code example}" 2)Reading
		 * the contents of each website produced in 1)
		 */
		mapExamplesToText(negativeExampleText, negativeExamples);
		List<String> negativeWordBags = mapOfTextToBags(negativeExampleText);
		negativeExampleText = null;

		mapExamplesToText(positiveExampleText, positiveExamples);
		List<String> positiveWordBags = mapOfTextToBags(positiveExampleText);
		positiveExampleText = null;

		/*
		 * Converts the above Map into a list of word bags. The a single word
		 * bag are produced by: 1) Searching for areas in the Text where the
		 * keyword, and all the words in the current example are within 15 words
		 * of eachother 2) extracting these areas plus 15 words before and after
		 * 3) removing the stop words, the words in the example, and the
		 * keyword.
		 */

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

		/*
		 * Creates the Instances object in which each contained Instance will
		 * obey the feature vector declared above, and which has a capacity
		 * equal to the total number of word bags.
		 */
		Instances posOrNegWordBags = new Instances("posOrNegWordBags", featureVector,
				positiveWordBags.size() + negativeWordBags.size());

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
	 *            The list of word bags to be added
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

	/**
	 * Converts {@code exampleText} into a list of word bags. Each word bag is
	 * extracted from one of the texts in {@code exampleText}. The word bags are
	 * extracted from text that contains {@link #keyword} and the words used to
	 * find the text, with stopwords, punctuation, the words used to find the
	 * text, and {@link keyword} removed.
	 * 
	 * @param exampleText
	 *            A map that maps from the example used to a list of the text of
	 *            each website found. (i.e. for the IsBlue(item) predicate, the
	 *            map might map from "sky" to a list containing "
	 * @return List of word bags. Each word bag is a list of words separated by
	 *         " ".
	 */
	private List<String> mapOfTextToBags(Map<String, List<String>> exampleText) {
		List<String> wordBags = new ArrayList<String>();

		for (String key : exampleText.keySet()) {
			List<String> texts = new ArrayList<String>(exampleText.get(key));
			/*
			 * Iterates through each text, converts it to lowercase, removes all
			 * that is not a number or a word or - or _
			 */
			for (int x = 0; x < texts.size(); x++) {
				String text = texts.get(x).toLowerCase();
				text = text.replaceAll("[^\\w0-9-]", " ");
				List<String> textAsList = new ArrayList<String>(Arrays.asList(text.split("\\s++")));

				Set<String> searchTermsAndKeywords = new HashSet<String>(Arrays.asList(key.toLowerCase().split(" +")));
				if (!keyword.equals("")) {
					searchTermsAndKeywords.add(this.keyword.toLowerCase());
				}
				// converts the formatted text into a list of word bags, and
				// adds it to the list.
				wordBags.addAll(textAsListToWordBags(textAsList, searchTermsAndKeywords));
			}
		}
		return wordBags;
	}

	/**
	 * Converts a list of words into a list of word bags in which each word is
	 * seperated by " ". Specifically: for each area in {@code textAsList} where
	 * all of the {@code neededWords} are within {@link #WORD_BAG_SPACING} of
	 * eachother, the following is done: 1) The area along with
	 * {@link #WORD_BAG_SPACING} words before and after are extracted. 2) All
	 * words in {@link #stopWords} along with the words in {@code neededWords}
	 * are removed. 3) The result is concatenated together with " " as a spacer
	 * between words. This is a single word bag.
	 * 
	 * @param textAsList
	 *            The text to be converted into word bags. Each individual word
	 *            should be an element in the list.
	 * @param neededWords
	 *            The words that must be present in the text from which the word
	 *            bag is extracted.
	 * @return A list of word bags.
	 */
	private List<String> textAsListToWordBags(List<String> textAsList, Set<String> neededWords) {
		// we are presently looking at the first word in the text
		int currFirstWord = 0;
		/*
		 * this list contains all the word bags found. The word bags contain all
		 * the neededWords with a max spacing of WORD_BAG_SPACING between them,
		 * plus WORD_BAG_SPACING word before and after the last found words in
		 * neededWords
		 */
		List<String> result = new ArrayList<String>();
		for (; currFirstWord < textAsList.size(); currFirstWord++) {
			/*
			 * Checks that the first word in the block of text being examined is
			 * one of the neededWords
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
					for (; currWord < lastWordFound + WORD_BAG_SPACING && currWord < textAsList.size(); currWord++) {
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
							if (!this.stopWords.contains(word) && !neededWords.contains(word)) {
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

	/**
	 * For each example in {@code examples}, it (along with the keyword) are
	 * searched in {@link #search}. Each link produced by {@link #search} is
	 * then read. Finally inside of {@code exampleText}, the example is mapped
	 * to the contents of each link.
	 * 
	 * @param exampleText
	 *            An empty map. After the method is run the map will map from an
	 *            example in {@code exampes} to a list of text extracted from
	 *            websites that were found using said example.
	 * @param examples
	 *            A list of example arguments for the predicate. For example,
	 *            for the IsBlue(item) predicate, the list may contain positive
	 *            examples such as "sky","ocean", etc... or negative examples
	 *            such as "brick", "dandelion", etc...
	 */
	private void mapExamplesToText(Map<String, List<String>> exampleText, List<String> examples) {
		for (String example : examples) {
			// extracts the texts from each website found using example
			List<String> texts = getText(example);

			// adds the texts to the map
			if (exampleText.containsKey(example)) {
				exampleText.get(example).addAll(texts);
			} else {
				exampleText.put(example, texts);
			}
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
		// search args using {@link #search} and read each website produced
		List<String> text = getText(args);

		// create map pointing from args to the texts
		Map<String, List<String>> argsToText = new HashMap<String, List<String>>();
		argsToText.put(args, text);

		// produce word bags needed
		List<String> wordBags = mapOfTextToBags(argsToText);

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
	 * Searches {@code example} using {@link #search}, then reads each link
	 * found and returns the contents.
	 * 
	 * @param example
	 *            An argument to the predicate
	 * @return List of website contents found by searching {@code example} along
	 *         with {@link #keyword}
	 */
	private List<String> getText(String example) {
		List<String> texts = new ArrayList<String>();

		SearchResults results = null;

		// Searches the example plus the keyword
		try {
			if (keyword.equals("")) {
				results = search.search(example);
			} else {
				results = search.search(this.keyword + " " + example);
			}
		} catch (IOException e) {
			System.out.println(example + " failed to be searched");
			e.printStackTrace();
		}

		// returns empty list if the search failed
		if (results == null) {
			return texts;
		}

		// otherwise for each link found in the search, opens and reads it
		for (SearchResult result : results) {
			String link = result.getLink();
			String linkContents = readLink(link);
			texts.add(linkContents);
		}
		return texts;
	}

	/**
	 * Connects to and reads {@code link}
	 */
	private String readLink(String link) {
		if (this.memoizeLinkContents && this.memoizedLinkContents.containsKey(link)) {
			return this.memoizedLinkContents.get(link);
		}

		try {
			Document doc = Jsoup.connect(link).get();
			String result = doc.body().text();
			if (memoizeLinkContents) {
				memoizedLinkContents.put(link, result);
			}
			return result;
		} catch (Exception e) {
			System.out.println("Reading " + link + " failed: " + e);
			// e.printStackTrace();
			if (memoizeLinkContents) {
				memoizedLinkContents.put(link, "");
			}
			return "";
		}
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
		}
	}

	public void saveMemoizedContents() throws IOException {
		try (FileOutputStream fos = new FileOutputStream(linkContentsPath)) {
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.memoizedLinkContents);
			oos.close();
		}
	}
}
