package com.enterprise.ai.agent.agent;

import com.enterprise.ai.agent.graph.AgentGraphSpec;
import com.enterprise.ai.agent.runtime.AgentRuntimeRequest;
import com.enterprise.ai.agent.runtime.AgentRuntimeSelector;
import com.enterprise.ai.agent.runtime.AgentRuntimeValidationResult;
import com.enterprise.ai.agent.tools.definition.ToolDefinitionEntity;
import com.enterprise.ai.agent.tools.definition.ToolDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentReleaseValidationService {

    private static final String START = "START";
    private static final String END = "END";
    private static final Set<String> SUPPORTED_NODE_TYPES = Set.of("LLM", "TOOL", "CAPABILITY");
    private static final Set<String> SUPPORTED_EDGE_CONDITIONS = Set.of(
            "always", "default", "else", "success", "error", "failure", "empty", "not_empty"
    );

    private final AgentRuntimeSelector runtimeSelector;
    private final ToolDefinitionService toolDefinitionService;

    public AgentReleaseValidationResult validate(AgentDefinition definition) {
        AgentReleaseValidationResult.Builder report = AgentReleaseValidationResult.builder();
        validateBasicFields(definition, report);
        validateRuntime(definition, report);
        validateGraph(definition, report);
        return report.build();
    }

    private void validateBasicFields(AgentDefinition definition, AgentReleaseValidationResult.Builder report) {
        if (definition == null) {
            report.error("AGENT_NOT_FOUND", null, "Agent 定义不存在");
            return;
        }
        if (!StringUtils.hasText(definition.getName())) {
            report.error("AGENT_NAME_EMPTY", null, "Agent 名称不能为空");
        }
        if (!StringUtils.hasText(definition.getKeySlug())) {
            report.error("AGENT_KEY_SLUG_EMPTY", null, "发布端点 keySlug 不能为空");
        }
        if (!definition.isEnabled()) {
            report.warn("AGENT_DISABLED", null, "Agent 当前未启用，发布后网关仍不可调用");
        }
        if (!StringUtils.hasText(definition.getModelInstanceId())) {
            report.error("AGENT_MODEL_EMPTY", null, "模型实例不能为空");
        }
    }

    private void validateRuntime(AgentDefinition definition, AgentReleaseValidationResult.Builder report) {
        if (definition == null) {
            return;
        }
        AgentRuntimeValidationResult validation = runtimeSelector.validate(AgentRuntimeRequest.builder()
                .agentDefinition(definition)
                .build());
        if (!validation.isValid()) {
            report.error(firstText(validation.getErrorCode(), "RUNTIME_INVALID"), null,
                    firstText(validation.getMessage(), "Runtime 配置不可用"));
        }

        String placement = normalize(definition.getRuntimePlacement(), "CENTRAL");
        if ("EMBEDDED".equals(placement) || "HYBRID".equals(placement)) {
            Map<String, Object> target = embeddedTarget(definition);
            if (target == null) {
                report.error("EMBEDDED_TARGET_MISSING", null,
                        "本地/混合运行必须选择一个 Runtime 实例");
            }
        }
    }

    private void validateGraph(AgentDefinition definition, AgentReleaseValidationResult.Builder report) {
        if (definition == null || definition.getGraphSpec() == null) {
            if ("LANGGRAPH4J".equalsIgnoreCase(definition == null ? null : definition.getRuntimeType())) {
                report.error("GRAPH_SPEC_MISSING", null, "LangGraph4j Runtime 发布必须包含 GraphSpec");
            }
            return;
        }

        AgentGraphSpec graph = definition.getGraphSpec();
        List<AgentGraphSpec.Node> nodes = graph.getNodes() == null ? List.of() : graph.getNodes();
        List<AgentGraphSpec.Edge> edges = graph.getEdges() == null ? List.of() : graph.getEdges();
        if (nodes.isEmpty()) {
            report.error("GRAPH_NODE_EMPTY", null, "GraphSpec 至少需要一个节点");
            return;
        }

        Map<String, AgentGraphSpec.Node> byId = new LinkedHashMap<>();
        Set<String> duplicateIds = new HashSet<>();
        for (AgentGraphSpec.Node node : nodes) {
            if (node == null) {
                report.error("GRAPH_NODE_EMPTY_ITEM", null, "GraphSpec node item cannot be null");
                continue;
            }
            String id = trim(node.getId());
            if (!StringUtils.hasText(id)) {
                report.error("GRAPH_NODE_ID_EMPTY", null, "节点 id 不能为空");
                continue;
            }
            if (byId.putIfAbsent(id, node) != null) {
                duplicateIds.add(id);
            }
            validateNode(node, definition, report);
        }
        for (String id : duplicateIds) {
            report.error("GRAPH_DUPLICATE_NODE_ID", id, "节点 id 重复: " + id);
        }

        String entry = StringUtils.hasText(graph.getEntry()) ? graph.getEntry().trim() : null;
        if (entry == null) {
            report.error("GRAPH_ENTRY_MISSING", null, "GraphSpec 必须配置入口节点");
        } else if (!byId.containsKey(entry)) {
            report.error("GRAPH_ENTRY_INVALID", entry, "入口节点不存在: " + entry);
        }

        boolean hasLlm = nodes.stream().anyMatch(node -> node != null && "LLM".equalsIgnoreCase(node.getType()));
        if (!hasLlm) {
            report.error("GRAPH_LLM_MISSING", null, "GraphSpec 至少需要一个 LLM 节点");
        }

        Map<String, List<AgentGraphSpec.Edge>> outgoing = new HashMap<>();
        Set<String> incoming = new HashSet<>();
        for (AgentGraphSpec.Edge edge : edges) {
            if (edge == null) {
                report.error("GRAPH_EDGE_EMPTY", null, "GraphSpec 杩炵嚎涓嶈兘涓虹┖");
                continue;
            }
            validateEdge(edge, byId, report);
            if (StringUtils.hasText(edge.getFrom())) {
                outgoing.computeIfAbsent(edge.getFrom().trim(), ignored -> new java.util.ArrayList<>()).add(edge);
            }
            if (StringUtils.hasText(edge.getTo()) && !isEnd(edge.getTo())) {
                incoming.add(edge.getTo().trim());
            }
        }

        if (entry != null && byId.containsKey(entry)) {
            Set<String> reachable = reachable(entry, outgoing);
            for (String nodeId : byId.keySet()) {
                if (!reachable.contains(nodeId)) {
                    report.error("GRAPH_UNREACHABLE_NODE", nodeId, "节点不可从入口到达: " + nodeId);
                }
            }
        }

        Set<String> finish = new HashSet<>(graph.getFinish() == null ? List.of() : graph.getFinish());
        for (String finishNode : finish) {
            if (!byId.containsKey(finishNode)) {
                report.error("GRAPH_FINISH_INVALID", finishNode, "结束节点不存在: " + finishNode);
            }
        }
        for (String nodeId : byId.keySet()) {
            if (!nodeId.equals(entry) && !incoming.contains(nodeId)) {
                report.warn("GRAPH_ISOLATED_INPUT", nodeId, "节点没有入边: " + nodeId);
            }
            if (!finish.contains(nodeId) && outgoing.getOrDefault(nodeId, List.of()).isEmpty()) {
                report.error("GRAPH_DEAD_END_NODE", nodeId, "非结束节点没有出边: " + nodeId);
            }
        }
        validateMappings(nodes, report);
    }

    private void validateNode(AgentGraphSpec.Node node,
                              AgentDefinition definition,
                              AgentReleaseValidationResult.Builder report) {
        String id = trim(node.getId());
        String type = normalize(node.getType(), "");
        if (!SUPPORTED_NODE_TYPES.contains(type)) {
            report.error("GRAPH_NODE_TYPE_UNSUPPORTED", id, "当前 Runtime 不支持节点类型: " + node.getType());
            return;
        }
        if ("TOOL".equals(type) || "CAPABILITY".equals(type)) {
            validateCapabilityRef(type, node, definition, report);
        }
    }

    private void validateCapabilityRef(String type,
                                       AgentGraphSpec.Node node,
                                       AgentDefinition definition,
                                       AgentReleaseValidationResult.Builder report) {
        AgentGraphSpec.CapabilityRef ref = node.getRef();
        Optional<ToolDefinitionEntity> found = Optional.empty();
        if (ref != null && ref.getDefinitionId() != null) {
            found = toolDefinitionService.findById(ref.getDefinitionId());
        }
        if (found.isEmpty() && ref != null && StringUtils.hasText(ref.getQualifiedName())) {
            found = toolDefinitionService.findByQualifiedName(ref.getQualifiedName());
        }
        if (found.isEmpty() && ref != null && StringUtils.hasText(ref.getName())) {
            found = toolDefinitionService.findByName(ref.getName());
        }
        if (found.isEmpty()) {
            report.error("GRAPH_REF_NOT_FOUND", node.getId(), "节点引用的 Tool/Capability 不存在");
            return;
        }
        ToolDefinitionEntity entity = found.get();
        String expectedKind = "TOOL".equals(type) ? "TOOL" : "SKILL";
        if (entity.getKind() != null && !expectedKind.equalsIgnoreCase(entity.getKind())) {
            report.warn("GRAPH_REF_KIND_MISMATCH", node.getId(),
                    "节点类型与引用能力类型不一致: node=" + type + ", ref=" + entity.getKind());
        }
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            report.error("GRAPH_REF_DISABLED", node.getId(), "引用能力未启用: " + entity.getName());
        }
        if (!Boolean.TRUE.equals(entity.getAgentVisible())) {
            report.error("GRAPH_REF_NOT_AGENT_VISIBLE", node.getId(), "引用能力未开放给 Agent: " + entity.getName());
        }
        if (Boolean.TRUE.equals(entity.getDraft())) {
            report.error("GRAPH_REF_DRAFT", node.getId(), "引用能力仍是草稿，不能发布: " + entity.getName());
        }
        if (!visibilityAllowed(definition, entity)) {
            report.error("GRAPH_REF_VISIBILITY_DENIED", node.getId(), "跨项目引用不符合 visibility 规则: " + entity.getName());
        }
    }

    private void validateEdge(AgentGraphSpec.Edge edge,
                              Map<String, AgentGraphSpec.Node> byId,
                              AgentReleaseValidationResult.Builder report) {
        String from = trim(edge.getFrom());
        String to = trim(edge.getTo());
        if (!StringUtils.hasText(from) || (!START.equalsIgnoreCase(from) && !byId.containsKey(from))) {
            report.error("GRAPH_EDGE_FROM_INVALID", from, "连线来源节点不存在: " + from);
        }
        if (END.equalsIgnoreCase(from)) {
            report.error("GRAPH_EDGE_FROM_END", from, "END cannot be an edge source");
        }
        if (!StringUtils.hasText(to) || (!END.equalsIgnoreCase(to) && !byId.containsKey(to))) {
            report.error("GRAPH_EDGE_TO_INVALID", to, "连线目标节点不存在: " + to);
        }
        if (START.equalsIgnoreCase(to)) {
            report.error("GRAPH_EDGE_TO_START", to, "START cannot be an edge target");
        }
        String condition = trim(edge.getCondition());
        if (StringUtils.hasText(condition) && !isSupportedCondition(condition)) {
            report.error("GRAPH_EDGE_CONDITION_UNSUPPORTED", from, "不支持的连线条件: " + condition);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateMappings(List<AgentGraphSpec.Node> nodes, AgentReleaseValidationResult.Builder report) {
        Set<String> nodeIds = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        for (AgentGraphSpec.Node node : nodes) {
            if (node == null) {
                continue;
            }
            nodeIds.add(node.getId());
            Object alias = node.getConfig() == null ? null : node.getConfig().get("outputAlias");
            if (alias != null && StringUtils.hasText(String.valueOf(alias))) {
                aliases.add(String.valueOf(alias).trim());
            }
        }
        for (AgentGraphSpec.Node node : nodes) {
            if (node == null) {
                continue;
            }
            Object raw = node.getConfig() == null ? null : node.getConfig().get("inputMapping");
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String target = entry.getKey() == null ? null : String.valueOf(entry.getKey()).trim();
                if (!StringUtils.hasText(target)) {
                    report.error("GRAPH_MAPPING_TARGET_EMPTY", node.getId(), "inputMapping 目标参数不能为空");
                }
                Object source = entry.getValue();
                validateMappingSource(node.getId(), source, nodeIds, aliases, report);
            }
        }
    }

    private void validateMappingSource(String nodeId,
                                       Object source,
                                       Set<String> nodeIds,
                                       Set<String> aliases,
                                       AgentReleaseValidationResult.Builder report) {
        if (!(source instanceof String raw)) {
            return;
        }
        String value = raw.trim();
        if (!StringUtils.hasText(value) || value.startsWith("\"") || value.startsWith("const:")
                || value.equals("null") || value.equals("true") || value.equals("false")
                || value.equals("$input") || value.equals("input")
                || value.equals("$answer") || value.equals("answer")
                || value.equals("$lastOutput") || value.equals("previousOutput")) {
            return;
        }
        if (value.startsWith("nodeOutput.")) {
            String referenced = value.substring("nodeOutput.".length()).split("\\.", 2)[0];
            if (!nodeIds.contains(referenced)) {
                report.error("GRAPH_MAPPING_NODE_REF_INVALID", nodeId, "inputMapping 引用了不存在的节点输出: " + referenced);
            }
            return;
        }
        String alias = value.split("\\.", 2)[0];
        if (!aliases.contains(alias)) {
            report.warn("GRAPH_MAPPING_ALIAS_UNKNOWN", nodeId, "inputMapping 引用了未知 outputAlias: " + alias);
        }
    }

    private Set<String> reachable(String entry, Map<String, List<AgentGraphSpec.Edge>> outgoing) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(entry);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            for (AgentGraphSpec.Edge edge : outgoing.getOrDefault(current, List.of())) {
                if (StringUtils.hasText(edge.getTo()) && !isEnd(edge.getTo()) && !START.equalsIgnoreCase(edge.getTo())) {
                    queue.add(edge.getTo().trim());
                }
            }
        }
        return visited;
    }

    private boolean isSupportedCondition(String condition) {
        String normalized = condition.trim().toLowerCase(Locale.ROOT);
        if (SUPPORTED_EDGE_CONDITIONS.contains(normalized)) {
            return true;
        }
        return normalized.startsWith("contains:")
                || normalized.startsWith("not_contains:")
                || normalized.startsWith("equals:")
                || normalized.startsWith("not_equals:");
    }

    private boolean visibilityAllowed(AgentDefinition definition, ToolDefinitionEntity entity) {
        String agentProjectCode = definition == null ? null : trim(definition.getProjectCode());
        String toolProjectCode = trim(entity.getProjectCode());
        if (!StringUtils.hasText(toolProjectCode) || toolProjectCode.equals(agentProjectCode)) {
            return true;
        }
        String visibility = normalize(entity.getVisibility(), "PRIVATE");
        return "PUBLIC".equals(visibility) || "SHARED".equals(visibility);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> embeddedTarget(AgentDefinition definition) {
        if (definition.getRuntimeConfig() == null) {
            return null;
        }
        Object raw = definition.getRuntimeConfig().get("embeddedRuntime");
        if (!(raw instanceof Map<?, ?>)) {
            raw = definition.getRuntimeConfig().get("runtimeInstance");
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Object projectCode = map.get("projectCode");
        Object instanceId = map.get("instanceId");
        if (!StringUtils.hasText(projectCode == null ? null : String.valueOf(projectCode))
                || !StringUtils.hasText(instanceId == null ? null : String.valueOf(instanceId))) {
            return null;
        }
        return (Map<String, Object>) map;
    }

    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isEnd(String value) {
        return END.equalsIgnoreCase(trim(value));
    }
}
