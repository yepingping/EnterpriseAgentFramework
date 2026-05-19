package com.enterprise.ai.agent.credential;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class WorkflowCredentialResponse {
    private Long id;
    private String credentialRef;
    private String name;
    private String type;
    private Long projectId;
    private String projectCode;
    private String scope;
    private String status;
    private Map<String, Object> secretPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
