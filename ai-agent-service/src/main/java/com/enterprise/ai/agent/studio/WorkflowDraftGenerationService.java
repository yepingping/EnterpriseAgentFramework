package com.enterprise.ai.agent.studio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowDraftGenerationService {

    private final List<WorkflowDraftGenerator> generators;

    public WorkflowDraftGenerationResult generate(WorkflowDraftGenerationRequest request) {
        String unavailableReason = generators.stream()
                .filter(CursorWorkflowDraftGenerator.class::isInstance)
                .map(CursorWorkflowDraftGenerator.class::cast)
                .map(CursorWorkflowDraftGenerator::unavailableReason)
                .findFirst()
                .orElse("没有可用的流程草稿生成器");
        return generators.stream()
                .filter(generator -> generator.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(unavailableReason))
                .generate(request);
    }
}
