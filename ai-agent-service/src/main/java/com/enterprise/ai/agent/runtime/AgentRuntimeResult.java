package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.model.interactive.UiRequestPayload;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgentRuntimeResult {

    private boolean success;

    private String answer;

    private String runtimeType;

    private String traceId;

    private String agentName;

    @Singular
    private List<String> toolCalls;

    @Singular
    private List<String> steps;

    private UiRequestPayload uiRequest;

    private Map<String, Object> artifacts;

    private Map<String, Object> tokenUsage;

    private Map<String, Object> metadata;

    private String errorCode;

    private String errorMessage;
}
