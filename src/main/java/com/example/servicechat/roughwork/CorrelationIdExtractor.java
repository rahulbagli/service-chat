package com.example.servicechat.roughwork;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CorrelationIdExtractor {

    private static final Pattern FULL_UUID_PATTERN =
            Pattern.compile("\\b[0-9a-fA-F]{8}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{12}\\b");

    // Partial UUID (minimum 2 blocks, prevents random matches)
    private static final Pattern PARTIAL_UUID_PATTERN =
            Pattern.compile("\\b[0-9a-fA-F]{4,8}(-[0-9a-fA-F]{4}){1,4}\\b");

    private CorrelationIdExtractor() {}

    public static Optional<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher fullMatcher = FULL_UUID_PATTERN.matcher(text);
        if (fullMatcher.find()) {
            return Optional.of(fullMatcher.group());
        }
        Matcher partialMatcher = PARTIAL_UUID_PATTERN.matcher(text);
        if (partialMatcher.find()) {
            return Optional.of(partialMatcher.group());
        }

        return Optional.empty();
    }

    public static void main(String[] args){

        System.out.println("Sentence: " + CorrelationIdExtractor.extract("The transaction ID is usb-service-isf-rewards for this order."));

    }
}

