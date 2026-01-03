package com.example.servicechat.constants;

import java.util.List;
import java.util.regex.Pattern;

public class AppConstants {

    public static final Pattern UUID_REGEX = Pattern.compile("\\b[0-9a-fA-F]{8}-" +
            "[0-9a-fA-F]{4}-" +
            "[0-9a-fA-F]{4}-" +
            "[0-9a-fA-F]{4}-" +
            "[0-9a-fA-F]{12}\\b");
    public static final Pattern PARTIAL_UUID_REGEX = Pattern.compile("\\b[0-9a-fA-F]{4,8}(-[0-9a-fA-F]{4}){1,4}\\b");
    public static final Pattern SCAN_TYPE = Pattern.compile("\\b(?:black\\s?duck|fortify)\\b", Pattern.CASE_INSENSITIVE);
    public static final List<String> ENVIRONMENTS = List.of("dev", "sit", "uat", "prod");
    public static String STORE_DRIVE =  "D:/";
    public static String POSTMAN =  "postman";
    public static String APPLICATION_LOG =  "logs";
}
