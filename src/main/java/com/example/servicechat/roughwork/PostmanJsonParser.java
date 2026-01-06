package com.example.servicechat.roughwork;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PostmanJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<JsonNode> extractRequests(File file) throws Exception {

        JsonNode root = MAPPER.readTree(file);
        List<JsonNode> requests = new ArrayList<>();

        extractItems(root.get("item"), requests);
        return requests;
    }

    private static void extractItems(JsonNode items, List<JsonNode> requests) {
        if (items == null || !items.isArray()) return;

        for (JsonNode item : items) {
            if (item.has("request")) {
                requests.add(item.get("request"));
            }
            if (item.has("item")) {
                extractItems(item.get("item"), requests);
            }
        }
    }
}

