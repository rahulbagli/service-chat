package com.example.servicechat.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class KeyValueService {

    private final Map<String, String> keyValueMap;

    public KeyValueService() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("src/main/resources/data.json");

        keyValueMap = objectMapper.readValue(file, new TypeReference<>() {});
    }

    public String getValueByKey(String key) {
        return keyValueMap.getOrDefault(key, null);
    }
}
