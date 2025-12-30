package com.example.servicechat.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SpellIndexBuilder {

    public void buildSpellIndex() throws Exception {
        Path indexDir = Paths.get("spell-index");
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
}
