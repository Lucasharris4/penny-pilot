package com.pennypilot.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobMatcherTest {

    @Test
    void startsWithPattern() {
        assertTrue(GlobMatcher.matches("STARBUCKS*", "STARBUCKS #1234 SEATTLE WA"));
    }

    @Test
    void containsPattern() {
        assertTrue(GlobMatcher.matches("*COFFEE*", "GOOD MORNING COFFEE SHOP"));
    }

    @Test
    void exactMatch() {
        assertTrue(GlobMatcher.matches("NETFLIX", "NETFLIX"));
    }

    @Test
    void caseInsensitive() {
        assertTrue(GlobMatcher.matches("STARBUCKS*", "starbucks #5678"));
        assertTrue(GlobMatcher.matches("starbucks*", "STARBUCKS #5678"));
    }

    @Test
    void noMatch() {
        assertFalse(GlobMatcher.matches("STARBUCKS*", "DUNKIN DONUTS"));
    }

    @Test
    void endsWithPattern() {
        assertTrue(GlobMatcher.matches("*PHARMACY", "CVS PHARMACY"));
    }

    @Test
    void truncatedMerchantName() {
        assertTrue(GlobMatcher.matches("*STARBUC*", "STARBUC"));
    }
}
