package com.example.servicechat.roughwork;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.List;

public class Runner {

    public static void main(String[] args) throws Exception {

        List<JsonNode> requests =
                PostmanJsonParser.extractRequests(
                        new File("D:/postman/ChatBot.postman_collection.json"));

        for (JsonNode request : requests) {
            PostmanExecutor.execute(request);
        }
    }
}

