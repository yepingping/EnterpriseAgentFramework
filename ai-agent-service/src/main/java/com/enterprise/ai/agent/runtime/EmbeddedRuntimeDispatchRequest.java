package com.enterprise.ai.agent.runtime;

import java.util.Map;

public record EmbeddedRuntimeDispatchRequest(
        String projectCode,
        String instanceId,
        String agentKey,
        String message,
        String sessionId,
        String userId,
        Map<String, Object> context,
        Map<String, Object> graphSpec
) {
    public EmbeddedRuntimeDispatchRequest {
        context = context == null ? Map.of() : Map.copyOf(context);
        graphSpec = graphSpec == null ? Map.of() : Map.copyOf(graphSpec);
    }
}
