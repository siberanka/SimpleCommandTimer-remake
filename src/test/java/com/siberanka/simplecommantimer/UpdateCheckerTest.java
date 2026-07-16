package com.siberanka.simplecommantimer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateCheckerTest {
    @Test
    void extractsReleaseFieldsFromGitHubJson() {
        String json = "{\"tag_name\":\"v1.2.3\",\"html_url\":\"https:\\/\\/github.com\\/siberanka\\/repo\\/releases\\/tag\\/v1.2.3\"}";

        assertEquals("v1.2.3", UpdateChecker.extractJsonString(json, "tag_name"));
        assertEquals("https://github.com/siberanka/repo/releases/tag/v1.2.3",
                UpdateChecker.extractJsonString(json, "html_url"));
    }

    @Test
    void rejectsMissingOrMalformedValues() {
        assertNull(UpdateChecker.extractJsonString("{}", "tag_name"));
        assertNull(UpdateChecker.extractJsonString("{\"tag_name\":\"broken}", "tag_name"));
    }
}
