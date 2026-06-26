package com.enterprise.ai.agent.workflow.aicoding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowAiCodingPublishRequest {

    private String version;

    private Integer rolloutPercent;

    private String note;

    private String publishedBy;
}
