package com.enterprise.ai.agent.runtime.host.controller;

import com.enterprise.ai.agent.runops.RunOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/runops")
@RequiredArgsConstructor
public class RunOpsController {

    private final RunOpsService runOpsService;

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<?> detail(@PathVariable String traceId) {
        try {
            return ResponseEntity.ok(runOpsService.detail(traceId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/traces/recent")
    public ResponseEntity<List<RunOpsService.RunSummary>> recent(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(runOpsService.recent(userId, limit, days));
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<RunOpsService.RunDiagnostics> diagnostics(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(runOpsService.diagnostics(userId, limit, days));
    }

    @PostMapping("/traces/{traceId}/replay")
    public ResponseEntity<?> replay(@PathVariable String traceId,
                                    @RequestBody(required = false) RunOpsService.ReplayRequest request) {
        try {
            return ResponseEntity.ok(runOpsService.replay(traceId, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/traces/{traceId}/compare/{candidateTraceId}")
    public ResponseEntity<?> compare(@PathVariable String traceId,
                                     @PathVariable String candidateTraceId) {
        try {
            return ResponseEntity.ok(runOpsService.compare(traceId, candidateTraceId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    public record ErrorResponse(String message) {}
}
