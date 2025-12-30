package com.example.servicechat.service;

import com.example.servicechat.config.ChatConfig;
import com.example.servicechat.model.SessionState;
import com.example.servicechat.utility.GenerateQueryToken;
import com.example.servicechat.utility.JsonMapLoader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.example.servicechat.constants.AppConstants.ENVIRONMENTS;

@Service
public class FullyAndPartiallyMatched {

    @Autowired
    private ChatConfig chatConfig;

    private final Map<String, List<String>> serviceOperations = JsonMapLoader.load("service-operation.json");

    private List<String> tokenize(String text) {
        Annotation annotation = new Annotation(text.toLowerCase());
        chatConfig.getPipeline().annotate(annotation);

        List<String> tokens = new ArrayList<>();
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                tokens.add(token.lemma());
            }
        }
        return tokens;
    }

    /** Basic fuzzy similarity scoring */
    private int similarity(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        if (a.equals(b)) return 100;
        if (b.contains(a)) return 50;

        int score = 0;
        for (String part : a.split("-")) {
            if (b.contains(part)) score += 10;
        }

        for (String aWord : a.split("[-\\s]")) {
            for (String bWord : b.split("[-\\s]")) {
                if (aWord.equals(bWord)) score += 20;
                else if (aWord.length() > 2 && bWord.contains(aWord)) score += 5;
            }
        }
        return score;
    }

    public Map<String, String> extractEntities(String text, SessionState session, GenerateQueryToken queryTokenUtil) {

        List<String> tokens = Arrays.stream(text.split(" ")).toList();
        Map<String, String> entities = session.getProvidedIntentField() != null ? session.getProvidedIntentField() : new HashMap<>();

        if(session.getProvidedIntentField().get("service") == null) {
            List<String> suggestions = queryTokenUtil.getSuggestedServiceOperations(text);
            for (String svc : suggestions) {
                String normalized = svc.replaceAll("[-\\s]+", " ").trim();
                if (normalized.equalsIgnoreCase(text)) {
                    entities.put("service", normalized.replaceAll(" ", "-"));
                    break;
                }
                boolean matched = getAllServices().stream()
                        .map(service -> service.replaceAll("-", " "))
                        .anyMatch(normalized::equalsIgnoreCase);
                if (matched) {
                    entities.put("service", normalized.replaceAll(" ", "-"));
                    break;
                }
            }
        }

        if(session.getProvidedIntentField().get("operation") == null) {
            String service = entities.get("service");
            String matchedOperation = matchOperation(tokens, service);

            if (matchedOperation != null) {
                entities.put("operation", matchedOperation);
            }
        }
        if(session.getProvidedIntentField().get("environment") == null) {
            Optional.ofNullable(matchEnvironment(tokens)).ifPresent(e -> entities.put("environment", e));
        }
        return entities;
    }

    private String matchOperation(List<String> tokens, String service) {
        if (service == null) return null;
        List<String> ops = serviceOperations.getOrDefault(service, List.of());

        for (String op : ops) {
            boolean hasExactMatch = false;
            for (String t : tokens) {
                if (similarity(op, t) == 100) {
                    hasExactMatch = true;
                    break; // no need to check further tokens
                }
            }
            if (hasExactMatch) {
                return op;
            }
        }
        return null;
    }

    private String matchEnvironment(List<String> tokens) {
        return tokens.stream()
                .filter(ENVIRONMENTS::contains)
                .findFirst()
                .orElse(null);
    }

    /** Utility getters */
    public List<String> getAllServices() {
        return new ArrayList<>(serviceOperations.keySet());
    }

    public List<String> getOperationsForService(String service) {
        return serviceOperations.getOrDefault(service, List.of());
    }

    /** Suggest operations based on fuzzy matching */
    public List<String> getSuggestedOperations(String text, String service) {
        if (text == null || text.isBlank() || service == null) return List.of();

        List<String> ops = serviceOperations.getOrDefault(service, List.of());
        if (ops.isEmpty()) return List.of();

        String query = text.toLowerCase();
        List<String> tokens = tokenize(query);

        Map<String, Integer> scores = new HashMap<>();

        for (String op : ops) {
            int score = 0;
            String opLower = op.toLowerCase();

            if (opLower.contains(query)) score += 100;
            if (query.length() > 2 && opLower.contains(query)) score += 50;

            for (String token : tokens) {
                score += similarity(token, op);
            }

            if (score > 0) scores.put(op, score);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }
}