package com.example.servicechat.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class StringCompare {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static double calculateSimilarity(String s1, String s2) {
        if (s1.length() < s2.length()) {
            String swap = s1;
            s1 = s2;
            s2 = swap;
        }
        int bigLen = s1.length();
        if (bigLen == 0) {
            return 1.0;
        }

        // Calculate edit distance and similarity
        int editDistance = computeLevenshteinDistance(s1, s2);
        BigDecimal similarity = new BigDecimal(bigLen - editDistance)
                .divide(new BigDecimal(bigLen), 2, RoundingMode.HALF_UP);

        return similarity.doubleValue(); // Returns the similarity score as a double with two decimal places
    }

    public static int computeLevenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1))
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public static String findMostCorrectWord(String word, List<String> joinWords) {
        String mostSimilarWord = "";
        double minDistance = Double.MIN_VALUE;
        for(String correctWord : joinWords){
            double distance = calculateSimilarity(correctWord, word);
            if (distance > minDistance) {
                minDistance = distance;
                mostSimilarWord = correctWord;
            }
        }
        LOGGER.debug("Most Similar Word: "+mostSimilarWord);
        return mostSimilarWord;
    }
}
