package com.enterprise.ai.agent.assist;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts files as object array or string path shorthand array for page assistant register.
 */
public class PageAssistantFileEvidenceListDeserializer extends JsonDeserializer<List<AiAccessSessionService.PageAssistantFileEvidence>> {

    @Override
    public List<AiAccessSessionService.PageAssistantFileEvidence> deserialize(JsonParser parser,
                                                                              DeserializationContext context)
            throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token != JsonToken.START_ARRAY) {
            throw context.weirdStringException(parser.getText(), List.class,
                    "files must be an array of path strings or file evidence objects");
        }
        JsonNode array = parser.getCodec().readTree(parser);
        List<AiAccessSessionService.PageAssistantFileEvidence> result = new ArrayList<>();
        for (JsonNode node : array) {
            if (node == null || node.isNull()) {
                continue;
            }
            if (node.isTextual()) {
                String path = node.asText("");
                if (path.isBlank()) {
                    continue;
                }
                result.add(new AiAccessSessionService.PageAssistantFileEvidence(path.trim(), "unknown", null, null));
                continue;
            }
            if (!node.isObject()) {
                throw context.weirdStringException(node.toString(), AiAccessSessionService.PageAssistantFileEvidence.class,
                        "files entries must be strings or objects with a path field");
            }
            JsonNode pathNode = node.get("path");
            if (pathNode == null || pathNode.isNull() || !pathNode.isTextual() || pathNode.asText("").isBlank()) {
                continue;
            }
            String role = textOrDefault(node.get("role"), "unknown");
            Boolean exists = node.has("exists") && !node.get("exists").isNull() ? node.get("exists").asBoolean() : null;
            String sha256 = textOrNull(node.get("sha256"));
            result.add(new AiAccessSessionService.PageAssistantFileEvidence(
                    pathNode.asText("").trim(),
                    role,
                    exists,
                    sha256));
        }
        return result;
    }

    private static String textOrDefault(JsonNode node, String fallback) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return fallback;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? fallback : value;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? null : value;
    }
}
