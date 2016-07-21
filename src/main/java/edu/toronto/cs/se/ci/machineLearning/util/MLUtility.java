package edu.toronto.cs.se.ci.machineLearning.util;

import java.io.File;
import java.io.FileNotFoundException;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * This is a static utility class containing methods that may be useful in
 * implementations of Weka ML aggregators.
 * 
 * @author Ian Berlot-Attwell
 *
 */
public class MLUtility {

	/**
	 * Converts a Weka compatible data file (such as ARFF, CSV, or XRFF) to an
	 * instances object.
	 * <p>
	 * Code from: https://weka.wikispaces.com/Use+WEKA+in+your+Java+code
	 * <p>
	 * Note that if no class attribute is explicitly given, then the last
	 * attribute will be considered the class attribute.
	 * 
	 * @param inputFilePath
	 *            The path to the file.
	 * @return The data file as a {@link weka.core.Instances}.
	 * @throws Exception
	 * @throws FileNotFoundException
	 *             Thrown if the {@code inputFilePath} is not a real path, or if
	 *             the path does not lead to a file.
	 */
	public static Instances fileToInstances(String inputFilePath) throws Exception {
		File toRead = new File(inputFilePath);
		if (!(toRead.exists())) {
			throw new FileNotFoundException("The file " + inputFilePath + " was not found.");
		} else if (!toRead.isFile()) {
			throw new FileNotFoundException(inputFilePath + " is not a path to a file.");
		}

		DataSource source = new DataSource(inputFilePath);
		Instances data = source.getDataSet();
		// setting class attribute if the data format does not provide this
		// information
		// For example, the XRFF format saves the class attribute information as
		// well
		if (data.classIndex() == -1)
			data.setClassIndex(data.numAttributes() - 1);
		return data;
	}
}
