package com.example.servicechat.utility;

import com.example.servicechat.model.ServiceMatch;
import com.example.servicechat.service.QuerySpellCorrector;
import com.example.servicechat.service.ServiceMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GenerateQueryToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private QuerySpellCorrector corrector;
    @Autowired
    private ServiceMatcher serviceMatcher;

    public String stopWords(String query) {
        List<String> stopWords = dataReadFromFile("stopwords.txt");
        String refinedQuery = Arrays.stream(query.split("\\s+"))
                .filter(word -> !stopWords.contains(word.toLowerCase()))
                .collect(Collectors.joining(" "));
        LOGGER.info("QueryPostStopWords: {}", refinedQuery);
        return refinedQuery;
    }


    public String checkSpelling(String query) {
        String correctQuery = corrector.correctQuery(query);
        LOGGER.info("QueryPostCheckSpell: {}", correctQuery);
        return correctQuery;
    }

    private List<String> dataReadFromFile(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        return new BufferedReader(
                new InputStreamReader(inputStream)).lines().toList();
    }

    public String removeSpecialCharacter(String query) {

        String queryPostRemoveSpecialChar = query.replaceAll("[^a-zA-Z0-9\\s]", " ");
        String cleanedQuery= queryPostRemoveSpecialChar.trim().replaceAll("\\s+", " ").trim();
        LOGGER.info("QueryPostRemoveSpecialChar: {}", cleanedQuery);
        return cleanedQuery;
    }

    public List<String> getSuggestedServiceOperations(String query) {
        List<ServiceMatch> matchingServices = serviceMatcher.findMatchingServices(query);
        LOGGER.info("Matching Services:");
        for (int i = 0; i < matchingServices.size(); i++) {
            ServiceMatch match = matchingServices.get(i);
            LOGGER.info(String.format(
                    "%d. Service: %-30s | Score: %.2f | Match: %.2f%%%s",
                    i + 1,
                    match.getServiceId(),
                    match.getScore(),
                    match.getPercentage(),
                    match.isExactMatch() ? " (EXACT)" : ""
            ));
        }



        return matchingServices.isEmpty()
                ? Collections.emptyList()
                : matchingServices.stream()
                .map(ServiceMatch::getServiceId)
                .toList();

    }
}
