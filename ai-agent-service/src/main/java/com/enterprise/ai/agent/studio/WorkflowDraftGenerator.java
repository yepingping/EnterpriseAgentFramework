package com.enterprise.ai.agent.studio;

public interface WorkflowDraftGenerator {

    String provider();

    boolean supports(WorkflowDraftGenerationRequest request);

    WorkflowDraftGenerationResult generate(WorkflowDraftGenerationRequest request);
}
