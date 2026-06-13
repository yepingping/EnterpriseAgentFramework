package com.enterprise.ai.agent.agent;

import com.enterprise.ai.agent.graph.GraphSpec;

import java.util.List;
import java.util.Map;

public final class AgentDefinitionGraphBootstrapper {

    private static final String LANGGRAPH4J = "LANGGRAPH4J";
    private static final String STARTER_NODE_ID = "starter_answer";
    private static final String STARTER_TEMPLATE = "请在 Agent Studio 中配置流程节点。";

    private AgentDefinitionGraphBootstrapper() {
    }

    public static void bootstrapLangGraphIfEmpty(AgentDefinition definition) {
        if (definition == null || !LANGGRAPH4J.equalsIgnoreCase(nullToEmpty(definition.getRuntimeType()))) {
            return;
        }
        GraphSpec graphSpec = definition.getGraphSpec();
        List<GraphSpec.Node> nodes = graphSpec == null || graphSpec.getNodes() == null ? List.of() : graphSpec.getNodes();
        boolean hasExecutableNode = nodes.stream()
                .anyMatch(node -> node != null && !nullToEmpty(node.getId()).isBlank() && !nullToEmpty(node.getType()).isBlank());
        if (hasExecutableNode) {
            normalizeExistingGraph(definition, graphSpec);
            return;
        }
        definition.setGraphSpec(starterGraph(definition, graphSpec));
    }

    private static void normalizeExistingGraph(AgentDefinition definition, GraphSpec graphSpec) {
        graphSpec.setRuntimeHint(LANGGRAPH4J);
        if (nullToEmpty(graphSpec.getMode()).isBlank()) {
            graphSpec.setMode("WORKFLOW");
        }
        if (nullToEmpty(graphSpec.getCode()).isBlank()) {
            graphSpec.setCode(firstNonBlank(definition.getKeySlug(), definition.getName(), "agent_graph"));
        }
        if (nullToEmpty(graphSpec.getName()).isBlank()) {
            graphSpec.setName(firstNonBlank(definition.getName(), "Agent Graph"));
        }
    }

    private static GraphSpec starterGraph(AgentDefinition definition, GraphSpec source) {
        return GraphSpec.builder()
                .code(firstNonBlank(source == null ? null : source.getCode(), definition.getKeySlug(), definition.getName(), "agent_graph"))
                .name(firstNonBlank(source == null ? null : source.getName(), definition.getName(), "Agent Graph"))
                .mode("WORKFLOW")
                .runtimeHint(LANGGRAPH4J)
                .node(GraphSpec.Node.builder()
                        .id(STARTER_NODE_ID)
                        .type("ANSWER")
                        .name("默认回复")
                        .description("新建流程智能体的初始占位节点，可在 Agent Studio 中替换为真实流程。")
                        .config(Map.of("template", STARTER_TEMPLATE))
                        .build())
                .edge(GraphSpec.Edge.builder().from("START").to(STARTER_NODE_ID).condition("always").build())
                .edge(GraphSpec.Edge.builder().from(STARTER_NODE_ID).to("END").condition("always").build())
                .entry(STARTER_NODE_ID)
                .finishNode(STARTER_NODE_ID)
                .build();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!nullToEmpty(value).isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
