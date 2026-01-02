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
            log.info("Reset requested by user");
            session.reset();
            return buildResponse(200, "Conversation reset. How can I help you?");
        }

        if(session.getActualInitialUserText()==null) session.setActualInitialUserText(userInput);

        String normalizedQuery = preprocessQuery(userInput);
        log.info("Normalized input: {}", normalizedQuery);

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

    private ChatResponse startConversation(String userInput) {
        log.info("Starting conversation with input: {}", userInput);
        String intent = classifier.fetchQueryIntent(userInput);
        if (intent == null || "?".equals(intent)) {
            return buildResponse(400, "I didn't understand. Please rephrase your query.");
        }

        log.info("Detected intent: {}", intent);
        session.setIntent(intent);
        session.setInitialUserText(userInput);
        session.getRequiredIntentFields().clear();
        session.getRequiredIntentFields().addAll(intentFieldMap.get(intent));

        fullyAndPartiallyMatched.updateSessionWithRequiredFields(userInput, session, queryTokenUtil);

        if (isSessionComplete()) {
            return intentExecutor.executeIntent(session);
        }

        if (hasMissingFields()) {
            String prompt = getMissingFieldPrompt();
            return buildResponse(206, prompt);
        }

        return buildResponse(400, "Unable to proceed with the request");
    }

    private ChatResponse continueConversation(String userInput) {
        session.setCurrentUserText(userInput);
        fullyAndPartiallyMatched.updateSessionWithRequiredFields(userInput, session,queryTokenUtil);

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
            case "correlationid" ->
                    "Please provide the correlation ID (UUID or token).\nExample: 550e8400-e29b-41d4-a716-465400";
            default -> "Provide " + field;
        };
    }

    private String buildServicePrompt() {
        String text = session.getCurrentUserText() !=null ? session.getCurrentUserText() : session.getInitialUserText();
        List<String> suggestions = queryTokenUtil.getSuggestedServices(text);
        if (!suggestions.isEmpty()) {
            return "Which service? Suggestions:\n" + String.join("\n", suggestions);
        }
        return "Please provide the service name.";
    }

    private String buildOperationPrompt() {
        String text = session.getCurrentUserText() !=null ? session.getCurrentUserText() : session.getInitialUserText();
        String service = session.getProvidedIntentField().get("service");
        log.info("Provided Service name: {}", service);

        if (service == null) return "Which operation?";

        List<String> operations = fullyAndPartiallyMatched.getOperationsForService(service);
        List<String> suggestions = fullyAndPartiallyMatched.getSuggestedOperations(text, service);

        return !suggestions.isEmpty()
                ? "Which operation? Suggestions:\n" + String.join("\n", suggestions)
                : "Which operation? Available operations for " + service + ":\n" + String.join("\n", operations);
    }

    private String preprocessQuery(String query) {
        return queryTokenUtil.checkSpelling(
                queryTokenUtil.stopWords(
                        queryTokenUtil.removeSpecialCharacter(query)));
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