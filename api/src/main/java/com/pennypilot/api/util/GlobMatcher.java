package com.pennypilot.api.util;

import java.util.regex.Pattern;

/**
 * Utility for matching text against glob-style patterns.
 *
 * Glob syntax:
 *   - {@code *} matches any sequence of characters (including empty)
 *   - All other characters are matched literally
 *   - Matching is always case-insensitive
 *
 * Examples:
 *   - {@code STARBUCKS*} matches "STARBUCKS #1234 SEATTLE WA" (starts-with)
 *   - {@code *COFFEE*} matches "GOOD MORNING COFFEE SHOP" (contains)
 *   - {@code *PHARMACY} matches "CVS PHARMACY" (ends-with)
 *   - {@code NETFLIX} matches "netflix" (exact, case-insensitive)
 */
public final class GlobMatcher {

    private GlobMatcher() {}

    /**
     * Tests whether the given text matches the glob pattern (case-insensitive).
     *
     * @param pattern glob pattern where {@code *} matches any characters
     * @param text    the text to test against the pattern
     * @return true if the text matches the pattern
     */
    public static boolean matches(String pattern, String text) {
        String regex = "(?i)" + globToRegex(pattern);
        return Pattern.matches(regex, text);
    }

    /**
     * Converts a glob pattern to a regex string. Each {@code *} becomes {@code .*},
     * and all other characters are escaped to match literally.
     */
    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }
}
