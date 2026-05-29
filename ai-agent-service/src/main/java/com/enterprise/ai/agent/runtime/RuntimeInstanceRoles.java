package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.registry.ProjectInstanceEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RuntimeInstanceRoles {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RuntimeInstanceRoles() {
    }

    public static boolean isCapabilityHost(ProjectInstanceEntity entity) {
        if (entity == null) {
            return false;
        }
        Map<String, Object> metadata = parseMetadata(entity.getMetadataJson());
        String placement = text(metadata.get("runtimePlacement"));
        if ("CAPABILITY_HOST".equalsIgnoreCase(placement)) {
            return true;
        }
        Object runtimeTypes = metadata.get("runtimeTypes");
        if (runtimeTypes instanceof List<?> list) {
            for (Object runtimeType : list) {
                if (containsCapabilityHost(runtimeType)) {
                    return true;
                }
            }
            return false;
        }
        return containsCapabilityHost(runtimeTypes);
    }

    private static boolean containsCapabilityHost(Object value) {
        String text = text(value);
        return text != null && text.toUpperCase(Locale.ROOT).contains("CAPABILITY_HOST");
    }

    private static Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
