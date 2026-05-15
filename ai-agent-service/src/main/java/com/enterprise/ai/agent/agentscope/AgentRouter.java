package com.enterprise.ai.agent.agentscope;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.agent.AgentDefinitionService;
import com.enterprise.ai.agent.model.AgentResult;
import com.enterprise.ai.agent.runtime.AgentRuntimeAdapter;
import com.enterprise.ai.agent.runtime.AgentRuntimeRequest;
import com.enterprise.ai.agent.runtime.AgentRuntimeResult;
import com.enterprise.ai.agent.runtime.AgentRuntimeSelector;
import com.enterprise.ai.agent.service.IntentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 路由门面。
 * <p>
 * Controller / A2A / Service 仍复用这个入口；具体执行已下沉到
 * {@link AgentRuntimeAdapter}，避免平台主链路继续直接绑定 AgentScope 类型。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRouter {

    private static final String GENERAL_CHAT = "GENERAL_CHAT";

    private final IntentService intentService;
    private final AgentDefinitionService agentDefinitionService;
    private final AgentRuntimeSelector runtimeSelector;

    public AgentResult executeByDefinition(AgentDefinition definition, String sessionId,
                                           String userId, String message) {
        return executeByDefinition(definition, sessionId, userId, message, null);
    }

    public AgentResult executeByDefinition(AgentDefinition definition, String sessionId,
                                           String userId, String message, List<String> roles) {
        String traceId = UUID.randomUUID().toString();
        String intentType = definition.getIntentType();
        log.info("[AgentRouter] 按定义执行: agent={}, keySlug={}, runtime={}, sessionId={}, roles={}, traceId={}",
                definition.getName(), definition.getKeySlug(), runtimeTypeOf(definition), sessionId, roles, traceId);

        return executeRuntime(AgentRuntimeRequest.builder()
                .traceId(traceId)
                .sessionId(sessionId)
                .userId(userId)
                .roles(roles == null ? List.of() : roles)
                .message(message)
                .intentType(intentType)
                .agentDefinition(definition)
                .build(), intentType);
    }

    public AgentResult route(String sessionId, String userId, String message, String intentHint) {
        return route(sessionId, userId, message, intentHint, null);
    }

    public AgentResult route(String sessionId, String userId, String message, String intentHint,
                             List<String> roles) {
        String intentType = resolveIntent(message, intentHint);
        String traceId = UUID.randomUUID().toString();
        AgentDefinition definition = agentDefinitionService.findByIntentType(intentType)
                .orElseThrow(() -> new IllegalStateException(
                        "未找到意图 '" + intentType + "' 对应的 Agent 定义"));

        log.info("[AgentRouter] 路由决策: intent={}, runtime={}, sessionId={}, userId={}, roles={}, traceId={}",
                intentType, runtimeTypeOf(definition), sessionId, userId, roles, traceId);

        return executeRuntime(AgentRuntimeRequest.builder()
                .traceId(traceId)
                .sessionId(sessionId)
                .userId(userId)
                .roles(roles == null ? List.of() : roles)
                .message(message)
                .intentType(intentType)
                .agentDefinition(definition)
                .build(), intentType);
    }

    private AgentResult executeRuntime(AgentRuntimeRequest request, String intentType) {
        try {
            AgentRuntimeAdapter adapter = runtimeSelector.select(request);
            AgentRuntimeResult result = adapter.execute(request);
            return toAgentResult(result, intentType);
        } catch (Exception e) {
            log.error("[AgentRouter] Agent 执行失败: intent={}, traceId={}", intentType, request.getTraceId(), e);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("intentType", intentType);
            metadata.put("traceId", request.getTraceId());
            metadata.put("runtimeType", runtimeTypeOf(request.getAgentDefinition()));
            return AgentResult.builder()
                    .success(false)
                    .answer("处理过程中遇到异常：" + e.getMessage())
                    .metadata(metadata)
                    .build();
        }
    }

    private AgentResult toAgentResult(AgentRuntimeResult runtimeResult, String intentType) {
        Map<String, Object> metadata = new HashMap<>();
        if (runtimeResult.getMetadata() != null) {
            metadata.putAll(runtimeResult.getMetadata());
        }
        metadata.putIfAbsent("intentType", intentType);
        metadata.putIfAbsent("traceId", runtimeResult.getTraceId());
        metadata.putIfAbsent("runtimeType", runtimeResult.getRuntimeType());
        if (runtimeResult.getAgentName() != null) {
            metadata.putIfAbsent("agentName", runtimeResult.getAgentName());
        }
        if (runtimeResult.getToolCalls() != null && !runtimeResult.getToolCalls().isEmpty()) {
            metadata.putIfAbsent("toolCalls", runtimeResult.getToolCalls());
        }
        if (runtimeResult.getSteps() != null && !runtimeResult.getSteps().isEmpty()) {
            metadata.putIfAbsent("steps", runtimeResult.getSteps());
        }

        return AgentResult.builder()
                .success(runtimeResult.isSuccess())
                .answer(runtimeResult.getAnswer())
                .steps(toStepRecords(runtimeResult.getSteps()))
                .metadata(metadata)
                .uiRequest(runtimeResult.getUiRequest())
                .build();
    }

    private List<AgentResult.StepRecord> toStepRecords(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        List<AgentResult.StepRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            records.add(new AgentResult.StepRecord("Step " + (i + 1), steps.get(i)));
        }
        return records;
    }

    private String resolveIntent(String message, String intentHint) {
        String raw;
        if (intentHint != null && !intentHint.isBlank()) {
            raw = intentHint;
        } else {
            raw = intentService.recognizeIntent(message);
        }

        if (agentDefinitionService.findByIntentType(raw).isEmpty()) {
            log.warn("[AgentRouter] 意图 '{}' 无对应 Agent 定义或已禁用，降级到 {}", raw, GENERAL_CHAT);
            return GENERAL_CHAT;
        }
        return raw;
    }

    private String runtimeTypeOf(AgentDefinition definition) {
        if (definition != null && definition.getRuntimeType() != null && !definition.getRuntimeType().isBlank()) {
            return definition.getRuntimeType();
        }
        return AgentRuntimeAdapter.DEFAULT_RUNTIME_TYPE;
    }
}
