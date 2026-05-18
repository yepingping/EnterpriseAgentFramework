package com.enterprise.ai.spring.registry;

import java.util.Map;

/**
 * SDK-declared Agent graph registration payload.
 * <p>
 * Business systems create this through {@link EafGraph}; the registry client
 * serializes it to the control plane, where it becomes an AgentDefinition draft.
 */
public record EafAgentGraph(
        String code,
        String name,
        String description,
        String runtimeType,
        String modelInstanceId,
        String systemPrompt,
        String visibility,
        Map<String, Object> graphSpec,
        Map<String, Object> metadata
) {
}
