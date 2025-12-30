package com.example.servicechat.service;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

@Component
public class QuerySpellCorrector {

    private final SpellChecker spellChecker;

    public QuerySpellCorrector() throws Exception {
        Path indexDir = Paths.get("spell-index");
        FSDirectory directory = FSDirectory.open(indexDir);

        if (!DirectoryReader.indexExists(directory)) {
            new SpellIndexBuilder().buildSpellIndex();
        }

        this.spellChecker = new SpellChecker(directory);
        this.spellChecker.setAccuracy(0.7f);
    }

    public String correctQuery(String query) {
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
}
