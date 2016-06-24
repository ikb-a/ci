package edu.toronto.cs.se.ci.machineLearning.util;

import java.util.Enumeration;
import java.util.List;

import edu.toronto.cs.se.ci.data.Opinion;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class MLUtility {

	/**
	 * Converts a Weka compatible data file (such as ARFF, CSV, or XRFF) to an
	 * instances object.
	 * <p>
	 * Code from: https://weka.wikispaces.com/Use+WEKA+in+your+Java+code
	 * 
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public static Instances fileToInstances(String inputFilePath) throws Exception {
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
