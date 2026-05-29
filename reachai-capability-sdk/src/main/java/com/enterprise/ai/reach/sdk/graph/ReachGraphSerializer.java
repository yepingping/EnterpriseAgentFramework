package com.enterprise.ai.reach.sdk.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ReachGraphSerializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ReachGraphSerializer() {
    }

    public static String toJson(ReachGraphSpec graphSpec) {
        try {
            return OBJECT_MAPPER.writeValueAsString(graphSpec);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("ReachGraphSpec serialization failed: " + e.getMessage(), e);
        }
    }
}
