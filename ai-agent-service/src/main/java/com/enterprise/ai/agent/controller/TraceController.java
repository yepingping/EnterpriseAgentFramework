package com.enterprise.ai.agent.controller;

import com.enterprise.ai.agent.tool.log.ToolCallLogEntity;
import com.enterprise.ai.agent.tool.log.ToolCallLogService;
import com.enterprise.ai.agent.trace.AgentTraceSpanEntity;
import com.enterprise.ai.agent.trace.AgentTraceSpanService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traces")
@RequiredArgsConstructor
public class TraceController {

    private static final TypeReference<List<Map<String, Object>>> CANDIDATES_TYPE = new TypeReference<>() {};
    private final ToolCallLogService toolCallLogService;
    private final AgentTraceSpanService traceSpanService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{traceId}")
    public ResponseEntity<TraceDetailResponse> detail(@PathVariable String traceId) {
        List<ToolCallLogEntity> logs = toolCallLogService.getTraceLogs(traceId);
        List<AgentTraceSpanEntity> spans = traceSpanService.listByTraceId(traceId);
        if (logs.isEmpty() && spans.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<TraceNode> nodes = java.util.stream.Stream.concat(
                        logs.stream().map(this::toNode),
                        spans.stream().map(this::toNode))
                .sorted(Comparator
                        .comparing(TraceNode::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(TraceNode::id, Comparator.nullsLast(Long::compareTo)))
                .toList();
        return ResponseEntity.ok(new TraceDetailResponse(traceId, nodes));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ToolCallLogService.TraceSummary>> recent(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(toolCallLogService.listRecentTraces(userId, limit, days));
    }

    private TraceNode toNode(ToolCallLogEntity log) {
        return new TraceNode(
                log.getId(),
                "tool_call_log",
                log.getTraceId(),
                log.getAgentName(),
                log.getToolName(),
                null,
                null,
                null,
                null,
                null,
                log.getArgsJson(),
                log.getResultSummary(),
                Boolean.TRUE.equals(log.getSuccess()),
                log.getErrorCode(),
                log.getElapsedMs(),
                log.getTokenCost(),
                parseRetrieval(log.getRetrievalTraceJson()),
                log.getCreateTime()
        );
    }

    private TraceNode toNode(AgentTraceSpanEntity span) {
        return new TraceNode(
                span.getId(),
                "agent_trace_span",
                span.getTraceId(),
                span.getAgentName(),
                traceSpanName(span),
                span.getSpanType(),
                span.getSpanId(),
                span.getParentSpanId(),
                span.getNodeId(),
                span.getRuntimeType(),
                span.getInputSummary(),
                span.getOutputSummary(),
                "SUCCESS".equalsIgnoreCase(span.getStatus()),
                span.getErrorCode(),
                span.getLatencyMs(),
                span.getTokenCost(),
                List.of(),
                span.getStartedAt() == null ? span.getCreatedAt() : span.getStartedAt()
        );
    }

    private String traceSpanName(AgentTraceSpanEntity span) {
        if (span.getToolName() != null && !span.getToolName().isBlank()) {
            return "span:" + span.getToolName();
        }
        if (span.getNodeId() != null && !span.getNodeId().isBlank()) {
            return "span:" + span.getNodeId();
        }
        return "span:" + span.getSpanType();
    }

    private List<Map<String, Object>> parseRetrieval(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, CANDIDATES_TYPE);
        } catch (Exception ignored) {
            return List.of(Map.of("raw", raw));
        }
    }

    public record TraceDetailResponse(String traceId, List<TraceNode> nodes) {}

    public record TraceNode(
            Long id,
            String source,
            String traceId,
            String agentName,
            String toolName,
            String spanType,
            String spanId,
            String parentSpanId,
            String nodeId,
            String runtimeType,
            String argsJson,
            String resultSummary,
            boolean success,
            String errorCode,
            Integer elapsedMs,
            Integer tokenCost,
            List<Map<String, Object>> retrievalCandidates,
            java.time.LocalDateTime createdAt
    ) {}
}
