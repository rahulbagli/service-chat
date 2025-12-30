package com.example.servicechat.roughwork;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.*;

public class SmartServiceMatcher {

    private static final Map<String, List<String>> SERVICE_DATA = Map.of(
            "tgs-cuv-rewards-redeem-order", List.of("initRedeem", "redeemOrder", "refundOrder"),
            "tgs-cuv-rewards", List.of("balance", "transactions", "details"),
            "tgs-cuv-rewards-redeem", List.of("wallet", "redeem"),
            "tgs-cuv-rewards-redemption-account", List.of("redemption", "accountDetail"),
            "tgs-cuv-partner-rewards", List.of("summary")
    );

    private static final float NAME_BOOST = 3.0f;
    private static final float OPS_BOOST = 1.5f;
    private static final float MIN_SCORE_THRESHOLD = 0.3f;
    private static final int MAX_RESULTS = 3;

    private final Directory index;
    private final StandardAnalyzer analyzer;

    public SmartServiceMatcher() throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.index = buildIndex();
    }

    public static void main(String[] args) {
        try {
            SmartServiceMatcher matcher = new SmartServiceMatcher();

            // Test cases
            System.out.println("=".repeat(80));
            matcher.findMatchingServices("execute redeem service and wallet");
            System.out.println("=".repeat(80));
            matcher.findMatchingServices("tgs cuv rewards redemption accou");
            System.out.println("=".repeat(80));
            matcher.findMatchingServices("execute and get response of tgs-cuv-partner-rewards");
            System.out.println("=".repeat(80));
            matcher.findMatchingServices("wallet balance");
            System.out.println("=".repeat(80));
            matcher.findMatchingServices("redemption account details");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Directory buildIndex() throws IOException {
        Directory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            for (var entry : SERVICE_DATA.entrySet()) {
                String serviceName = entry.getKey();
                List<String> operations = entry.getValue();

                Document doc = new Document();
                doc.add(new StringField("id", serviceName, Field.Store.YES));
                doc.add(new TextField("name", serviceName, Field.Store.YES));
                String opsText = String.join(" ", operations);
                doc.add(new TextField("ops", opsText, Field.Store.YES));
                writer.addDocument(doc);
            }
        }
        return directory;
    }

    public List<ServiceMatch> findMatchingServices(String userQuery) {
        System.out.println("\n🔍 User Query: \"" + userQuery + "\"");

        try (DirectoryReader reader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());

            // Step 1: Try exact match first
            List<ServiceMatch> exactMatches = searchExactMatch(searcher, userQuery);
            if (!exactMatches.isEmpty()) {
                printResults(exactMatches);
                return exactMatches;
            }

            // Step 2: Multi-field search with boosting
            List<ServiceMatch> matches = searchMultiField(searcher, userQuery);
            printResults(matches);
            return matches;

        } catch (Exception e) {
            System.err.println("❌ Search error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ServiceMatch> searchExactMatch(IndexSearcher searcher, String query) throws IOException {
        Query exactQuery = new TermQuery(new Term("id", query.toLowerCase()));
        TopDocs docs = searcher.search(exactQuery, 1);

        if (docs.totalHits.value > 0) {
            Document doc = searcher.doc(docs.scoreDocs[0].doc);
            return List.of(new ServiceMatch(doc.get("id"), 100.0f, 100.0f, true));
        }
        return Collections.emptyList();
    }

    private List<ServiceMatch> searchMultiField(IndexSearcher searcher, String userQuery)
            throws ParseException, IOException {

        Map<String, Float> boosts = Map.of(
                "name", NAME_BOOST,
                "ops", OPS_BOOST
        );

        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[]{"name", "ops"},
                analyzer,
                boosts
        );

        Query query = parser.parse(userQuery);
        TopDocs docs = searcher.search(query, MAX_RESULTS);

        return processResults(searcher, docs);
    }

    private List<ServiceMatch> processResults(IndexSearcher searcher, TopDocs docs) throws IOException {
        if (docs.totalHits.value == 0) {
            return Collections.emptyList();
        }

        // Find max score for percentage calculation
        float maxScore = Arrays.stream(docs.scoreDocs)
                .map(hit -> hit.score)
                .max(Float::compare)
                .orElse(1.0f);

        List<ServiceMatch> matches = new ArrayList<>();

        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            if (scoreDoc.score >= MIN_SCORE_THRESHOLD) {
                Document doc = searcher.doc(scoreDoc.doc);
                float percentage = (scoreDoc.score / maxScore) * 100.0f;
                matches.add(new ServiceMatch(doc.get("id"), scoreDoc.score, percentage, false));
            }
        }
        matches.sort(Comparator.comparing(ServiceMatch::getScore).reversed());
        return matches;
    }

    private void printResults(List<ServiceMatch> matches) {
        if (matches.isEmpty()) {
            System.out.println("❌ No matches found.");
            return;
        }

        System.out.println("\n✅ All Matching Services (0-100%):");
        System.out.println("-".repeat(80));
        for (int i = 0; i < matches.size(); i++) {
            ServiceMatch match = matches.get(i);
            System.out.printf("%d. %-45s | Score: %6.2f | Match: %6.2f%%%s%n",
                    i + 1,
                    match.getServiceId(),
                    match.getScore(),
                    match.getPercentage(),
                    match.isExactMatch() ? " (EXACT)" : "");
        }
        System.out.println("-".repeat(80));
    }

    public static class ServiceMatch {
        private final String serviceId;
        private final float score;
        private final float percentage;
        private final boolean exactMatch;

        public ServiceMatch(String serviceId, float score, float percentage, boolean exactMatch) {
            this.serviceId = serviceId;
            this.score = score;
            this.percentage = percentage;
            this.exactMatch = exactMatch;
        }

        public String getServiceId() {
            return serviceId;
        }

        public float getScore() {
            return score;
        }

        public float getPercentage() {
            return percentage;
        }

        public boolean isExactMatch() {
            return exactMatch;
        }

        @Override
        public String toString() {
            return String.format("ServiceMatch{id='%s', score=%.2f, percentage=%.2f%%, exact=%s}",
                    serviceId, score, percentage, exactMatch);
        }
    }
}