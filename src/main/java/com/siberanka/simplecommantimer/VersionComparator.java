package com.siberanka.simplecommantimer;

import java.util.Locale;

final class VersionComparator {
    private VersionComparator() {
    }

    static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    static int compare(String left, String right) {
        Version a = Version.parse(left);
        Version b = Version.parse(right);
        int max = Math.max(a.parts.length, b.parts.length);
        for (int i = 0; i < max; i++) {
            int aPart = i < a.parts.length ? a.parts[i] : 0;
            int bPart = i < b.parts.length ? b.parts[i] : 0;
            if (aPart != bPart) {
                return aPart < bPart ? -1 : 1;
            }
        }

        if (a.preRelease.isEmpty() && !b.preRelease.isEmpty()) {
            return 1;
        }
        if (!a.preRelease.isEmpty() && b.preRelease.isEmpty()) {
            return -1;
        }
        return a.preRelease.compareTo(b.preRelease);
    }

    private static final class Version {
        private final int[] parts;
        private final String preRelease;

        private Version(int[] parts, String preRelease) {
            this.parts = parts;
            this.preRelease = preRelease;
        }

        private static Version parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("v")) {
                normalized = normalized.substring(1);
            }
            int metadataIndex = normalized.indexOf('+');
            if (metadataIndex >= 0) {
                normalized = normalized.substring(0, metadataIndex);
            }
            String preRelease = "";
            int dashIndex = normalized.indexOf('-');
            if (dashIndex >= 0) {
                preRelease = normalized.substring(dashIndex + 1);
                normalized = normalized.substring(0, dashIndex);
            }
            String[] tokens = normalized.split("\\.");
            int[] parts = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                parts[i] = parsePart(tokens[i]);
            }
            return new Version(parts, preRelease);
        }

        private static int parsePart(String token) {
            if (token.isEmpty()) {
                return 0;
            }
            long result = 0L;
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (c < '0' || c > '9') {
                    break;
                }
                result = Math.min(Integer.MAX_VALUE, (result * 10L) + (c - '0'));
            }
            return (int) result;
        }
    }
}
