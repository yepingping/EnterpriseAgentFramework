package com.enterprise.ai.spring.registry;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LangGraph4j-like builder that emits the platform GraphSpec contract.
 */
public final class EafGraph {

    public static final String START = "START";
    public static final String END = "END";

    private EafGraph() {
    }

    public static Builder agent(String code) {
        return new Builder(code);
    }

    public static class Builder {
        private final String code;
        private String name;
        private String description;
        private String runtimeType = "LANGGRAPH4J";
        private String modelInstanceId;
        private String systemPrompt;
        private String visibility = "PROJECT";
        private final List<NodeDraft> nodes = new ArrayList<>();
        private final List<EdgeDraft> edges = new ArrayList<>();
        private final Map<String, Object> metadata = new LinkedHashMap<>();
        private NodeDraft currentNode;
        private String explicitEntry;
        private final List<String> finish = new ArrayList<>();

        private Builder(String code) {
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("agent graph code is required");
            }
            this.code = code.trim();
            this.name = this.code;
        }

        public Builder name(String name) {
            this.name = textOr(name, this.name);
            return this;
        }

        public Builder description(String description) {
            this.description = trimToNull(description);
            return this;
        }

        public Builder runtimeType(String runtimeType) {
            this.runtimeType = textOr(runtimeType, this.runtimeType).toUpperCase();
            return this;
        }

        public Builder modelInstanceId(String modelInstanceId) {
            this.modelInstanceId = trimToNull(modelInstanceId);
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = trimToNull(systemPrompt);
            return this;
        }

        public Builder visibility(String visibility) {
            this.visibility = textOr(visibility, this.visibility).toUpperCase();
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (StringUtils.hasText(key) && value != null) {
                metadata.put(key.trim(), value);
            }
            return this;
        }

        public Builder llm(String id) {
            currentNode = addNode(id, "LLM");
            if (modelInstanceId != null) {
                currentNode.config.put("modelInstanceId", modelInstanceId);
            }
            return this;
        }

        public Builder tool(String id) {
            currentNode = addNode(id, "TOOL");
            currentNode.ref.put("kind", "TOOL");
            currentNode.ref.put("name", id);
            return this;
        }

        public Builder capability(String id) {
            currentNode = addNode(id, "CAPABILITY");
            currentNode.ref.put("kind", "CAPABILITY");
            currentNode.ref.put("name", id);
            return this;
        }

        public Builder label(String label) {
            requireCurrentNode().name = trimToNull(label);
            return this;
        }

        public Builder ref(String qualifiedName) {
            NodeDraft node = requireCurrentNode();
            if (!StringUtils.hasText(qualifiedName)) {
                return this;
            }
            String trimmed = qualifiedName.trim();
            String name = trimmed.contains(":") ? trimmed.substring(trimmed.lastIndexOf(':') + 1) : trimmed;
            node.ref.put("name", name);
            node.ref.put("qualifiedName", trimmed);
            if (trimmed.contains(":")) {
                node.ref.put("projectCode", trimmed.substring(0, trimmed.indexOf(':')));
            }
            return this;
        }

        public Builder input(String targetPath, String expression) {
            if (!StringUtils.hasText(targetPath) || !StringUtils.hasText(expression)) {
                return this;
            }
            NodeDraft node = requireCurrentNode();
            @SuppressWarnings("unchecked")
            Map<String, String> inputMapping = (Map<String, String>) node.config.computeIfAbsent(
                    "inputMapping", ignored -> new LinkedHashMap<String, String>());
            inputMapping.put(targetPath.trim(), expression.trim());
            return this;
        }

        public Builder outputAlias(String alias) {
            if (StringUtils.hasText(alias)) {
                requireCurrentNode().config.put("outputAlias", alias.trim());
            }
            return this;
        }

        public Builder config(String key, Object value) {
            if (StringUtils.hasText(key) && value != null) {
                requireCurrentNode().config.put(key.trim(), value);
            }
            return this;
        }

        public Builder entry(String nodeId) {
            this.explicitEntry = normalizeEndpoint(nodeId);
            return this;
        }

        public Builder finish(String nodeId) {
            String normalized = normalizeEndpoint(nodeId);
            if (!END.equals(normalized)) {
                finish.add(normalized);
            }
            return this;
        }

        public EdgeBuilder edge(String from, String to) {
            EdgeDraft edge = new EdgeDraft(normalizeEndpoint(from), normalizeEndpoint(to), "always");
            edges.add(edge);
            return new EdgeBuilder(this, edge);
        }

        public EafAgentGraph build() {
            validate();
            normalizeNodes();
            List<EdgeDraft> normalizedEdges = new ArrayList<>(edges);
            String entry = resolveEntry(normalizedEdges);
            ensureStartEdge(normalizedEdges, entry);
            ensureFinishEdge(normalizedEdges);
            Map<String, Object> graphSpec = new LinkedHashMap<>();
            graphSpec.put("code", code);
            graphSpec.put("name", name);
            graphSpec.put("mode", "WORKFLOW");
            graphSpec.put("runtimeHint", runtimeType);
            graphSpec.put("nodes", nodes.stream().map(NodeDraft::toMap).toList());
            graphSpec.put("edges", normalizedEdges.stream().map(EdgeDraft::toMap).toList());
            graphSpec.put("entry", entry);
            graphSpec.put("finish", finish.isEmpty() ? List.of(lastExecutableNodeId()) : List.copyOf(finish));
            return new EafAgentGraph(
                    code,
                    name,
                    description,
                    runtimeType,
                    modelInstanceId,
                    systemPrompt,
                    visibility,
                    graphSpec,
                    Map.copyOf(metadata));
        }

        private NodeDraft addNode(String id, String type) {
            if (!StringUtils.hasText(id)) {
                throw new IllegalArgumentException("node id is required");
            }
            NodeDraft node = new NodeDraft(id.trim(), type);
            nodes.add(node);
            return node;
        }

        private NodeDraft requireCurrentNode() {
            if (currentNode == null) {
                throw new IllegalStateException("No current node. Call llm/tool/capability first.");
            }
            return currentNode;
        }

        private void validate() {
            Set<String> ids = new LinkedHashSet<>();
            boolean hasLlm = false;
            for (NodeDraft node : nodes) {
                if (!ids.add(node.id)) {
                    throw new IllegalArgumentException("duplicate graph node id: " + node.id);
                }
                hasLlm = hasLlm || "LLM".equals(node.type);
            }
            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("agent graph must declare at least one node");
            }
            if (!hasLlm) {
                throw new IllegalArgumentException("agent graph must declare at least one LLM node");
            }
            for (EdgeDraft edge : edges) {
                requireEndpoint(ids, edge.from);
                requireEndpoint(ids, edge.to);
            }
        }

        private void normalizeNodes() {
            if (!StringUtils.hasText(modelInstanceId)) {
                return;
            }
            for (NodeDraft node : nodes) {
                if ("LLM".equals(node.type)) {
                    node.config.putIfAbsent("modelInstanceId", modelInstanceId);
                }
            }
        }

        private void requireEndpoint(Set<String> ids, String endpoint) {
            if (!START.equals(endpoint) && !END.equals(endpoint) && !ids.contains(endpoint)) {
                throw new IllegalArgumentException("edge endpoint does not exist: " + endpoint);
            }
        }

        private String resolveEntry(List<EdgeDraft> normalizedEdges) {
            if (StringUtils.hasText(explicitEntry)) {
                return explicitEntry;
            }
            return normalizedEdges.stream()
                    .filter(edge -> START.equals(edge.from))
                    .map(edge -> edge.to)
                    .filter(to -> !END.equals(to))
                    .findFirst()
                    .orElse(nodes.get(0).id);
        }

        private void ensureStartEdge(List<EdgeDraft> normalizedEdges, String entry) {
            boolean exists = normalizedEdges.stream().anyMatch(edge -> START.equals(edge.from));
            if (!exists) {
                normalizedEdges.add(0, new EdgeDraft(START, entry, "always"));
            }
        }

        private void ensureFinishEdge(List<EdgeDraft> normalizedEdges) {
            boolean hasEnd = normalizedEdges.stream().anyMatch(edge -> END.equals(edge.to));
            if (!hasEnd) {
                normalizedEdges.add(new EdgeDraft(lastExecutableNodeId(), END, "always"));
            }
        }

        private String lastExecutableNodeId() {
            return nodes.get(nodes.size() - 1).id;
        }

        private String normalizeEndpoint(String id) {
            if (!StringUtils.hasText(id)) {
                throw new IllegalArgumentException("edge endpoint is required");
            }
            String trimmed = id.trim();
            if ("start".equalsIgnoreCase(trimmed)) {
                return START;
            }
            if ("end".equalsIgnoreCase(trimmed)) {
                return END;
            }
            return trimmed;
        }

        private String textOr(String value, String fallback) {
            return StringUtils.hasText(value) ? value.trim() : fallback;
        }

        private String trimToNull(String value) {
            return StringUtils.hasText(value) ? value.trim() : null;
        }
    }

    public static class EdgeBuilder {
        private final Builder parent;
        private final EdgeDraft edge;

        private EdgeBuilder(Builder parent, EdgeDraft edge) {
            this.parent = parent;
            this.edge = edge;
        }

        public Builder always() {
            edge.condition = "always";
            return parent;
        }

        public Builder when(String condition) {
            edge.condition = StringUtils.hasText(condition) ? condition.trim() : "always";
            return parent;
        }

        public EafAgentGraph build() {
            return parent.build();
        }
    }

    private static class NodeDraft {
        private final String id;
        private final String type;
        private String name;
        private final Map<String, Object> ref = new LinkedHashMap<>();
        private final Map<String, Object> config = new LinkedHashMap<>();

        private NodeDraft(String id, String type) {
            this.id = id;
            this.type = type;
            this.name = id;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", id);
            out.put("type", type);
            out.put("name", name);
            if (!ref.isEmpty()) {
                out.put("ref", ref);
            }
            if (!config.isEmpty()) {
                out.put("config", config);
            }
            return out;
        }
    }

    private static class EdgeDraft {
        private final String from;
        private final String to;
        private String condition;

        private EdgeDraft(String from, String to, String condition) {
            this.from = from;
            this.to = to;
            this.condition = condition;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("from", from);
            out.put("to", to);
            out.put("condition", condition);
            return out;
        }
    }
}
