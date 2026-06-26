package com.enterprise.ai.agent.runtime.host.controller;

import com.enterprise.ai.agent.credential.WorkflowCredentialRequest;
import com.enterprise.ai.agent.credential.WorkflowCredentialResponse;
import com.enterprise.ai.agent.credential.WorkflowCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/workflow-credentials")
@RequiredArgsConstructor
public class WorkflowCredentialController {

    private final WorkflowCredentialService service;

    @GetMapping
    public List<WorkflowCredentialResponse> list(@RequestParam(required = false) Long projectId,
                                                 @RequestParam(required = false) String projectCode) {
        return service.list(projectId, projectCode);
    }

    @PostMapping
    public WorkflowCredentialResponse create(@RequestBody WorkflowCredentialRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WorkflowCredentialResponse update(@PathVariable Long id, @RequestBody WorkflowCredentialRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
