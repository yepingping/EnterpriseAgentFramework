package com.enterprise.ai.agent.runtime;

import java.util.List;
import java.util.Map;

public record EmbeddedRuntimeDispatchResult(
        boolean success,
        String answer,
        String projectCode,
        String instanceId,
        String dispatchUrl,
        List<String> steps,
        Map<String, Object> metadata,
        String errorCode,
        String errorMessage
) {
    public EmbeddedRuntimeDispatchResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static EmbeddedRuntimeDispatchResult failure(String projectCode,
                                                        String instanceId,
                                                        String errorCode,
                                                        String errorMessage) {
        return new EmbeddedRuntimeDispatchResult(
                false,
                errorMessage,
                projectCode,
                instanceId,
                null,
                List.of(),
                Map.of(),
                errorCode,
                errorMessage
        );
    }
}
