package com.example.servicechat.service;

import com.example.servicechat.model.ServiceMatch;
import jakarta.annotation.PostConstruct;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ServiceMatcher {

    private final ServiceIndexBuilder builder;
    private Directory index;
    private StandardAnalyzer analyzer;

    public ServiceMatcher(ServiceIndexBuilder builder) {
        this.builder = builder;
    }

    @PostConstruct
    public void init() throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.index = builder.buildIndex(analyzer); // build index after bean creation
    }


    private static final float NAME_BOOST = 3.0f;
    private static final float OPS_BOOST = 1.5f;
    private static final float MIN_SCORE_THRESHOLD = 0.3f;
    private static final int MAX_RESULTS = 3;

    public List<ServiceMatch> findMatchingServices(String userQuery) {
        try (DirectoryReader reader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());

            // Step 1: Exact match
            List<ServiceMatch> exactMatches = searchExactMatch(searcher, userQuery);
            if (!exactMatches.isEmpty()) {
                return exactMatches;
            }

            // Step 2: Multi-field boosted search
            return searchMultiField(searcher, userQuery);

        } catch (Exception e) {
            throw new RuntimeException("Search error: " + e.getMessage(), e);
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

        Map<String, Float> boosts = Map.of("name", NAME_BOOST, "ops", OPS_BOOST);

        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[]{"name", "ops"}, analyzer, boosts);

        Query query = parser.parse(userQuery);
        TopDocs docs = searcher.search(query, MAX_RESULTS);

        return processResults(searcher, docs);
    }

    private List<ServiceMatch> processResults(IndexSearcher searcher, TopDocs docs) throws IOException {
        if (docs.totalHits.value == 0) {
            return Collections.emptyList();
        }

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
}
