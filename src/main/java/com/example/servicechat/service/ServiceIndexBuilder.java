package com.example.servicechat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class ServiceIndexBuilder {

    public Directory buildIndex(StandardAnalyzer analyzer) throws IOException {
        Directory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());

        // Load JSON file from resources
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = new ClassPathResource("service-operation.json").getInputStream();
        Map<String, List<String>> serviceData = mapper.readValue(inputStream, new TypeReference<>() {});

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            for (var entry : serviceData.entrySet()) {
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
}