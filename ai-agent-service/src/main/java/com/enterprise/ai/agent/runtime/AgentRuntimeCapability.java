package com.enterprise.ai.agent.runtime;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class AgentRuntimeCapability {

    private String runtimeType;

    private String displayName;

    private String description;

    private boolean available;

    private String unavailableReason;

    @Singular
    private List<String> supportedModelTypes;

    private boolean supportsStreaming;

    private boolean supportsTools;

    private boolean supportsHandoff;

    private boolean supportsGraph;

    private boolean supportsHumanInterrupt;

    private boolean supportsArtifacts;

    private boolean supportsCodeWorkspace;

    private boolean supportsCloudExecution;

    private String securityLevel;
}
