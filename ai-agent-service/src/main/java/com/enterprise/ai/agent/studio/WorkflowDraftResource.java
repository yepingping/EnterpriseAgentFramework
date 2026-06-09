package com.enterprise.ai.agent.studio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDraftResource {

    private String kind;

    private String name;

    private String qualifiedName;

    private Long definitionId;

    private String projectCode;

    private String description;

    private java.util.Map<String, Object> metadata;
}
