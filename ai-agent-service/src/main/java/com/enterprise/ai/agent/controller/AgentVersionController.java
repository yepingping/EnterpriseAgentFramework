package com.enterprise.ai.agent.controller;

import com.enterprise.ai.agent.agent.AgentVersionService;
import com.enterprise.ai.agent.agent.AgentDefinitionService;
import com.enterprise.ai.agent.agent.AgentReleaseEventService;
import com.enterprise.ai.agent.agent.AgentReleaseValidationResult;
import com.enterprise.ai.agent.agent.AgentReleaseValidationService;
import com.enterprise.ai.agent.agent.persist.AgentReleaseEventEntity;
import com.enterprise.ai.agent.agent.persist.AgentVersionEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 版本 & 发布 API（Phase 3.0）
 * <p>
 * 职责：
 * <ul>
 *   <li>发布新版本：把当前 {@code AgentDefinition} 冻结成一个 {@code agent_version} 快照；</li>
 *   <li>回滚：把历史版本重新置 ACTIVE；</li>
 *   <li>列出版本：支撑前端"版本历史"页。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/versions")
@RequiredArgsConstructor
public class AgentVersionController {

    private final AgentVersionService versionService;
    private final AgentDefinitionService definitionService;
    private final AgentReleaseValidationService releaseValidationService;
    private final AgentReleaseEventService releaseEventService;

    @GetMapping
    public ResponseEntity<List<AgentVersionEntity>> list(@PathVariable String agentId) {
        return ResponseEntity.ok(versionService.listVersions(agentId));
    }

    @GetMapping("/events")
    public ResponseEntity<List<AgentReleaseEventEntity>> events(@PathVariable String agentId,
                                                               @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(releaseEventService.listByAgent(agentId, limit));
    }

    @PostMapping("/validate")
    public ResponseEntity<AgentReleaseValidationResult> validate(@PathVariable String agentId,
                                                                @RequestBody(required = false) ValidateRequest request) {
        return definitionService.findById(agentId)
                .map(releaseValidationService::validate)
                .map(result -> {
                    String operator = request == null ? null : request.getOperator();
                    String version = request == null ? null : request.getVersion();
                    Integer rollout = request == null ? null : request.getRolloutPercent();
                    releaseEventService.record(agentId, null, version, "VALIDATE",
                            result.valid() ? "PASSED" : "BLOCKED", rollout, operator,
                            result.valid() ? "发布前校验通过" : "发布前校验未通过", result,
                            versionService.describeReleaseTarget(agentId, null));
                    return result;
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AgentVersionEntity> publish(@PathVariable String agentId,
                                                     @RequestBody PublishRequest request) {
        int rollout = request.getRolloutPercent() == null ? 100 : request.getRolloutPercent();
        AgentVersionEntity entity = versionService.publish(agentId,
                request.getVersion(), rollout, request.getNote(), request.getPublishedBy());
        return ResponseEntity.ok(entity);
    }

    @PostMapping("/{versionId}/rollback")
    public ResponseEntity<AgentVersionEntity> rollback(@PathVariable String agentId,
                                                      @PathVariable Long versionId,
                                                      @RequestBody(required = false) RollbackRequest request) {
        String operator = request == null ? null : request.getOperator();
        AgentVersionEntity entity = versionService.rollback(agentId, versionId, operator);
        return ResponseEntity.ok(entity);
    }

    @Data
    public static class PublishRequest {
        private String version;
        private Integer rolloutPercent;
        private String note;
        private String publishedBy;
    }

    @Data
    public static class ValidateRequest {
        private String version;
        private Integer rolloutPercent;
        private String operator;
    }

    @Data
    public static class RollbackRequest {
        private String operator;
    }
}
