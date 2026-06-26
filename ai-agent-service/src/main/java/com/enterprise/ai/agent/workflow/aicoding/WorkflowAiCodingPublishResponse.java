package com.enterprise.ai.agent.workflow.aicoding;

import com.enterprise.ai.agent.workflow.WorkflowVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowAiCodingPublishResponse {

    private String workflowId;

    private Long versionId;

    private String version;

    private String status;

    private Integer rolloutPercent;

    private String publishedBy;

    private String publishedAt;

    public static WorkflowAiCodingPublishResponse fromEntity(WorkflowVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowAiCodingPublishResponse.builder()
                .workflowId(entity.getWorkflowId())
                .versionId(entity.getId())
                .version(entity.getVersion())
                .status(entity.getStatus())
                .rolloutPercent(entity.getRolloutPercent())
                .publishedBy(entity.getPublishedBy())
                .publishedAt(entity.getPublishedAt() == null ? null : entity.getPublishedAt().toString())
                .build();
    }
}
