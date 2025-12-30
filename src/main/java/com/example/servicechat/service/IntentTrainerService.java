package com.example.servicechat.service;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.ling.Datum;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Service("intentTrainerService")
public class IntentTrainerService {

    private static final String TRAINING_FILE = "intent-training-data.txt";
    private static final String MODEL_FILE = "intent-model.ser.gz";

    private ColumnDataClassifier cdc;

    @PostConstruct
    public void init() {
        try {
            File modelFile = getModelFile();

            if (modelFile != null && modelFile.exists()) {
                // Load existing model
                loadModel(modelFile);
                System.out.println("✓ Loaded existing model: " + modelFile.getAbsolutePath());
            } else {
                // Train and save new model
                System.out.println("Model not found. Training new model...");
                trainAndSaveModel();
            }
        } catch (Exception e) {
            System.err.println("Error initializing IntentTrainerService: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize intent model", e);
        }
    }

    private File getModelFile() {
        try {
            ClassPathResource resource = new ClassPathResource(MODEL_FILE);
            if (resource.exists()) {
                return resource.getFile();
            }
        } catch (IOException e) {
            // Ignore, will try file system
        }

        Path resourcePath = Paths.get("src/main/resources", MODEL_FILE);
        if (Files.exists(resourcePath)) {
            return resourcePath.toFile();
        }

        return null;
    }

    private void loadModel(File modelFile) throws Exception {
        cdc = ColumnDataClassifier.getClassifier(modelFile.getAbsolutePath());
    }

    private void trainAndSaveModel() throws Exception {
        InputStream trainingStream = null;

        try {
            // Try to load training file from classpath
            ClassPathResource resource = new ClassPathResource(TRAINING_FILE);
            if (!resource.exists()) {
                throw new FileNotFoundException(
                        "Training file not found in classpath: " + TRAINING_FILE +
                                "\nPlease create the file in src/main/resources/ with training data in format:\n" +
                                "intent_name\\ttraining text"
                );
            }

            trainingStream = resource.getInputStream();
            System.out.println("✓ Found training file in classpath: " + TRAINING_FILE);

            // Create temporary file for training
            Path tempTrainingFile = Files.createTempFile("intent-training", ".txt");
            Files.copy(trainingStream, tempTrainingFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Initialize classifier and train
            cdc = new ColumnDataClassifier(createProperties());
            cdc.trainClassifier(tempTrainingFile.toString());

            // Save model to file system
            Path resourcePath = Paths.get("src/main/resources", MODEL_FILE);
            Files.createDirectories(resourcePath.getParent());

            cdc.serializeClassifier(resourcePath.toString());

            System.out.println("✓ Model trained and saved → " + resourcePath.toAbsolutePath());

            // Cleanup
            Files.deleteIfExists(tempTrainingFile);

        } finally {
            if (trainingStream != null) {
                trainingStream.close();
            }
        }
    }

    private Properties createProperties() {
        Properties props = new Properties();

        // Define that we have a goldAnswerColumn (the label/intent)
        props.setProperty("goldAnswerColumn", "0");

        // Text is in column 1
        props.setProperty("displayedColumn", "1");

        // Basic configuration for text classification
        props.setProperty("useClassFeature", "true");
        props.setProperty("1.useSplitWords", "true");
        props.setProperty("1.useLowercaseWords", "true");
        props.setProperty("1.usePrefixSuffixNGrams", "true");
        props.setProperty("1.minPrefixLength", "1");
        props.setProperty("1.maxPrefixLength", "3");
        props.setProperty("1.minSuffixLength", "1");
        props.setProperty("1.maxSuffixLength", "3");
        props.setProperty("1.wordShape", "chris2useLC");

        return props;
    }

    public String classifyIntent(String text) {
        if (cdc == null) {
            throw new IllegalStateException("Model not initialized. Check application logs for errors.");
        }

        try {
            // For classification, we need to create a datum with just the text (no label)
            // Use a dummy tab to create proper format: "<unknown>\ttext"
            String formattedText = "unknown\t" + text;
            Datum<String, String> datum = cdc.makeDatumFromLine(formattedText);
        return cdc.classOf(datum);
        } catch (Exception e) {
            System.err.println("Error classifying intent for text: '" + text + "' - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public edu.stanford.nlp.stats.Counter<String> getConfidenceScores(String text) {
        if (cdc == null) {
            throw new IllegalStateException("Model not initialized. Check application logs for errors.");
        }

        try {
            // For classification, we need to create a datum with just the text (no label)
            String formattedText = "unknown\t" + text;
            Datum<String, String> datum = cdc.makeDatumFromLine(formattedText);
        return cdc.scoresOf(datum);
        } catch (Exception e) {
            System.err.println("Error getting confidence scores for text: '" + text + "' - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Getter for the classifier (for compatibility with existing code)
    public ColumnDataClassifier getModel() {
        if (cdc == null) {
            throw new IllegalStateException("Model not initialized. Check application logs for errors.");
        }
        return cdc;
    }
}