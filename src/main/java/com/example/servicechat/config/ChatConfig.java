package com.example.servicechat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class ChatConfig {

    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.setProperty("ner.useSUTime", "0");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, depparse, dcoref");
        props.setProperty("tokenize.whitespace", "true");
        pipeline = new StanfordCoreNLP(props);
    }

    @Bean
    public ObjectMapper getObjectMapper(){
        return new ObjectMapper();
    }

    public StanfordCoreNLP getPipeline() {
        return pipeline;
    }
}
