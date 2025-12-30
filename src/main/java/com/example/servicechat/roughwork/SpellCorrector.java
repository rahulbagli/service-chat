package com.example.servicechat.roughwork;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

public class SpellCorrector {

    private static SpellChecker spellChecker = null;

    public SpellCorrector() throws Exception {
        Path indexDir = Paths.get("spell-index");
        FSDirectory directory = FSDirectory.open(indexDir);

        // Build index if not exists
        if (!DirectoryReader.indexExists(directory)) {
            buildSpellIndex(indexDir);
        }

        spellChecker = new SpellChecker(directory);
        spellChecker.setAccuracy(0.7f); // stricter accuracy for better matches
    }

    private void buildSpellIndex(Path indexDir) throws Exception {
        InputStream dictStream = getClass().getClassLoader().getResourceAsStream("dictionary.txt");

        if (dictStream == null) {
            throw new IllegalStateException("dictionary.txt not found in resources folder");
        }

        PlainTextDictionary dictionary = new PlainTextDictionary(new InputStreamReader(dictStream));

        try (SpellChecker checker = new SpellChecker(FSDirectory.open(indexDir))) {
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            checker.indexDictionary(dictionary, config, true);
        }
    }

    public static String correctQuery(String query) {
        StringBuilder correctedQuery = new StringBuilder();

        for (String word : query.toLowerCase().split("\\s+")) {
            try {
                // Step 1: Exact match check
                if (spellChecker.exist(word)) {
                    correctedQuery.append(word);
                } else {
                    // Step 2: Fallback to similar suggestions
                    String[] suggestions = spellChecker.suggestSimilar(word, 5);

                    if (suggestions.length > 0) {
                        // Prefer prefix matches first, then longest word
                        String chosen = Arrays.stream(suggestions)
                                .filter(s -> s.startsWith(word)) // prioritize prefix
                                .findFirst()
                                .orElse(Arrays.stream(suggestions)
                                        .max(Comparator.comparingInt(String::length)) // fallback: longest
                                        .orElse(suggestions[0]));

                        correctedQuery.append(chosen);
                    } else {
                        correctedQuery.append(word); // no suggestion, keep original
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            correctedQuery.append(" ");
        }

        return correctedQuery.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        SpellCorrector corrector = new SpellCorrector();

        String input = "rewards redemption cont";
        String output = corrector.correctQuery(input);

        System.out.println("Input : " + input);
        System.out.println("Output: " + output);
    }
}