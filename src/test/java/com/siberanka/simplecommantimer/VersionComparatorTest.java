package com.siberanka.simplecommantimer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparatorTest {
    @Test
    void comparesNumericSegmentsInsteadOfLexicographicText() {
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.9"));
        assertFalse(VersionComparator.isNewer("1.9.9", "1.10.0"));
    }

    @Test
    void acceptsCommonTagPrefixAndMissingPatchSegment() {
        assertTrue(VersionComparator.isNewer("v2.0", "1.9.9"));
        assertFalse(VersionComparator.isNewer("v1.1", "1.1.0"));
    }

    @Test
    void stableReleaseSortsAfterPrerelease() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.1.0-beta.1"));
        assertFalse(VersionComparator.isNewer("1.1.0-beta.1", "1.1.0"));
    }
}
