package com.example.servicechat.service;

import com.example.servicechat.model.ChatResponse;
import com.example.servicechat.model.SessionState;
import org.springframework.stereotype.Component;

@Component
public class ProcessIntentExecution {

    public ChatResponse executeIntent(SessionState session) {
        String result = switch (session.getIntent()) {
            case "execute_api" -> String.format("""
                            Executing API
                            Service: %s
                            Operation: %s
                            Environment: %s
                            """,
                    session.getProvidedIntentField().get("service"),
                    session.getProvidedIntentField().get("operation"),
                    session.getProvidedIntentField().get("environment"));

            case "get_postman" -> "Postman Collection for: " + session.getProvidedIntentField().get("service");

            case "get_log" -> String.format("""
                            Fetching log for:
                            Service: %s
                            Environment: %s
                            CorrelationId: %s
                            """,
                    session.getProvidedIntentField().get("service"),
                    session.getProvidedIntentField().get("environment"),
                    session.getProvidedIntentField().get("correlationid"));

            case "get_request_response" -> String.format("Request/Response for %s → %s → %s",
                    session.getProvidedIntentField().get("service"),
                    session.getProvidedIntentField().get("operation"),
                    session.getProvidedIntentField().get("environment"));

            case "get_client_info" -> String.format("""
                            Client info for:
                            Service: %s
                            Operation: %s
                            """,
                    session.getProvidedIntentField().get("service"),
                    session.getProvidedIntentField().get("operation"));

            default -> "Unknown Intent.";
        };

        String intent = session.getIntent();
        String filename = session.getProvidedIntentField().get("service")+".json";
        session.reset();

        return new ChatResponse(200, result, intent, filename);
    }
}
