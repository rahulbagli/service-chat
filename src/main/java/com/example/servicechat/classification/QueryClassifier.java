package com.example.servicechat.classification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.*;
import weka.core.converters.ConverterUtils;

import java.lang.invoke.MethodHandles;

@Component
public class QueryClassifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public String fetchQueryIntent(String query) {
        QueryClassifier queryClassifier = new QueryClassifier();
        PredictedQueryClassify predictedQueryClassify = new PredictedQueryClassify();
        String classIntent = null;
        try {
            Instances trainDataSetInstance = queryClassifier.loadModal();
            Classifier classifier = queryClassifier.getClassifier();
            Instances instance = queryClassifier.evaluateTrainingInstance(trainDataSetInstance, classifier, predictedQueryClassify);
            classIntent = predictedQueryClassify.predictedQueryClassify(query, instance, trainDataSetInstance, classifier);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return classIntent;
    }

    private Instances loadModal() throws Exception {
        ConverterUtils.DataSource source = new ConverterUtils.DataSource("training.arff");
        Instances trainDataSetInstance = source.getDataSet();
        trainDataSetInstance.setClassIndex(trainDataSetInstance.numAttributes() - 1);
        return trainDataSetInstance;
    }

    private Classifier getClassifier() throws Exception {
        String classString = "weka.classifiers.bayes.NaiveBayes";
        return AbstractClassifier.forName(classString, null);
    }

    private Instances evaluateTrainingInstance(Instances trainDataSetInstance, Classifier classifier, PredictedQueryClassify predictedQueryClassify) throws Exception {
        Instances instance = predictedQueryClassify.filterText(trainDataSetInstance);
        classifier.buildClassifier(instance);
        Evaluation evaluation = new Evaluation(trainDataSetInstance);
        try {
            evaluation.evaluateModel(classifier, trainDataSetInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
      //  printClassifierAndEvaluation(classifier, evaluation);
        return instance;
    }

    private StringBuffer printClassifierAndEvaluation(Classifier thisClassifier, Evaluation thisEvaluation) {
        StringBuffer result = new StringBuffer();
        try {
            LOGGER.info("INFORMATION ABOUT THE CLASSIFIER AND EVALUATION:\n");
            LOGGER.info("\nclassifier.toString():\n" + thisClassifier.toString() + "\n");
            LOGGER.info("\nevaluation.toSummaryString(title, false):\n" + thisEvaluation.toSummaryString("Summary", false) + "\n");
            LOGGER.info("\nevaluation.toMatrixString():\n" + thisEvaluation.toMatrixString() + "\n");
            LOGGER.info("\nevaluation.toClassDetailsString():\n" + thisEvaluation.toClassDetailsString("Details") + "\n");
            LOGGER.info("\nevaluation.toCumulativeMarginDistribution:\n" + thisEvaluation.toCumulativeMarginDistributionString() + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("\nException (sorry!):\n" + e.toString());
        }
        return result;
    }
}
