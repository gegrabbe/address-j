package com.glenn.address;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.glenn.address.web.Sanitize;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SanitizeTest {

    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> toodle = Map.of(
            "A", "1",
            "B", "2+2=4",
            "N", "Nonce: {\"allie\":\"foons\"}",
            "S", "Sally's Strut"
    );

    @Test
    void testJackson() {
        try {
            String json = objectMapper.writeValueAsString(toodle);

            // Jackson should preserve single quotes (not escape to \u0027)
            assertFalse(json.contains("\\u0027"), "Jackson should not escape single quotes to \\u0027");
            assertTrue(json.contains("Sally's Strut"), "Jackson should preserve single quotes");

            // Jackson should escape double quotes in JSON strings
            assertTrue(json.contains("\\\""), "Jackson should escape double quotes for valid JSON");

            // Deserialize and verify data integrity
            Map<?, ?> deserialized = objectMapper.readValue(json, Map.class);
            assertEquals(toodle, deserialized, "Original and deserialized maps should be equal");

        } catch (JsonProcessingException e) {
            fail("Jackson serialization should not throw exception", e);
        }
    }

    @Test
    void testSanitize() {
        String asString = toodle.toString();
        String sanitized = Sanitize.fix(asString);

        // Sanitize should escape single quotes to &#39;
        assertTrue(sanitized.contains("Sally&#39;s Strut"),
            "Sanitize should escape single quotes to &#39;");

        // Sanitize should escape double quotes to &quot;
        assertTrue(sanitized.contains("&quot;"),
            "Sanitize should escape double quotes to &quot;");

        // Sanitize should be safe for HTML display
        assertFalse(sanitized.contains("\"allie\""),
            "Sanitize should escape quotes for HTML safety");
    }

    @Test
    void testGson() {
        Gson gson = new Gson();
        String gsonJson = gson.toJson(toodle);

        // Gson escapes single quotes to \u0027 (the problem we replaced)
        assertTrue(gsonJson.contains("\\u0027"),
            "Gson escapes single quotes to \\u0027");

        // This is why we replaced Gson with Jackson
        assertFalse(gsonJson.contains("Sally's Strut"),
            "Gson does not preserve original single quotes");
    }
}
