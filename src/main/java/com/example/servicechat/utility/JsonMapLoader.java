package com.example.servicechat.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class JsonMapLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Map<String, List<String>> load(String jsonFileName) {
        try (InputStream input = JsonMapLoader.class
                .getClassLoader()
                .getResourceAsStream(jsonFileName)) {

            if (input == null) {
                throw new RuntimeException("JSON file not found: " + jsonFileName);
            }

            return OBJECT_MAPPER.readValue(
                    input,
                    new TypeReference<>() {
                    }
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON: " + jsonFileName, e);
        }
    }

    public static Map<String, String> loadJsonAsMap(String jsonFileName) {
        try (InputStream input = JsonMapLoader.class
                .getClassLoader()
                .getResourceAsStream(jsonFileName)) {

            if (input == null) {
                throw new RuntimeException("JSON file not found: " + jsonFileName);
            }
            return OBJECT_MAPPER.readValue(input, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON file: " + jsonFileName, e);
        }
    }

}


