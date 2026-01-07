package com.example.servicechat.roughwork;

import com.example.servicechat.model.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.File;

public class SimplePostmanExecutor {

    public static void main(String[] args) {
        try {
            String collectionPath = "D:/postman/ChatBot.postman_collection.json";
            ObjectMapper mapper = new ObjectMapper();
            JsonNode collection = mapper.readTree(new File(collectionPath));
            JsonNode items = collection.path("item");
            for (JsonNode item : items) {
                executeRequest(item);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void executeRequest(JsonNode item) {
        try {
            JsonNode request = item.path("request");

            String method = request.path("method").asText();
            String url = request.path("url").get("raw").asText();
            String body = getBody(request.path("body"));
            System.out.println("\n" + "=".repeat(50));

            System.out.println("Request: " + item.path("name").asText());
            System.out.println("Body: " + body);
            System.out.println("Method: " + method);
            System.out.println("URL: " + url);

            if (method.equals("POST")) {
                executePost( url, request);
            } else if (method.equals("GET")) {
                executeGet(url);
            }
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    static void executeGet(String url) {
        RestClient restClient = RestClient.create();
        ResponseEntity<String> getResponse = restClient.get()
                .uri(url)
                .retrieve()
                .toEntity(String.class);
        System.out.println("GET Response: " + getResponse.getBody());
        printResponse(getResponse);
    }

    static void executePost(String url, JsonNode request) throws Exception {
        String body = getBody(request.path("body"));
        RestClient restClient = RestClient.create();
        ResponseEntity<ChatResponse> postResponse = restClient.post()
                .uri(url)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body)
                .retrieve()
                .toEntity(ChatResponse.class);
        System.out.println("POST Response: " + postResponse.getBody().getResponseText());
    }

    static String getBody(JsonNode bodyNode) {
        String mode = bodyNode.path("mode").asText("");
        if (mode.equals("raw")) {
            return bodyNode.path("raw").asText("");
        }
        return "";
    }

    static void printResponse(ResponseEntity<String> response) {
        System.out.println("\nResponse:");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
    }
}