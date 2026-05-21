package com.enterprise.ai.agent.controller;

import com.enterprise.ai.agent.studio.WorkflowDraftGenerationRequest;
import com.enterprise.ai.agent.studio.WorkflowDraftGenerationResult;
import com.enterprise.ai.agent.studio.WorkflowDraftGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/studio")
@RequiredArgsConstructor
public class AgentStudioDraftController {

    private final WorkflowDraftGenerationService generationService;

    @PostMapping("/generate-draft")
    public ResponseEntity<WorkflowDraftGenerationResult> generateDraft(
            @RequestBody WorkflowDraftGenerationRequest request) {
        return ResponseEntity.ok(generationService.generate(request));
    }
}
