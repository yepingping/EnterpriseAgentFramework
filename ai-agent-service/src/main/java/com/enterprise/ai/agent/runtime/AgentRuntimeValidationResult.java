package com.enterprise.ai.agent.runtime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRuntimeValidationResult {

    private boolean valid;

    private String runtimeType;

    private String modelInstanceId;

    private String modelType;

    private String provider;

    private String message;

    private String errorCode;
}
