package com.enterprise.ai.agent.credential;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WorkflowCredentialRuntime {
    private String credentialRef;
    private String name;
    private String type;
    private Map<String, Object> secret;
}
