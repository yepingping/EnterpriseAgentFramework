package com.enterprise.ai.agent.studio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowDraftGenerationServiceTest {

    @Test
    void generateReportsClearReasonWhenCursorProviderDisabled() {
        WorkflowDraftProperties properties = new WorkflowDraftProperties();
        properties.setCursorEnabled(false);
        properties.setCursorUnavailableReason("Cursor SDK 未配置 workspace");
        WorkflowDraftGenerationService service = new WorkflowDraftGenerationService(List.of(
                new CursorWorkflowDraftGenerator(properties)));

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                service.generate(WorkflowDraftGenerationRequest.builder()
                        .requirement("查询订单")
                        .build()));

        assertTrue(error.getMessage().contains("Cursor SDK 未配置 workspace"));
    }
}
