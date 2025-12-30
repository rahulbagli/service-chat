package com.example.servicechat.roughwork;

import java.util.*;
import java.util.stream.Collectors;

public class CosineSimilarityTFIDF {

    private final Set<String> stopwords = Set.of(
            "is", "the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "at", "by"
    );

    /** Tokenize text into lowercase words, remove punctuation and stopwords */
    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-zA-Z0-9\\s]", "")
                        .split("\\s+"))
                .filter(token -> !token.isBlank() && !stopwords.contains(token))
                .collect(Collectors.toList());
    }

    /** Term Frequency (normalized) */
    private Map<String, Double> termFrequency(List<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0.0) + 1.0);
        }
        int total = tokens.size();
        tf.replaceAll((k, v) -> v / total);
        return tf;
    }

    /** Inverse Document Frequency (smooth IDF) */
    private Map<String, Double> computeIDF(List<List<String>> docs) {
        Map<String, Double> idf = new HashMap<>();
        int totalDocs = docs.size();

        Set<String> uniqueTerms = docs.stream().flatMap(List::stream).collect(Collectors.toSet());
        for (String term : uniqueTerms) {
            int count = 0;
            for (List<String> doc : docs) {
                if (doc.contains(term)) count++;
            }
            idf.put(term, Math.log(1 + (double) totalDocs / (1 + count)));
        }
        return idf;
    }

    /** Build TF-IDF vector */
    private Map<String, Double> tfidfVector(List<String> tokens, Map<String, Double> idf) {
        Map<String, Double> tf = termFrequency(tokens);
        Map<String, Double> tfidf = new HashMap<>();
        for (String term : idf.keySet()) {
            tfidf.put(term, tf.getOrDefault(term, 0.0) * idf.get(term));
        }
        return tfidf;
    }

    /** Cosine similarity between two vectors */
    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (String key : v1.keySet()) {
            double a = v1.getOrDefault(key, 0.0);
            double b = v2.getOrDefault(key, 0.0);
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Public API: compute similarity between two sentences */
    public double similarity(String s1, String s2) {
        // Shortcut: identical strings → similarity = 1.0
        if (s1.trim().equalsIgnoreCase(s2.trim())) {
            return 1.0;
        }

        List<String> tokens1 = tokenize(s1);
        List<String> tokens2 = tokenize(s2);

        // Build IDF from just these two sentences
        List<List<String>> docs = List.of(tokens1, tokens2);
        Map<String, Double> idf = computeIDF(docs);

        Map<String, Double> tfidf1 = tfidfVector(tokens1, idf);
        Map<String, Double> tfidf2 = tfidfVector(tokens2, idf);

        return cosineSimilarity(tfidf1, tfidf2);
    }

    /** Example usage */
    public static void main(String[] args) {
        CosineSimilarityTFIDF similarityEngine = new CosineSimilarityTFIDF();

        String s1 = "cuv parner tgs";
        String s2 = "tgs cuv parner";

        double score = similarityEngine.similarity(s1, s2);
        System.out.println("Cosine Similarity = " + score);

        String s3 = "tgs cuv partner rewards";
        double score2 = similarityEngine.similarity(s1, s3);
        System.out.println("Cosine Similarity = " + score2);
    }
}