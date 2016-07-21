package openEval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import edu.toronto.cs.se.ci.utils.searchEngine.BingSearchJSON;
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
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

//TODO: make way to store/load search results, preferably remembering keyword used
public class SimpleOpenEval extends Source<String, Boolean, double[]> {
	GenericSearchEngine search;
	String keyword;
	StringToWordVector filter;
	Instances trainingData;
	Classifier classifier;
	int pagesToCheck = 1;
	// TODO: eventually change to reading from text file
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

	public static void main(String[] args) throws Exception {
		List<String> pos = new ArrayList<String>();
		pos.add("white");
		// SimpleOpenEval bob = new SimpleOpenEval(pos, new ArrayList<String>(),
		// "cat", "./wordFreqTest3.arff");
	}

	public SimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword)
			throws Exception {
		// TODO add search engine as parameter, and mark training data
		// accordingly
		// search = new BingSearchJSON();
		search = new GoogleCSESearchJSON();
		classifier = new SMO();
		this.keyword = keyword;
		filter = new StringToWordVector();
		String[] options = new String[] { "-C" };
		filter.setOptions(options);
		// change to create Instances method;
		Instances wordBags = createTrainingData(positiveExamples, negativeExamples);
		this.trainingData = wordBagsToWordFrequencies(wordBags);
		classifier.buildClassifier(this.trainingData);
	}

	public SimpleOpenEval(List<String> positiveExamples, List<String> negativeExamples, String keyword,
			String pathToSaveTrainingData) throws Exception {
		// search = new BingSearchJSON();
		search = new GoogleCSESearchJSON();
		classifier = new SMO();
		this.keyword = keyword;
		Instances wordBags = createTrainingData(positiveExamples, negativeExamples);
		filter = new StringToWordVector();
		String[] options = new String[] { "-C" };
		filter.setOptions(options);
		this.trainingData = wordBagsToWordFrequencies(wordBags);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.trainingData);
		saver.setFile(new File(pathToSaveTrainingData));
		saver.writeBatch();

		classifier.buildClassifier(this.trainingData);
	}

	private Instances wordBagsToWordFrequencies(Instances wordBags) throws Exception {
		assert (filter != null);
		filter.setInputFormat(wordBags);
		return Filter.useFilter(wordBags, filter);
	}

	// training data would be numeric, each source is the name of a word, and
	// it's value is the number of occurrences in the word bag
	public SimpleOpenEval(Instances wordBagTrainingData, String keyword) throws Exception {
		// search = new BingSearchJSON();
		search = new GoogleCSESearchJSON();
		classifier = new SMO();
		this.keyword = keyword;
		// TODO add search engine as paramater, and mark training data
		// accordingly
		filter = new StringToWordVector();
		// set the option so that word count is the measure of freq.
		String[] options = new String[] { "-C" };
		try {
			filter.setOptions(options);
		} catch (Exception e) {
			e.printStackTrace();
			// Should not happen
			throw new RuntimeException(e);
		}
		// TODO: check how to copy word bag
		this.trainingData = wordBagTrainingData;
		classifier.buildClassifier(this.trainingData);
	}

	private Instances createTrainingData(List<String> positiveExamples, List<String> negativeExamples) {
		// these maps point from examples given, to a list of articles found
		// about said example
		Map<String, List<String>> positiveExampleText = new HashMap<String, List<String>>();
		Map<String, List<String>> negativeExampleText = new HashMap<String, List<String>>();
		mapExamplesToText(positiveExampleText, positiveExamples);
		mapExamplesToText(negativeExampleText, negativeExamples);

		List<String> positiveWordBags = mapOfTextToBags(positiveExampleText);
		List<String> negativeWordBags = mapOfTextToBags(negativeExampleText);

		// Create a corpus attribute of the Weka type string attribute
		List<String> textValues = null;
		Attribute textCorpus = new Attribute("corpus", textValues);

		// Create a nominal attribute to function as the Class attribute
		List<String> classValues = new ArrayList<String>(2);
		classValues.add("true");
		classValues.add("false");
		Attribute classAttribute = new Attribute("class", classValues);

		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(2);
		featureVector.add(textCorpus);
		featureVector.add(classAttribute);

		Instances posOrNegWordBags = new Instances("posOrNegWordBags", featureVector,
				positiveWordBags.size() + negativeWordBags.size());

		addWordBagInstances(posOrNegWordBags, "true", positiveWordBags, featureVector);
		addWordBagInstances(posOrNegWordBags, "false", negativeWordBags, featureVector);

		// TODO remove later
		ArffSaver saver = new ArffSaver();
		saver.setInstances(posOrNegWordBags);
		try {
			saver.setFile(new File("./rawResults.arff"));
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return posOrNegWordBags;
	}

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

	private List<String> mapOfTextToBags(Map<String, List<String>> exampleText) {
		List<String> wordBags = new ArrayList<String>();

		for (String key : exampleText.keySet()) {
			List<String> texts = new ArrayList<String>(exampleText.get(key));
			// to lowercase, remove keyword, remove search terms, remove stop
			// words
			for (int x = 0; x < texts.size(); x++) {
				String text = texts.get(x).toLowerCase();
				text = text.replaceAll("[^\\w0-9-]", " ");
				List<String> textAsList = new ArrayList<String>(Arrays.asList(text.split("\\s++")));
				// textAsList.removeAll(this.stopWords);
				Set<String> searchTermsAndKeywords = new HashSet<String>(Arrays.asList(key.toLowerCase().split(" +")));
				searchTermsAndKeywords.add(this.keyword.toLowerCase());
				// textAsList.removeAll(searchTermsAndKeywords);
				wordBags.addAll(textAsListToWordBags(textAsList, searchTermsAndKeywords));
			}
		}

		return wordBags;
	}

	private List<String> textAsListToWordBags(List<String> textAsList, Set<String> neededWords) {
		// we are presently looking at the first word in the text
		int currFirstWord = 0;
		/*
		 * this list contains all the word bags found The word bags contain all
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

	private void mapExamplesToText(Map<String, List<String>> exampleText, List<String> examples) {
		for (String example : examples) {
			List<String> texts = getText(example);

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

	@Override
	public Opinion<Boolean, double[]> getOpinion(String args) throws UnknownException {
		List<String> text = getText(args);
		
		return null;
	}

	@Override
	public double[] getTrust(String args, Optional<Boolean> value) {
		return null;
	}

	// Adds the keyword to the example and searches
	private List<String> getText(String example) {
		List<String> texts = new ArrayList<String>();

		SearchResults results = null;
		
		try {
			results = search.search(this.keyword + " " + example);
		} catch (IOException e) {
			// TODO: change this to something better
			System.out.println(example + " failed to be searched");
			e.printStackTrace();
		}

		if (results == null) {
			return texts;
		}

		for (SearchResult result : results) {
			String link = result.getLink();
			String linkContents = readLink(link);
			texts.add(linkContents);
		}

		return texts;
	}

	// link must have http:// or https://
	private String readLink(String link) {
		try {
			Document doc = Jsoup.connect(link).get();
			return doc.body().text();
		} catch (Exception e) {
			// TODO improve failure
			System.out.println("Reading " + link + " failed");
			e.printStackTrace();
			return "";
		}
	}
	
	public int getPagesToCheck(){
		return pagesToCheck;
	}
	
	public void setPagesToCheck(int numOfPages){
		if(numOfPages<0 || numOfPages > 10){
			throw new IllegalArgumentException();
		}
		this.pagesToCheck = numOfPages;
	}
}
