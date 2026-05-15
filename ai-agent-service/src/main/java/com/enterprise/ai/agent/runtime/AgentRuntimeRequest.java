package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.agent.AgentDefinition;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgentRuntimeRequest {

    private String traceId;

    private String sessionId;

    private String userId;

    @Singular
    private List<String> roles;

    private String message;

    private String intentType;

    private AgentDefinition agentDefinition;

    private Map<String, Object> runtimeOptions;

    private Map<String, Object> metadata;
}
