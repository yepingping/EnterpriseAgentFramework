package com.enterprise.ai.agent.credential;

import lombok.Data;

import java.util.Map;

@Data
public class WorkflowCredentialRequest {
    private String credentialRef;
    private String name;
    private String type;
    private Long projectId;
    private String projectCode;
    private String scope;
    private String status;
    private Map<String, Object> secret;
}
