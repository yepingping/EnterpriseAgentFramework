package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.client.ModelServiceClient;
import com.enterprise.ai.agent.graph.AgentGraphSpec;
import com.enterprise.ai.agent.skill.ToolExecutionContextHolder;
import com.enterprise.ai.agent.tool.log.ToolCallLogService;
import com.enterprise.ai.agent.tool.log.ToolExecutionContext;
import com.enterprise.ai.agent.tools.definition.ToolDefinitionService;
import com.enterprise.ai.agent.trace.AgentTraceSpanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
@RequiredArgsConstructor
public class LangGraph4jRuntimeAdapter implements AgentRuntimeAdapter {

    static final String NODE_LLM = "llm";
    static final String NODE_TOOL = "tool";
    static final String NODE_CAPABILITY = "capability";
    static final String INPUT = "input";
    static final String SYSTEM_PROMPT = "systemPrompt";
    static final String MODEL_INSTANCE_ID = "modelInstanceId";
    static final String ANSWER = "answer";
    static final String MODEL = "model";
    static final String PROVIDER = "provider";
    static final String TOKEN_USAGE = "tokenUsage";
    static final String FINISH_REASON = "finishReason";
    static final String LAST_OUTPUT = "lastOutput";
    static final String LAST_SUCCESS = "lastSuccess";
    static final String LAST_ERROR = "lastError";
    static final String CONFIG_ARGS = "args";
    static final String CONFIG_TOOL_ARGS = "toolArgs";
    static final String CONFIG_TOOL_NAME = "toolName";
    static final String CONFIG_QUALIFIED_NAME = "qualifiedName";
    static final String CONFIG_INPUT_MAPPING = "inputMapping";
    static final String CONFIG_OUTPUT_ALIAS = "outputAlias";
    static final String VAR_PREFIX = "var.";
    static final String SPAN_ROOT = "agent-run";

    private static final int SUCCESS_CODE = 200;

    private final ModelServiceClient modelServiceClient;
    private final ToolDefinitionService toolDefinitionService;
    private final ToolCallLogService toolCallLogService;
    private final AgentTraceSpanService traceSpanService;

    @Override
    public String runtimeType() {
        return LANGGRAPH4J_RUNTIME_TYPE;
    }

    @Override
    public AgentRuntimeCapability capability() {
        return AgentRuntimeCapability.builder()
                .runtimeType(LANGGRAPH4J_RUNTIME_TYPE)
                .displayName("LangGraph4j")
                .description("Java graph runtime that compiles platform GraphSpec into observable LLM, Tool and conditional edge execution.")
                .available(true)
                .supportedModelType("LLM")
                .supportsStreaming(false)
                .supportsTools(true)
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
        if (definition == null || "pipeline".equalsIgnoreCase(definition.getType())) {
            return false;
        }
        List<AgentGraphSpec.Node> nodes = orderedExecutableNodes(definition);
        return nodes.stream().anyMatch(this::isLlmNode) && nodes.stream().allMatch(this::isSupportedNode);
    }

    @Override
    public String unsupportedReason(AgentRuntimeRequest request) {
        AgentDefinition definition = request == null ? null : request.getAgentDefinition();
        if (definition == null) {
            return "LangGraph4j requires a valid Agent definition.";
        }
        if ("pipeline".equalsIgnoreCase(definition.getType())) {
            return "LangGraph4j currently executes a single GraphSpec workflow, not legacy pipeline sub-agents.";
        }
        AgentGraphSpec spec = graphSpec(definition);
        if (spec == null || spec.getNodes() == null || spec.getNodes().isEmpty()) {
            return "LangGraph4j requires graphSpec.nodes.";
        }
        if (orderedExecutableNodes(definition).stream().noneMatch(this::isLlmNode)) {
            return "LangGraph4j graphSpec must contain at least one LLM node.";
        }
        return "LangGraph4j currently supports LLM, TOOL, CAPABILITY nodes and simple conditional edges.";
    }

    @Override
    public AgentRuntimeResult execute(AgentRuntimeRequest request) {
        AgentDefinition definition = request.getAgentDefinition();
        String traceId = request.getTraceId();
        long start = System.currentTimeMillis();
        LocalDateTime startedAt = LocalDateTime.now();
        ToolExecutionContext context = buildExecutionContext(request, definition);
        List<AgentGraphSpec.Node> nodes = orderedExecutableNodes(definition);
        Map<String, Object> initialState = initialState(request, definition, nodes);
        try {
            StateGraph<LangGraphState> graph = buildGraph(context, definition, nodes);
            LangGraphState finalState = graph.compile()
                    .invoke(initialState,
                            RunnableConfig.builder()
                                    .threadId(request.getSessionId())
                                    .graphId("agent:" + definition.getId())
                                    .build())
                    .orElseThrow(() -> new IllegalStateException("LangGraph4j execution returned no final state"));

            String answer = finalAnswer(finalState);
            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> metadata = metadata(request, definition, elapsed, finalState, nodes);
            logRuntimeRun(context, request, definition, metadata, true, null, elapsed);
            recordSpan(context, definition, AgentTraceSpanService.SpanRecord.builder()
                    .traceId(traceId)
                    .spanId(SPAN_ROOT)
                    .spanType("AGENT_RUN")
                    .runtimeType(runtimeType())
                    .input(request.getMessage())
                    .output(answer)
                    .success(true)
                    .latencyMs(elapsed)
                    .startedAt(startedAt)
                    .endedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .build());
            return AgentRuntimeResult.builder()
                    .success(true)
                    .answer(answer)
                    .runtimeType(runtimeType())
                    .traceId(traceId)
                    .agentName(definition.getName())
                    .steps(steps(nodes))
                    .toolCalls(toolCalls(nodes))
                    .tokenUsage(asMap(finalState.value(TOKEN_USAGE).orElse(null)))
                    .metadata(metadata)
                    .build();
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[LangGraph4jRuntime] execution failed: agent={}, traceId={}", definition.getName(), traceId, ex);
            logRuntimeRun(context, request, definition, Map.of("error", ex.getMessage()), false, ex, elapsed);
            recordSpan(context, definition, AgentTraceSpanService.SpanRecord.builder()
                    .traceId(traceId)
                    .spanId(SPAN_ROOT)
                    .spanType("AGENT_RUN")
                    .runtimeType(runtimeType())
                    .input(request.getMessage())
                    .output(Map.of("error", nullToEmpty(ex.getMessage())))
                    .success(false)
                    .errorCode(ex.getClass().getSimpleName())
                    .errorMessage(ex.getMessage())
                    .latencyMs(elapsed)
                    .startedAt(startedAt)
                    .endedAt(LocalDateTime.now())
                    .metadata(Map.of("agentName", definition.getName(), "runtimeType", runtimeType()))
                    .build());
            return AgentRuntimeResult.builder()
                    .success(false)
                    .answer("LangGraph4j Runtime execution failed: " + ex.getMessage())
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

    private StateGraph<LangGraphState> buildGraph(ToolExecutionContext context,
                                                  AgentDefinition definition,
                                                  List<AgentGraphSpec.Node> nodes) throws Exception {
        StateGraph<LangGraphState> graph = new StateGraph<>(LangGraphState::new);
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("LangGraph4j GraphSpec has no executable nodes");
        }
        AgentGraphSpec spec = graphSpec(definition);
        Map<String, AgentGraphSpec.Node> nodeById = new LinkedHashMap<>();
        for (AgentGraphSpec.Node node : nodes) {
            nodeById.put(node.getId(), node);
        }
        for (AgentGraphSpec.Node graphNode : nodes) {
            AgentGraphSpec.Node node = graphNode;
            boolean allowErrorRoute = hasErrorRoute(outgoingRoutes(spec, node.getId(), nodeById));
            if (isLlmNode(node)) {
                graph = graph.addNode(node.getId(), node_async(state -> callModelNode(state, context, definition, node, allowErrorRoute)));
            } else if (isToolNode(node)) {
                graph = graph.addNode(node.getId(), node_async(state -> callToolNode(state, context, definition, node, allowErrorRoute)));
            }
        }
        String entry = resolveEntry(spec, nodeById);
        graph = graph.addEdge(START, entry);
        for (AgentGraphSpec.Node node : nodes) {
            List<GraphEdgeRoute> outgoing = outgoingRoutes(spec, node.getId(), nodeById);
            if (outgoing.isEmpty()) {
                graph = graph.addEdge(node.getId(), END);
            } else if (outgoing.size() == 1 && isAlwaysCondition(outgoing.get(0).condition())) {
                graph = graph.addEdge(node.getId(), outgoing.get(0).target());
            } else {
                Map<String, String> mappings = new LinkedHashMap<>();
                for (GraphEdgeRoute route : outgoing) {
                    mappings.put(route.key(), route.target());
                }
                List<GraphEdgeRoute> routes = List.copyOf(outgoing);
                graph = graph.addConditionalEdges(node.getId(), edge_async(state -> selectRouteKey(state, routes)), mappings);
            }
        }
        return graph;
    }

    private Map<String, Object> callModelNode(LangGraphState state,
                                              ToolExecutionContext context,
                                              AgentDefinition definition,
                                              AgentGraphSpec.Node node,
                                              boolean allowErrorRoute) {
        long start = System.currentTimeMillis();
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> config = safeMap(node.getConfig());
        String modelInstanceId = firstNonBlank(asString(config.get(MODEL_INSTANCE_ID)), state.value(MODEL_INSTANCE_ID, ""));
        String systemPrompt = firstNonBlank(asString(config.get(SYSTEM_PROMPT)), state.value(SYSTEM_PROMPT, ""));
        String userMessage = renderModelInput(state);
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
                throw new IllegalStateException("model call failed: " + message);
            }

            ModelServiceClient.ModelChatData data = result.getData();
            Map<String, Object> update = new LinkedHashMap<>();
            update.put(ANSWER, nullToEmpty(data.getContent()));
            update.put(LAST_OUTPUT, nullToEmpty(data.getContent()));
            update.put(LAST_SUCCESS, true);
            update.put(LAST_ERROR, "");
            update.put(nodeOutputKey(node.getId()), nullToEmpty(data.getContent()));
            putOutputAlias(update, node, nullToEmpty(data.getContent()));
            update.put(MODEL, data.getModel());
            update.put(PROVIDER, data.getProvider());
            update.put(TOKEN_USAGE, tokenUsage(data.getUsage()));
            update.put(FINISH_REASON, data.getFinishReason());
            long elapsed = System.currentTimeMillis() - start;
            logNode(context, node, userMessage, update, true, null, elapsed);
            recordSpan(context, definition, AgentTraceSpanService.SpanRecord.builder()
                    .spanType("LLM_CALL")
                    .runtimeType(runtimeType())
                    .parentSpanId(SPAN_ROOT)
                    .nodeId(node.getId())
                    .modelInstanceId(modelInstanceId)
                    .input(userMessage)
                    .output(update)
                    .success(true)
                    .latencyMs(elapsed)
                    .tokenCost(tokenCostFrom(update))
                    .startedAt(startedAt)
                    .endedAt(LocalDateTime.now())
                    .metadata(nodeMetadata(node))
                    .build());
            return update;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> error = Map.of("error", nullToEmpty(ex.getMessage()));
            Map<String, Object> update = new LinkedHashMap<>(error);
            update.put(LAST_OUTPUT, nullToEmpty(ex.getMessage()));
            update.put(LAST_SUCCESS, false);
            update.put(LAST_ERROR, nullToEmpty(ex.getMessage()));
            putOutputAlias(update, node, nullToEmpty(ex.getMessage()));
            logNode(context, node, userMessage, update, false, ex, elapsed);
            recordSpan(context, definition, AgentTraceSpanService.SpanRecord.builder()
                    .spanType("LLM_CALL")
                    .runtimeType(runtimeType())
                    .parentSpanId(SPAN_ROOT)
                    .nodeId(node.getId())
                    .modelInstanceId(modelInstanceId)
                    .input(userMessage)
                    .output(update)
                    .success(false)
                    .errorCode(ex.getClass().getSimpleName())
                    .errorMessage(ex.getMessage())
                    .latencyMs(elapsed)
                    .startedAt(startedAt)
                    .endedAt(LocalDateTime.now())
                    .metadata(nodeMetadata(node))
                    .build());
            if (allowErrorRoute) {
                return update;
            }
            throw ex;
        }
    }

    private Map<String, Object> callToolNode(LangGraphState state,
                                             ToolExecutionContext context,
                                             AgentDefinition definition,
                                             AgentGraphSpec.Node node,
                                             boolean allowErrorRoute) {
        long start = System.currentTimeMillis();
        LocalDateTime startedAt = LocalDateTime.now();
        String toolName = resolveExecutableToolName(node);
        Map<String, Object> args = resolveToolArgs(state, node);
        ToolExecutionContext prev = ToolExecutionContextHolder.get();
        try {
            ToolExecutionContextHolder.set(context);
            Object result = toolDefinitionService.executeTool(toolName, args);
            Map<String, Object> update = new LinkedHashMap<>();
            update.put(LAST_OUTPUT, result);
            update.put(LAST_SUCCESS, true);
            update.put(LAST_ERROR, "");
            update.put(nodeOutputKey(node.getId()), result);
            putOutputAlias(update, node, result);
            long elapsed = System.currentTimeMillis() - start;
            logNode(context, node, args, result, true, null, elapsed);
            recordSpan(context, definition, AgentTraceSpanService.SpanRecord.builder()
                    .spanType("TOOL_CALL")
                    .runtimeType(runtimeType())
                    .parentSpanId(SPAN_ROOT)
                    .nodeId(node.getId())
                    .toolName(toolName)
                    .input(args)
                    .output(result)
                    .success(true)
                    .latencyMs(elapsed)
                    .startedAt(startedAt)
                    .endedAt(LocalDateTime.now())
                    .metadata(nodeMetadata(node))
                    .build());
            return update;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> error = Map.of("error", nullToEmpty(ex.getMessage()));
            Map<String, Object> update = new LinkedHashMap<>(error);
            update.put(LAST_OUTPUT, nullToEmpty(ex.getMessage()));
            update.put(LAST_SUCCESS, false);
            update.put(LAST_ERROR, nullToEmpty(ex.getMessage()));
            putOutputAlias(update, node, nullToEmpty(ex.getMessage()));
            logNode(context, node, args, update, false, ex, elapsed);
            recordSpan(context, definition, AgentTraceSpanService.SpanRecord.builder()
                    .spanType("TOOL_CALL")
                    .runtimeType(runtimeType())
                    .parentSpanId(SPAN_ROOT)
                    .nodeId(node.getId())
                    .toolName(toolName)
                    .input(args)
                    .output(update)
                    .success(false)
                    .errorCode(ex.getClass().getSimpleName())
                    .errorMessage(ex.getMessage())
                    .latencyMs(elapsed)
                    .startedAt(startedAt)
                    .endedAt(LocalDateTime.now())
                    .metadata(nodeMetadata(node))
                    .build());
            if (allowErrorRoute) {
                return update;
            }
            throw ex;
        } finally {
            ToolExecutionContextHolder.set(prev);
        }
    }

    private Map<String, Object> metadata(AgentRuntimeRequest request,
                                         AgentDefinition definition,
                                         long elapsed,
                                         LangGraphState state,
                                         List<AgentGraphSpec.Node> nodes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.getMetadata() != null) {
            metadata.putAll(request.getMetadata());
        }
        metadata.put("traceId", request.getTraceId());
        metadata.put("runtimeType", runtimeType());
        metadata.put("runtimePlacement", definition.getRuntimePlacement());
        metadata.put("agentName", definition.getName());
        if (definition.getExtra() != null) {
            metadata.putIfAbsent("version", definition.getExtra().get("__version"));
            metadata.putIfAbsent("versionId", definition.getExtra().get("__versionId"));
        }
        metadata.put("intentType", request.getIntentType());
        metadata.put("graphCode", graphSpec(definition) == null ? null : graphSpec(definition).getCode());
        metadata.put("graphNodes", configuredGraphNodes(nodes));
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

    private Map<String, Object> initialState(AgentRuntimeRequest request,
                                             AgentDefinition definition,
                                             List<AgentGraphSpec.Node> nodes) {
        AgentGraphSpec.Node firstLlm = nodes.stream().filter(this::isLlmNode).findFirst().orElse(null);
        Map<String, Object> config = firstLlm == null ? Map.of() : safeMap(firstLlm.getConfig());
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(INPUT, nullToEmpty(request.getMessage()));
        state.put(SYSTEM_PROMPT, firstNonBlank(asString(config.get(SYSTEM_PROMPT)), nullToEmpty(definition.getSystemPrompt())));
        state.put(MODEL_INSTANCE_ID, firstNonBlank(asString(config.get(MODEL_INSTANCE_ID)), nullToEmpty(definition.getModelInstanceId())));
        return state;
    }

    private void logNode(ToolExecutionContext context,
                         AgentGraphSpec.Node node,
                         Object input,
                         Object result,
                         boolean success,
                         Exception error,
                         long elapsedMs) {
        if (toolCallLogService == null) {
            return;
        }
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("runtimeType", runtimeType());
        args.put("node", node.getId());
        args.put("nodeType", node.getType());
        args.put("input", input);
        toolCallLogService.record(context,
                "runtime.langgraph4j.node." + node.getId(),
                args,
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
                        "graph", graphSpec(definition) == null ? "" : nullToEmpty(graphSpec(definition).getCode()),
                        "agentName", definition.getName(),
                        "userInput", nullToEmpty(request.getMessage())),
                result,
                success,
                error == null ? null : error.getClass().getSimpleName(),
                elapsedMs,
                null);
    }

    private void recordSpan(ToolExecutionContext context,
                            AgentDefinition definition,
                            AgentTraceSpanService.SpanRecord record) {
        if (traceSpanService != null) {
            traceSpanService.record(context, definition, record);
        }
    }

    private List<AgentGraphSpec.Node> orderedExecutableNodes(AgentDefinition definition) {
        AgentGraphSpec spec = graphSpec(definition);
        if (spec == null || spec.getNodes() == null) {
            return List.of();
        }
        Map<String, AgentGraphSpec.Node> byId = new LinkedHashMap<>();
        for (AgentGraphSpec.Node node : spec.getNodes()) {
            if (node != null && !asString(node.getId()).isBlank() && isSupportedNode(node)) {
                byId.put(node.getId(), node);
            }
        }
        if (byId.isEmpty()) {
            return List.of();
        }
        List<AgentGraphSpec.Node> walked = walkReachableGraph(spec, byId);
        if (!walked.isEmpty()) {
            return walked;
        }
        return byId.values().stream().toList();
    }

    private List<AgentGraphSpec.Node> walkReachableGraph(AgentGraphSpec spec, Map<String, AgentGraphSpec.Node> byId) {
        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        if (spec.getEdges() != null) {
            for (AgentGraphSpec.Edge edge : spec.getEdges()) {
                String from = asString(edge.getFrom());
                String to = asString(edge.getTo());
                if (!from.isBlank() && !to.isBlank()) {
                    outgoing.computeIfAbsent(from, key -> new ArrayList<>()).add(to);
                }
            }
        }
        List<AgentGraphSpec.Node> ordered = new ArrayList<>();
        List<String> queue = new ArrayList<>();
        queue.add(resolveEntry(spec, byId));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < queue.size(); i++) {
            String current = queue.get(i);
            if (current == null || !byId.containsKey(current) || !seen.add(current)) {
                continue;
            }
            ordered.add(byId.get(current));
            List<String> targets = outgoing.getOrDefault(current, List.of());
            for (String target : targets) {
                if (byId.containsKey(target) && !seen.contains(target)) {
                    queue.add(target);
                }
            }
        }
        return ordered;
    }

    private String resolveEntry(AgentGraphSpec spec, Map<String, AgentGraphSpec.Node> byId) {
        String entry = firstNonBlank(asString(spec == null ? null : spec.getEntry()), null);
        if (entry != null && byId.containsKey(entry)) {
            return entry;
        }
        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        if (spec != null && spec.getEdges() != null) {
            for (AgentGraphSpec.Edge edge : spec.getEdges()) {
                outgoing.computeIfAbsent(asString(edge.getFrom()), key -> new ArrayList<>()).add(asString(edge.getTo()));
            }
        }
        String startTarget = firstExecutableTarget(outgoing.get(START), byId);
        if (startTarget != null) {
            return startTarget;
        }
        return byId.keySet().iterator().next();
    }

    private List<GraphEdgeRoute> outgoingRoutes(AgentGraphSpec spec,
                                                String nodeId,
                                                Map<String, AgentGraphSpec.Node> nodeById) {
        if (spec == null || spec.getEdges() == null) {
            return List.of();
        }
        List<GraphEdgeRoute> routes = new ArrayList<>();
        int index = 0;
        for (AgentGraphSpec.Edge edge : spec.getEdges()) {
            if (!nodeId.equals(asString(edge.getFrom()))) {
                continue;
            }
            String rawTarget = asString(edge.getTo());
            String target = normalizeTarget(rawTarget, nodeById);
            if (target == null) {
                continue;
            }
            String condition = normalizeCondition(edge.getCondition());
            routes.add(new GraphEdgeRoute("route_" + index, target, condition));
            index++;
        }
        return routes;
    }

    private String normalizeTarget(String target, Map<String, AgentGraphSpec.Node> nodeById) {
        if (target == null || target.isBlank()) {
            return null;
        }
        if (END.equals(target) || "end".equalsIgnoreCase(target)) {
            return END;
        }
        if (START.equals(target) || "start".equalsIgnoreCase(target)) {
            return null;
        }
        return nodeById.containsKey(target) ? target : null;
    }

    private String selectRouteKey(LangGraphState state, List<GraphEdgeRoute> routes) {
        GraphEdgeRoute fallback = null;
        for (GraphEdgeRoute route : routes) {
            if (isAlwaysCondition(route.condition())) {
                if (fallback == null) {
                    fallback = route;
                }
                continue;
            }
            if (matchesCondition(state, route.condition())) {
                return route.key();
            }
        }
        return (fallback == null ? routes.get(0) : fallback).key();
    }

    private boolean hasErrorRoute(List<GraphEdgeRoute> routes) {
        return routes.stream()
                .map(GraphEdgeRoute::condition)
                .anyMatch(condition -> "error".equalsIgnoreCase(condition) || "failure".equalsIgnoreCase(condition));
    }

    private boolean matchesCondition(LangGraphState state, String condition) {
        String normalized = normalizeCondition(condition);
        if (isAlwaysCondition(normalized)) {
            return true;
        }
        boolean success = Boolean.TRUE.equals(state.value(LAST_SUCCESS).orElse(Boolean.TRUE));
        if ("success".equalsIgnoreCase(normalized)) {
            return success;
        }
        if ("error".equalsIgnoreCase(normalized) || "failure".equalsIgnoreCase(normalized)) {
            return !success || !state.value(LAST_ERROR, "").isBlank();
        }
        String output = stringify(state.value(LAST_OUTPUT).orElse(state.value(ANSWER, "")));
        String lowerOutput = output.toLowerCase();
        String lowerCondition = normalized.toLowerCase();
        if (lowerCondition.startsWith("contains:")) {
            return lowerOutput.contains(lowerCondition.substring("contains:".length()).trim());
        }
        if (lowerCondition.startsWith("not_contains:")) {
            return !lowerOutput.contains(lowerCondition.substring("not_contains:".length()).trim());
        }
        if (lowerCondition.startsWith("equals:")) {
            return output.trim().equals(normalized.substring("equals:".length()).trim());
        }
        if (lowerCondition.startsWith("not_equals:")) {
            return !output.trim().equals(normalized.substring("not_equals:".length()).trim());
        }
        if (lowerCondition.startsWith("empty")) {
            return output.isBlank();
        }
        if (lowerCondition.startsWith("not_empty")) {
            return !output.isBlank();
        }
        return lowerOutput.contains(lowerCondition);
    }

    private static boolean isAlwaysCondition(String condition) {
        String normalized = normalizeCondition(condition);
        return normalized.isBlank()
                || "always".equalsIgnoreCase(normalized)
                || "default".equalsIgnoreCase(normalized)
                || "else".equalsIgnoreCase(normalized);
    }

    private static String normalizeCondition(String condition) {
        return condition == null ? "" : condition.trim();
    }

    private static String firstExecutableTarget(List<String> targets, Map<String, AgentGraphSpec.Node> byId) {
        if (targets == null) {
            return null;
        }
        return targets.stream()
                .map(LangGraph4jRuntimeAdapter::asString)
                .filter(byId::containsKey)
                .findFirst()
                .orElse(null);
    }

    private boolean isSupportedNode(AgentGraphSpec.Node node) {
        return isLlmNode(node) || isToolNode(node);
    }

    private boolean isLlmNode(AgentGraphSpec.Node node) {
        String type = asString(node == null ? null : node.getType());
        return "LLM".equalsIgnoreCase(type) || NODE_LLM.equalsIgnoreCase(type);
    }

    private boolean isToolNode(AgentGraphSpec.Node node) {
        String type = asString(node == null ? null : node.getType());
        return "TOOL".equalsIgnoreCase(type)
                || NODE_TOOL.equalsIgnoreCase(type)
                || "CAPABILITY".equalsIgnoreCase(type)
                || NODE_CAPABILITY.equalsIgnoreCase(type);
    }

    private String resolveToolName(AgentGraphSpec.Node node) {
        Map<String, Object> config = safeMap(node.getConfig());
        AgentGraphSpec.CapabilityRef ref = node.getRef();
        String name = firstNonBlank(
                ref == null ? null : ref.getQualifiedName(),
                asString(config.get(CONFIG_QUALIFIED_NAME)));
        name = firstNonBlank(name, ref == null ? null : ref.getName());
        name = firstNonBlank(name, asString(config.get(CONFIG_TOOL_NAME)));
        name = firstNonBlank(name, node.getName());
        name = firstNonBlank(name, node.getId());
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool node has no resolvable name: " + node.getId());
        }
        return name;
    }

    private String resolveExecutableToolName(AgentGraphSpec.Node node) {
        String name = resolveToolName(node);
        if (toolDefinitionService.findByName(name).isPresent()) {
            return name;
        }
        return toolDefinitionService.findByQualifiedName(name)
                .map(entity -> firstNonBlank(entity.getName(), name))
                .orElse(name);
    }

    private Map<String, Object> resolveToolArgs(LangGraphState state, AgentGraphSpec.Node node) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("input", state.value(INPUT, ""));
        state.value(ANSWER).ifPresent(answer -> args.put("answer", answer));
        state.value(LAST_OUTPUT).ifPresent(output -> args.put("previousOutput", output));
        Map<String, Object> config = safeMap(node.getConfig());
        Object configured = config.containsKey(CONFIG_ARGS) ? config.get(CONFIG_ARGS) : config.get(CONFIG_TOOL_ARGS);
        if (configured instanceof Map<?, ?> map) {
            map.forEach((key, value) -> args.put(String.valueOf(key), value));
        }
        Object inputMapping = config.get(CONFIG_INPUT_MAPPING);
        if (inputMapping instanceof Map<?, ?> map) {
            map.forEach((target, expression) -> putMappedArg(args,
                    String.valueOf(target),
                    resolveExpression(state, String.valueOf(expression))));
        }
        args.putIfAbsent("query", firstNonBlank(asString(args.get("answer")), asString(args.get("input"))));
        return args;
    }

    private String renderModelInput(LangGraphState state) {
        String input = state.value(INPUT, "");
        Object lastOutput = state.value(LAST_OUTPUT).orElse(null);
        if (lastOutput == null) {
            return input;
        }
        return input + "\n\nPrevious node output:\n" + stringify(lastOutput);
    }

    private Object resolveExpression(LangGraphState state, String expression) {
        String expr = expression == null ? "" : expression.trim();
        if (expr.isBlank()) {
            return "";
        }
        if (expr.startsWith("const:")) {
            return expr.substring("const:".length());
        }
        if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
            return expr.substring(1, expr.length() - 1);
        }
        if ("true".equalsIgnoreCase(expr) || "false".equalsIgnoreCase(expr)) {
            return Boolean.parseBoolean(expr);
        }
        if ("null".equalsIgnoreCase(expr)) {
            return null;
        }
        Number numeric = tryParseNumber(expr);
        if (numeric != null) {
            return numeric;
        }

        String path = expr.startsWith("$") ? expr.substring(1) : expr;
        Object exact = stateValue(state, path);
        if (exact != null) {
            return exact;
        }
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return expr;
        }
        Object root = stateValue(state, parts[0]);
        if (root == null && parts.length > 1 && "nodeOutput".equals(parts[0])) {
            root = stateValue(state, nodeOutputKey(parts[1]));
            return traverse(root, parts, 2, expr);
        }
        if (root == null) {
            root = stateValue(state, VAR_PREFIX + parts[0]);
        }
        return traverse(root, parts, 1, expr);
    }

    private Object stateValue(LangGraphState state, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return switch (key) {
            case "input" -> state.value(INPUT).orElse(null);
            case "answer" -> state.value(ANSWER).orElse(null);
            case "lastOutput", "previousOutput" -> state.value(LAST_OUTPUT).orElse(null);
            case "lastError" -> state.value(LAST_ERROR).orElse(null);
            case "lastSuccess" -> state.value(LAST_SUCCESS).orElse(null);
            default -> {
                Object exact = state.value(key).orElse(null);
                if (exact != null) {
                    yield exact;
                }
                Object alias = state.value(VAR_PREFIX + key).orElse(null);
                if (alias != null) {
                    yield alias;
                }
                yield state.value(nodeOutputKey(key)).orElse(null);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Object traverse(Object value, String[] parts, int start, String fallback) {
        Object current = value;
        for (int i = start; i < parts.length; i++) {
            if (current == null) {
                return fallback;
            }
            String part = parts[i];
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
                continue;
            }
            if (current instanceof List<?> list) {
                Integer index = tryParseIndex(part);
                current = index == null || index < 0 || index >= list.size() ? null : list.get(index);
                continue;
            }
            return fallback;
        }
        return current == null ? fallback : current;
    }

    @SuppressWarnings("unchecked")
    private void putMappedArg(Map<String, Object> args, String targetPath, Object value) {
        String target = targetPath == null ? "" : targetPath.trim();
        if (target.isBlank()) {
            return;
        }
        String[] parts = target.split("\\.");
        Map<String, Object> cursor = args;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i].trim();
            if (part.isBlank()) {
                return;
            }
            Object next = cursor.get(part);
            if (!(next instanceof Map<?, ?>)) {
                next = new LinkedHashMap<String, Object>();
                cursor.put(part, next);
            }
            cursor = (Map<String, Object>) next;
        }
        String leaf = parts[parts.length - 1].trim();
        if (!leaf.isBlank()) {
            cursor.put(leaf, value);
        }
    }

    private void putOutputAlias(Map<String, Object> update, AgentGraphSpec.Node node, Object value) {
        String alias = asString(safeMap(node.getConfig()).get(CONFIG_OUTPUT_ALIAS));
        if (alias.isBlank()) {
            return;
        }
        update.put(VAR_PREFIX + alias, value);
        update.put(alias, value);
    }

    private Number tryParseNumber(String value) {
        try {
            if (value.matches("-?\\d+")) {
                return Long.parseLong(value);
            }
            if (value.matches("-?\\d+\\.\\d+")) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private Integer tryParseIndex(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String finalAnswer(LangGraphState state) {
        String answer = state.value(ANSWER, "");
        if (!answer.isBlank()) {
            return answer;
        }
        return stringify(state.value(LAST_OUTPUT).orElse(""));
    }

    private List<String> steps(List<AgentGraphSpec.Node> nodes) {
        List<String> steps = new ArrayList<>();
        steps.add("Runtime: " + runtimeType());
        nodes.forEach(node -> steps.add("Graph node: " + node.getId() + " (" + node.getType() + ")"));
        return steps;
    }

    private List<String> toolCalls(List<AgentGraphSpec.Node> nodes) {
        return nodes.stream()
                .filter(this::isToolNode)
                .map(this::resolveToolName)
                .toList();
    }

    private static Map<String, Object> tokenUsage(ModelServiceClient.ModelUsage usage) {
        if (usage == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("promptTokens", usage.getPromptTokens());
        result.put("completionTokens", usage.getCompletionTokens());
        result.put("totalTokens", usage.getTotalTokens());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
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

    private static List<String> configuredGraphNodes(List<AgentGraphSpec.Node> nodes) {
        return nodes.stream()
                .map(node -> asString(node.getId()))
                .filter(id -> !id.isBlank())
                .toList();
    }

    private static Object configuredGraphEdges(AgentDefinition definition) {
        AgentGraphSpec spec = graphSpec(definition);
        if (spec == null || spec.getEdges() == null) {
            return List.of();
        }
        return spec.getEdges().stream()
                .map(edge -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("from", nullToEmpty(edge.getFrom()));
                    item.put("to", nullToEmpty(edge.getTo()));
                    item.put("condition", nullToEmpty(edge.getCondition()));
                    return item;
                })
                .toList();
    }

    private static Map<String, Object> nodeMetadata(AgentGraphSpec.Node node) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nodeId", node.getId());
        metadata.put("nodeType", node.getType());
        metadata.put("nodeName", node.getName());
        if (node.getRef() != null) {
            metadata.put("refKind", node.getRef().getKind());
            metadata.put("refName", node.getRef().getName());
            metadata.put("refQualifiedName", node.getRef().getQualifiedName());
            metadata.put("refDefinitionId", node.getRef().getDefinitionId());
        }
        return metadata;
    }

    private static AgentGraphSpec graphSpec(AgentDefinition definition) {
        return definition == null ? null : definition.getGraphSpec();
    }

    private static String nodeOutputKey(String nodeId) {
        return "nodeOutput." + nodeId;
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence s) {
            return s.toString();
        }
        return String.valueOf(value);
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    public static class LangGraphState extends AgentState {
        public LangGraphState(Map<String, Object> initData) {
            super(initData);
        }
    }

    private record GraphEdgeRoute(String key, String target, String condition) {
    }
}
