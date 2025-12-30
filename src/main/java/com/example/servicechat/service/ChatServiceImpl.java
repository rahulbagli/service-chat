package com.example.servicechat.service;

import com.example.servicechat.classification.QueryClassifier;
import com.example.servicechat.model.ChatResponse;
import com.example.servicechat.model.SessionState;
import com.example.servicechat.utility.GenerateQueryToken;
import com.example.servicechat.utility.JsonMapLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import static com.example.servicechat.constants.AppConstants.*;

@Service
@SessionScope
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SessionState session = new SessionState();
    private final Map<String, List<String>> intentFieldMap = JsonMapLoader.load("intent-fields.json");

    @Autowired
    private FullyAndPartiallyMatched fullyAndPartiallyMatched;
    @Autowired
    private ProcessIntentExecution intentExecutor;
    @Autowired
    private QueryClassifier classifier;
    @Autowired
    private GenerateQueryToken queryTokenUtil;

    @Override
    public ChatResponse handleUserMessage(String userInput) {
        if (isResetCommand(userInput)) {
            log.info("Reset command detected. Resetting session.");
            session.reset();
            return buildResponse(200, "Conversation reset. How can I help you?");
        }
        String normalizedQuery = preprocessQuery(userInput);
        log.info("Normalized Query: {}", normalizedQuery);

        if (session.getIntent() == null) {
            log.info("No intent in session. Starting new conversation.");
            ChatResponse response = startConversation(normalizedQuery);
            log.info("StartConversation result: intent={}, responseCode={}, responseMessage={}",
                    response.getQueryIntent(), response.getResponseCode(), response.getResponseText());
            return response;
        }

        log.info("Continuing conversation with existing intent: {}", session.getIntent());
        return continueConversation(normalizedQuery);
    }

    private ChatResponse continueConversation(String userInput) {
        session.setCurrentUserText(userInput);
        updateSessionWithEntities(userInput);

        if (!isSessionComplete()) {
            if (hasMissingFields()) {
                String prompt = getMissingFieldPrompt();
                log.info("Missing field detected. Prompting user: {}", prompt);
                return buildResponse(206, prompt);
            }
            log.warn("Session incomplete but no missing field prompt available");
            return new ChatResponse(
                    400,
                    "Showing some error. Please try again",
                    null,
                    null
            );
        }

        ChatResponse response = intentExecutor.executeIntent(session);
        if (response == null) {
            log.error("Intent execution returned null");
            return new ChatResponse(
                    500,
                    "Unexpected error occurred",
                    null,
                    null
            );
        }
        session.reset();
        return response;

    }

    private ChatResponse startConversation(String userInput) {
        log.info("Starting conversation with input: {}", userInput);
        String intent = classifier.fetchQueryIntent(userInput);
        if (intent == null || intent.equals("?"))
            return buildResponse(400, "I didn't understand. Please rephrase your query.");

        log.info("Detected intent: {}", intent);
        session.setIntent(intent);
        session.setInitialUserText(userInput);
        session.getRequiredIntentFields().clear();
        session.getRequiredIntentFields().addAll(intentFieldMap.get(intent));

        updateSessionWithEntities(userInput);

        if (isSessionComplete()) {
            return intentExecutor.executeIntent(session);
        }

        if (hasMissingFields()) {
            String prompt = getMissingFieldPrompt();
            return buildResponse(206, prompt);
        }

        return buildResponse(400, "Unable to proceed with the request");
    }

    private void updateSessionWithEntities(String text) {
        log.info("Extracting entities from text: {}", text);
        Map<String, String> entities = fullyAndPartiallyMatched.extractEntities(text, session, queryTokenUtil);
        for (String field : session.getRequiredIntentFields()) {
            if (!session.getProvidedIntentField().containsKey(field)) {
                String value = entities.get(field);
                if (value != null && !value.isEmpty()) {
                    session.getProvidedIntentField().put(field, value);
                    log.info("Entity extracted for field '{}': {}", field, value);
                }
            }
        }
        log.info("Updated session fields: {}", session.getProvidedIntentField());
    }

    private boolean hasMissingFields() {
        for (String field : session.getRequiredIntentFields()) {
            if (!session.getProvidedIntentField().containsKey(field)) {
                return true;
            }
        }
        return false;
    }

    private String getMissingFieldPrompt() {
        for (String field : session.getRequiredIntentFields()) {
            if (!session.getProvidedIntentField().containsKey(field)) {
                log.info("Building prompt for missing field: {}", field);
                return buildPrompt(field);
            }
        }
        return null;
    }

    private String buildPrompt(String field) {
        return switch (field) {
            case "service" -> buildServicePrompt();
            case "operation" -> buildOperationPrompt();
            case "environment" -> "Which environment? Options: dev, sit, uat, prod";
            case "correlationid" -> buildCorrelationPrompt();

            default -> "Provide " + field;
        };
    }

    private String buildCorrelationPrompt() {
        String cid = session.getCurrentUserText().replaceAll(" ", "-");
        cid = UUID_REGEX.matcher(cid).matches() ? cid : extractCorrelationId(cid);

        if (cid != null) {
            session.getProvidedIntentField().put("correlationid", cid);
            log.info("Correlation ID extracted and stored: {}", cid);
            return "Correlation ID captured: " + cid;
        } else {
            return "Please provide the correlation ID (UUID or token).\nExample: 550e8400-e29b-41d4-a716-465400";
        }
    }


    private String buildServicePrompt() {
        List<String> suggestions = queryTokenUtil.getSuggestedServiceOperations(session.getInitialUserText());
        return suggestions.isEmpty()
                ? "Which service? Available:\n" + String.join("\n", fullyAndPartiallyMatched.getAllServices())
                : "Which service? Suggestions:\n" + String.join("\n", suggestions);
    }

    private String buildOperationPrompt() {
        String service = session.getProvidedIntentField().get("service");
        log.info("Provided Service name: {}", service);

        if (service == null) return "Which operation?";

        List<String> operations = fullyAndPartiallyMatched.getOperationsForService(service);
        List<String> suggestions = fullyAndPartiallyMatched.getSuggestedOperations(session.getInitialUserText(), service);
        String attemptedOp = fullyAndPartiallyMatched.extractEntities(session.getInitialUserText(), session, queryTokenUtil).get("operation");

        if (attemptedOp != null && !operations.contains(attemptedOp)) {
            return !suggestions.isEmpty()
                    ? String.format("Operation '%s' not found in %s. Did you mean?\n%s", attemptedOp, service, String.join("\n", suggestions))
                    : String.format("Operation '%s' not found in %s. Available:\n%s", attemptedOp, service, String.join("\n", operations));
        }

        return !suggestions.isEmpty()
                ? "Which operation? Suggestions:\n" + String.join("\n", suggestions)
                : "Which operation? Available for " + service + ":\n" + String.join("\n", operations);
    }

    private String preprocessQuery(String query) {
        return queryTokenUtil.checkSpelling(
                queryTokenUtil.stopWords(
                        queryTokenUtil.removeSpecialCharacter(query)));
    }

    private String extractCorrelationId(String text) {
        var matcher = UUID_REGEX.matcher(text);
        if (matcher.find()) return matcher.group();
        matcher = TOKEN_REGEX.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private boolean isSessionComplete() {
        return session.getProvidedIntentField().size() == session.getRequiredIntentFields().size();
    }

    private boolean isResetCommand(String text) {
        return text.equalsIgnoreCase("reset") || text.equalsIgnoreCase("cancel");
    }

    private ChatResponse buildResponse(int code, String message) {
        return new ChatResponse(code, message, session.getIntent(), null);
    }
}