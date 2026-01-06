package com.example.servicechat.roughwork;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class PostmanExecutor {

    private static final RestClient CLIENT = RestClient.create();

    public static void execute(JsonNode request) {

        HttpMethod method = HttpMethod.valueOf(request.get("method").asText().toUpperCase());

        String url = request.get("url").get("raw").asText();

        RestClient.RequestBodySpec spec = CLIENT
                .method(method)
                .uri(url)
                .headers(h -> applyHeaders(h, request));

        if (request.has("body") && request.get("body").has("raw") && !request.get("body").get("raw").isNull()) {

            spec = spec
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request.get("body")
                            .get("raw")
                            .asText());
        }

        String response = spec.retrieve().body(String.class);
        System.out.println("Response:\n" + response);
    }

    private static void applyHeaders(HttpHeaders headers, JsonNode request) {
        if (request.has("header")) {
            request.get("header").forEach(h -> headers.add(h.get("key").asText(), h.get("value").asText()));
        }
    }
}

