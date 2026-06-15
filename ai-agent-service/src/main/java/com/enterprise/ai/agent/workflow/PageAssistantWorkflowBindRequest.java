package com.enterprise.ai.agent.workflow;

import lombok.Data;

import java.util.List;

@Data
public class PageAssistantWorkflowBindRequest {

    private Long projectId;
    private String projectCode;
    private String agentId;
    private String pageKey;
    private String routePattern;
    private List<String> actionKeys;
}
