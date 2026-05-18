package com.enterprise.ai.spring.registry;

import java.util.List;
import java.util.Map;

public record EmbeddedRuntimeResult(
        boolean success,
        String answer,
        List<String> steps,
        Map<String, Object> metadata
) {
    public EmbeddedRuntimeResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static EmbeddedRuntimeResult success(String answer) {
        return new EmbeddedRuntimeResult(true, answer, List.of(), Map.of());
    }

    public static EmbeddedRuntimeResult failure(String answer) {
        return new EmbeddedRuntimeResult(false, answer, List.of(), Map.of());
    }
}
