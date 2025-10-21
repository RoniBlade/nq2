package org.example.util.field;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DisplayNameUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String parseDisplayName(String jsonDisplayName, String language) {
        if (jsonDisplayName == null || jsonDisplayName.isEmpty()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(jsonDisplayName);
            if (language == null || language.trim().isEmpty()) {
                return node.path("orig").asText();
            }
            JsonNode langNode = node.path("lang");
            if (langNode.has(language) && !langNode.get(language).asText().isEmpty()) {
                return langNode.get(language).asText();
            }
            return node.path("orig").asText();
        } catch (Exception e) {
            return jsonDisplayName;
        }
    }
}
