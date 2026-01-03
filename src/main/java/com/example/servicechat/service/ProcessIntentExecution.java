package com.example.servicechat.service;

import com.example.servicechat.model.ChatResponse;
import com.example.servicechat.model.SessionState;
import org.springframework.stereotype.Component;

@Component
public class ProcessIntentExecution {

    public ChatResponse executeIntent(SessionState session) {

        ChatResponse response = switch (session.getIntent()) {
            case "execute_api" -> executeAPI(session);
            case "get_postman" -> getPostmanCollection(session);
            case "get_log" -> getLogs(session);
            case "get_request_response" -> getRequestResponse(session);
            case "get_client_info" -> getClientInfo(session);
            case "get_vulnerabilities" -> getVulnerabilities(session);
            case "get_last_scan" -> getLastScan(session);
            case "get_api_documentation" -> getApiDocumentation(session);
            default -> unknownIntent(session);
        };
        session.reset();
        return response;
    }

    private ChatResponse executeAPI(SessionState session) {
        String responseText =
                String.format("""
                                Executing API:
                                Service: %s
                                Operation: %s
                                Environment: %s
                                """,
                        session.getProvidedIntentField().get("service"),
                        session.getProvidedIntentField().get("operation"),
                        session.getProvidedIntentField().get("environment")
                );
        String fileName = session.getProvidedIntentField().get("service") + ".json";
        return new ChatResponse(200, responseText, session.getIntent(), fileName);
    }

    private ChatResponse getPostmanCollection(SessionState session) {
        String responseText =
                String.format("""
                                Postman Collection:
                                Service: %s
                                """,
                        session.getProvidedIntentField().get("service")
                );
        String fileName = session.getProvidedIntentField().get("service") + ".json";
        return new ChatResponse(200, responseText, session.getIntent(), fileName);
    }

    private ChatResponse getLogs(SessionState session) {
        String responseText =
                String.format("""
                                Fetching Logs:
                                    Service: %s
                                    Environment: %s
                                    CorrelationId: %s
                                """,
                        session.getProvidedIntentField().get("service"),
                        session.getProvidedIntentField().get("environment"),
                        session.getProvidedIntentField().get("correlationId")
                );
        String fileName = session.getProvidedIntentField().get("service") + ".json";
        return new ChatResponse(200, responseText, session.getIntent(), fileName);
    }

    private ChatResponse getRequestResponse(SessionState session) {
        String responseText =
                String.format("""
                                Request / Response:
                                Service: %s
                                Operation: %s
                                Environment: %s
                                """,
                        session.getProvidedIntentField().get("service"),
                        session.getProvidedIntentField().get("operation"),
                        session.getProvidedIntentField().get("environment")
                );
        String fileName = session.getProvidedIntentField().get("service") + ".json";
        return new ChatResponse(200, responseText, session.getIntent(), fileName);
    }

    private ChatResponse getClientInfo(SessionState session) {
        String responseText =
                String.format("""
                                Client Information:
                                    Service: %s
                                    Client Details: %s
                                """,
                        session.getProvidedIntentField().get("service"),
                        session.getProvidedIntentField().get("operation")
                );
        return new ChatResponse(200, responseText, session.getIntent(), null);
    }

    private ChatResponse getVulnerabilities(SessionState session) {
        String responseText =
                String.format("""
                                Vulnerability Report:
                                    Service: %s
                                    Scan Type: %s
                                """,
                        session.getProvidedIntentField().get("service"),
                        session.getProvidedIntentField().get("scanType")
                );
        return new ChatResponse(200, responseText, session.getIntent(), null);
    }

    private ChatResponse getLastScan(SessionState session) {
        String responseText =
                String.format("""
                                Last Scan Details:
                                    Service: %s
                                    Scan Type: %s
                                """,
                        session.getProvidedIntentField().get("service"),
                        session.getProvidedIntentField().get("scanType")
                );
        return new ChatResponse(200, responseText, session.getIntent(), null);
    }

    private ChatResponse getApiDocumentation(SessionState session) {
        String responseText =
                String.format("""
                                API Documentation:
                                    Service: %s
                                """,
                        session.getProvidedIntentField().get("service")
                );
        String fileName = session.getProvidedIntentField().get("service") + ".json";
        return new ChatResponse(200, responseText, session.getIntent(), fileName);
    }

    private ChatResponse unknownIntent(SessionState session) {
        return new ChatResponse(
                400,
                "Sorry, I couldn’t understand your request.",
                session.getIntent(),
                null
        );
    }
}
