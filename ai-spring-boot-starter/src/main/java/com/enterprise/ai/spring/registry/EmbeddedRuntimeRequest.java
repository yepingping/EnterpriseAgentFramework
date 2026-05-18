package com.enterprise.ai.spring.registry;

import java.util.Map;

public record EmbeddedRuntimeRequest(
        String agentKey,
        String message,
        String sessionId,
        String userId,
        Map<String, Object> context,
        Map<String, Object> graphSpec
) {
    public EmbeddedRuntimeRequest {
        context = context == null ? Map.of() : Map.copyOf(context);
        graphSpec = graphSpec == null ? Map.of() : Map.copyOf(graphSpec);
    }
}
