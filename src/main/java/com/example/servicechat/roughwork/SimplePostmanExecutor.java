package com.example.servicechat.roughwork;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SimplePostmanExecutor {

    public static void main(String[] args) {
        try {
            // Change this to your collection file path
            String collectionPath = "D:/postman/ChatBot.postman_collection.json";

            ObjectMapper mapper = new ObjectMapper();
            JsonNode collection = mapper.readTree(new File(collectionPath));

            // Get all requests from collection
            JsonNode items = collection.path("item");

            // Execute each request
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

            // Get method and URL
            String method = request.path("method").asText();
            String url = getUrl(request.path("url"));
            String body = getBody(request.path("body"));
            System.out.println("\n" + "=".repeat(50));

            System.out.println("Request: " + item.path("name").asText());
            System.out.println("Body: " + body);
            System.out.println("Method: " + method);
            System.out.println("URL: " + url);

            HttpClient client = HttpClient.newHttpClient();

            if (method.equals("GET")) {
                executeGet(client, url, request);
            } else if (method.equals("POST")) {
                executePost(client, url, request);
            }

        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    static void executeGet(HttpClient client, String url, JsonNode request) throws Exception {
        // Build GET request
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        // Add headers if any
        addHeaders(builder, request);

        // Send request
        HttpResponse<String> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        printResponse(response);
    }

    static void executePost(HttpClient client, String url, JsonNode request) throws Exception {
        // Get body content
        String body = getBody(request.path("body"));

        // Build POST request
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        // Add headers
        addHeaders(builder, request);

        // Add Content-Type if not present
        builder.header("Content-Type", "application/json");

        // Send request
        HttpResponse<String> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        printResponse(response);
    }

    static void addHeaders(HttpRequest.Builder builder, JsonNode request) {
        JsonNode headers = request.path("header");
        if (headers.isArray()) {
            for (JsonNode header : headers) {
                String key = header.path("key").asText();
                String value = header.path("value").asText();
                if (!key.isEmpty()) {
                    builder.header(key, value);
                }
            }
        }
    }

    static String getUrl(JsonNode urlNode) {
        // Simple case: URL is just a string
        if (urlNode.isTextual()) {
            return urlNode.asText();
        }

        // Complex case: URL is an object
        StringBuilder url = new StringBuilder();

        // Protocol
        url.append(urlNode.path("protocol").asText("http")).append("://");

        // Host
        JsonNode host = urlNode.path("host");
        if (host.isArray()) {
            for (int i = 0; i < host.size(); i++) {
                url.append(host.get(i).asText());
                if (i < host.size() - 1) url.append(".");
            }
        }

        // Path
        JsonNode path = urlNode.path("path");
        if (path.isArray()) {
            for (JsonNode segment : path) {
                url.append("/").append(segment.asText());
            }
        }

        // Query parameters
        JsonNode query = urlNode.path("query");
        if (query.isArray() && query.size() > 0) {
            url.append("?");
            for (int i = 0; i < query.size(); i++) {
                JsonNode param = query.get(i);
                url.append(param.path("key").asText())
                        .append("=")
                        .append(param.path("value").asText());
                if (i < query.size() - 1) url.append("&");
            }
        }

        return url.toString();
    }

    static String getBody(JsonNode bodyNode) {
        String mode = bodyNode.path("mode").asText("");

        if (mode.equals("raw")) {
            return bodyNode.path("raw").asText("");
        }

        return "";
    }

    static void printResponse(HttpResponse<String> response) {
        System.out.println("\nResponse:");
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}