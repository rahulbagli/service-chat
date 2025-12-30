package com.example.servicechat.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SessionState {

    private String intent;
    private String initialUserText;
    private String currentUserText;
    private Map<String, String> providedIntentField = new HashMap<>();
    private List<String> requiredIntentFields = new ArrayList<>();

    public void reset() {
        intent = null;
        initialUserText = null;
        currentUserText = null;
        providedIntentField.clear();
        requiredIntentFields.clear();
    }
}
