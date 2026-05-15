package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.client.ModelServiceClient;
import com.enterprise.ai.agent.tool.log.ToolCallLogService;
import com.enterprise.ai.agent.tool.log.ToolExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
@RequiredArgsConstructor
public class LangGraph4jRuntimeAdapter implements AgentRuntimeAdapter {

    static final String NODE_LLM = "llm";
    static final String INPUT = "input";
    static final String SYSTEM_PROMPT = "systemPrompt";
    static final String MODEL_INSTANCE_ID = "modelInstanceId";
    static final String ANSWER = "answer";
    static final String MODEL = "model";
    static final String PROVIDER = "provider";
    static final String TOKEN_USAGE = "tokenUsage";
    static final String FINISH_REASON = "finishReason";
    static final String CONFIG_LANGGRAPH4J = "langGraph4j";
    static final String CONFIG_NODES = "nodes";
    static final String CONFIG_EDGES = "edges";

    private static final int SUCCESS_CODE = 200;

    private final ModelServiceClient modelServiceClient;
    private final ToolCallLogService toolCallLogService;

    @Override
    public String runtimeType() {
        return LANGGRAPH4J_RUNTIME_TYPE;
    }

    @Override
    public AgentRuntimeCapability capability() {
        return AgentRuntimeCapability.builder()
                .runtimeType(LANGGRAPH4J_RUNTIME_TYPE)
                .displayName("LangGraph4j")
                .description("面向状态图、节点编排和可观测执行流的 Java Agent Runtime。当前最小闭环支持单 Agent LLM 图执行。")
                .available(true)
                .supportedModelType("LLM")
                .supportsStreaming(false)
                .supportsTools(false)
                .supportsHandoff(false)
                .supportsGraph(true)
                .supportsHumanInterrupt(false)
                .supportsArtifacts(false)
                .supportsCodeWorkspace(false)
                .supportsCloudExecution(false)
                .securityLevel("PROJECT")
                .build();
    }

    @Override
    public boolean supports(AgentRuntimeRequest request) {
        AgentDefinition definition = request == null ? null : request.getAgentDefinition();
        if (definition == null) {
            return false;
        }
        if ("pipeline".equalsIgnoreCase(definition.getType())) {
            return false;
        }
        return isEmpty(definition.getTools())
                && isEmpty(definition.getToolRefs())
                && isEmpty(definition.getSkills())
                && isEmpty(definition.getSkillRefs())
                && configuredLlmNode(definition) != null;
    }

    @Override
    public String unsupportedReason(AgentRuntimeRequest request) {
        AgentDefinition definition = request == null ? null : request.getAgentDefinition();
        if (definition == null) {
            return "LangGraph4j 当前仅支持有效的单 Agent LLM 配置";
        }
        if ("pipeline".equalsIgnoreCase(definition.getType())) {
            return "LangGraph4j 当前仅支持单 Agent LLM 执行，请将 Agent 类型改为 single 并清空 Pipeline 子 Agent";
        }
        if (!isEmpty(definition.getTools()) || !isEmpty(definition.getToolRefs())
                || !isEmpty(definition.getSkills()) || !isEmpty(definition.getSkillRefs())) {
            return "LangGraph4j 当前仅支持单 Agent LLM 执行，暂不支持 Tool/能力调用，请清空 Tool 配置和能力配置";
        }
        if (configuredLlmNode(definition) == null) {
            return "LangGraph4j runtimeConfig 必须至少包含一个 type=llm 的节点";
        }
        return "LangGraph4j 当前仅支持单 Agent LLM 执行";
    }

    @Override
    public AgentRuntimeResult execute(AgentRuntimeRequest request) {
        AgentDefinition definition = request.getAgentDefinition();
        String traceId = request.getTraceId();
        long start = System.currentTimeMillis();
        ToolExecutionContext context = buildExecutionContext(request, definition);
        String llmNode = configuredLlmNode(definition);
        try {
            StateGraph<LangGraphState> graph = buildGraph(context, llmNode);
            LangGraphState finalState = graph.compile()
                    .invoke(Map.of(
                            INPUT, nullToEmpty(request.getMessage()),
                            SYSTEM_PROMPT, nullToEmpty(definition.getSystemPrompt()),
                            MODEL_INSTANCE_ID, definition.getModelInstanceId().trim()),
                            RunnableConfig.builder()
                                    .threadId(request.getSessionId())
                                    .graphId("agent:" + definition.getId())
                                    .build())
                    .orElseThrow(() -> new IllegalStateException("LangGraph4j execution returned no final state"));

            String answer = finalState.value(ANSWER, "");
            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> metadata = metadata(request, definition, elapsed, finalState);
            logRuntimeRun(context, request, definition, metadata, true, null, elapsed);
            return AgentRuntimeResult.builder()
                    .success(true)
                    .answer(answer)
                    .runtimeType(runtimeType())
                    .traceId(traceId)
                    .agentName(definition.getName())
                    .steps(List.of(
                            "Runtime: " + runtimeType(),
                            "LangGraph4j 节点: " + llmNode,
                            "Agent执行: " + definition.getName()))
                    .tokenUsage(asMap(finalState.value(TOKEN_USAGE).orElse(null)))
                    .metadata(metadata)
                    .build();
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[LangGraph4jRuntime] 执行失败: agent={}, traceId={}", definition.getName(), traceId, ex);
            logRuntimeRun(context, request, definition, Map.of("error", ex.getMessage()), false, ex, elapsed);
            return AgentRuntimeResult.builder()
                    .success(false)
                    .answer("LangGraph4j Runtime 执行失败：" + ex.getMessage())
                    .runtimeType(runtimeType())
                    .traceId(traceId)
                    .agentName(definition.getName())
                    .errorCode(ex.getClass().getSimpleName())
                    .errorMessage(ex.getMessage())
                    .metadata(Map.of(
                            "traceId", traceId,
                            "agentName", definition.getName(),
                            "runtimeType", runtimeType(),
                            "elapsedMs", elapsed))
                    .build();
        }
    }

    private StateGraph<LangGraphState> buildGraph(ToolExecutionContext context, String llmNode) throws Exception {
        return new StateGraph<>(LangGraphState::new)
                .addNode(llmNode, node_async(state -> callModelNode(state, context, llmNode)))
                .addEdge(START, llmNode)
                .addEdge(llmNode, END);
    }

    private Map<String, Object> callModelNode(LangGraphState state, ToolExecutionContext context, String nodeId) {
        long start = System.currentTimeMillis();
        String modelInstanceId = state.value(MODEL_INSTANCE_ID, "");
        String systemPrompt = state.value(SYSTEM_PROMPT, "");
        String userMessage = state.value(INPUT, "");
        try {
            ModelServiceClient.ModelChatResult result = modelServiceClient.chat(ModelServiceClient.ModelChatRequest.builder()
                    .modelInstanceId(modelInstanceId)
                    .messages(List.of(
                            ModelServiceClient.ModelChatRequest.ChatMessage.builder()
                                    .role("system")
                                    .content(systemPrompt)
                                    .build(),
                            ModelServiceClient.ModelChatRequest.ChatMessage.builder()
                                    .role("user")
                                    .content(userMessage)
                                    .build()))
                    .build());
            if (result == null || result.getCode() != SUCCESS_CODE || result.getData() == null) {
                String message = result == null ? "empty response" : result.getMessage();
                throw new IllegalStateException("模型调用失败: " + message);
            }

            ModelServiceClient.ModelChatData data = result.getData();
            Map<String, Object> update = new HashMap<>();
            update.put(ANSWER, nullToEmpty(data.getContent()));
            update.put(MODEL, data.getModel());
            update.put(PROVIDER, data.getProvider());
            update.put(TOKEN_USAGE, tokenUsage(data.getUsage()));
            update.put(FINISH_REASON, data.getFinishReason());
            logNode(context, nodeId, userMessage, update, true, null, System.currentTimeMillis() - start);
            return update;
        } catch (Exception ex) {
            logNode(context, nodeId, userMessage, Map.of("error", ex.getMessage()), false, ex, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private Map<String, Object> metadata(AgentRuntimeRequest request,
                                         AgentDefinition definition,
                                         long elapsed,
                                         LangGraphState state) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("traceId", request.getTraceId());
        metadata.put("runtimeType", runtimeType());
        metadata.put("agentName", definition.getName());
        metadata.put("intentType", request.getIntentType());
        metadata.put("graphNodes", configuredGraphNodes(definition));
        metadata.put("graphEdges", configuredGraphEdges(definition));
        metadata.put("elapsedMs", elapsed);
        metadata.put("model", state.value(MODEL).orElse(null));
        metadata.put("provider", state.value(PROVIDER).orElse(null));
        metadata.put("finishReason", state.value(FINISH_REASON).orElse(null));
        return metadata;
    }

    private ToolExecutionContext buildExecutionContext(AgentRuntimeRequest request, AgentDefinition definition) {
        return ToolExecutionContext.builder()
                .traceId(request.getTraceId())
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .agentName(definition.getName())
                .intentType(request.getIntentType())
                .projectCode(definition.getProjectCode())
                .allowIrreversible(definition.isAllowIrreversible())
                .roles(request.getRoles())
                .currentTurnMessage(request.getMessage())
                .build();
    }

    private void logNode(ToolExecutionContext context,
                         String nodeId,
                         String userMessage,
                         Object result,
                         boolean success,
                         Exception error,
                         long elapsedMs) {
        if (toolCallLogService == null) {
            return;
        }
        toolCallLogService.record(context,
                "runtime.langgraph4j.node." + nodeId,
                Map.of("runtimeType", runtimeType(), "node", nodeId, "userInput", userMessage),
                result,
                success,
                error == null ? null : error.getClass().getSimpleName(),
                elapsedMs,
                tokenCostFrom(result));
    }

    private void logRuntimeRun(ToolExecutionContext context,
                               AgentRuntimeRequest request,
                               AgentDefinition definition,
                               Object result,
                               boolean success,
                               Exception error,
                               long elapsedMs) {
        if (toolCallLogService == null) {
            return;
        }
        toolCallLogService.record(context,
                "runtime.agent.run",
                Map.of(
                        "runtimeType", runtimeType(),
                        "graph", "single-agent-llm",
                        "agentName", definition.getName(),
                        "userInput", nullToEmpty(request.getMessage())),
                result,
                success,
                error == null ? null : error.getClass().getSimpleName(),
                elapsedMs,
                null);
    }

    private static Map<String, Object> tokenUsage(ModelServiceClient.ModelUsage usage) {
        if (usage == null) {
            return Map.of();
        }
        return Map.of(
                "promptTokens", usage.getPromptTokens(),
                "completionTokens", usage.getCompletionTokens(),
                "totalTokens", usage.getTotalTokens());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Integer tokenCostFrom(Object result) {
        Map<String, Object> map = asMap(result);
        Object usage = map.get(TOKEN_USAGE);
        if (usage instanceof Map<?, ?> usageMap) {
            Object total = usageMap.get("totalTokens");
            if (total instanceof Number number) {
                return number.intValue();
            }
        }
        return null;
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private static String configuredLlmNode(AgentDefinition definition) {
        for (Map<String, Object> node : configuredNodeMaps(definition)) {
            String id = asString(node.get("id"));
            String type = asString(node.get("type"));
            if (!id.isBlank() && ("llm".equalsIgnoreCase(type) || NODE_LLM.equalsIgnoreCase(id))) {
                return id;
            }
        }
        return configuredNodeMaps(definition).isEmpty() ? NODE_LLM : null;
    }

    private static List<String> configuredGraphNodes(AgentDefinition definition) {
        List<String> nodes = configuredNodeMaps(definition).stream()
                .map(node -> asString(node.get("id")))
                .filter(id -> !id.isBlank())
                .toList();
        return nodes.isEmpty() ? List.of(NODE_LLM) : nodes;
    }

    private static Object configuredGraphEdges(AgentDefinition definition) {
        Object edges = langGraphConfig(definition).get(CONFIG_EDGES);
        return edges == null ? List.of(Map.of("from", START, "to", NODE_LLM), Map.of("from", NODE_LLM, "to", END)) : edges;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> configuredNodeMaps(AgentDefinition definition) {
        Object nodes = langGraphConfig(definition).get(CONFIG_NODES);
        if (!(nodes instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(node -> (Map<String, Object>) node)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> langGraphConfig(AgentDefinition definition) {
        Map<String, Object> runtimeConfig = definition == null || definition.getRuntimeConfig() == null
                ? Map.of()
                : definition.getRuntimeConfig();
        Object nested = runtimeConfig.get(CONFIG_LANGGRAPH4J);
        if (nested instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return runtimeConfig;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static class LangGraphState extends AgentState {
        public LangGraphState(Map<String, Object> initData) {
            super(initData);
        }
    }
}
