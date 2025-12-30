package com.example.servicechat.classification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.lang.invoke.MethodHandles;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class PredictedQueryClassify {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Set<String> modelWords = null;

    public String predictedQueryClassify(String testQuery, Instances instance, Instances trainDataSetInstance, Classifier classifier) throws Exception {

        setModelValue(instance);
        String acceptedWord = acceptedWord(testQuery);

        Instances testInstance = new Instances(trainDataSetInstance);
        testInstance.setClass(trainDataSetInstance.classAttribute());

        Instance dataTestInstance = new DenseInstance(2);
        dataTestInstance.setValue(trainDataSetInstance.attribute(0), acceptedWord);
        dataTestInstance.setValue(trainDataSetInstance.classAttribute(), "?");
        testInstance.add(dataTestInstance);

        Instances filteredTests = filterText(testInstance);
        SparseInstance sparseInst = new SparseInstance(filteredTests.instance(testInstance.size() - 1));
        sparseInst.setDataset(filteredTests);

        double classIndex = classifier.classifyInstance(sparseInst);
        String predictedClass = trainDataSetInstance.classAttribute().value((int) classIndex);
        LOGGER.info("Predicted Class: " + predictedClass);
        return predictedClass;
    }

    private void setModelValue(Instances filteredData) {
        modelWords = new HashSet<>();
        Enumeration<?> enumx = filteredData.enumerateAttributes();
        while (enumx.hasMoreElements()) {
            Attribute att = (Attribute) enumx.nextElement();
            String attName = att.name().toLowerCase();
            modelWords.add(attName);
        }
        LOGGER.info(modelWords + " <-modelWords \n");
    }

    private String acceptedWord(String testQuery) {
        StringBuilder acceptedWordsThisLine = new StringBuilder();
        String delimitersStringToWordVector = "\\s.,:'\\\"()?!";
        String[] splittedText = testQuery.split("[" + delimitersStringToWordVector + "]");
        for (String sWord : splittedText) {
            if (modelWords.contains(sWord)) {
                acceptedWordsThisLine.append(sWord).append(" ");
            }
        }
        return acceptedWordsThisLine.toString();
    }


    public Instances filterText(Instances theseInstances) {
        StringToWordVector filter = null;
        int wordsToKeep = 1000;
        Instances filtered = null;
        try {
            filter = new StringToWordVector(wordsToKeep);
            filter.setOutputWordCounts(true);
            filter.setSelectedRange("1");
            filter.setInputFormat(theseInstances);
            filtered = weka.filters.Filter.useFilter(theseInstances, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filtered;
    }
}
