package com.enterprise.ai.spring.registry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EafGraphTest {

    @Test
    @SuppressWarnings("unchecked")
    void builderEmitsPlatformGraphSpec() {
        EafAgentGraph graph = EafGraph.agent("order_assistant")
                .name("Order Assistant")
                .modelInstanceId("llm-1")
                .systemPrompt("Help users with order questions.")
                .llm("classify")
                .tool("queryOrder")
                .ref("order-service:queryOrder")
                .input("orderNo", "$.message.orderNo")
                .outputAlias("order")
                .edge("classify", "queryOrder").when("success")
                .edge("queryOrder", EafGraph.END).always()
                .build();

        assertEquals("order_assistant", graph.code());
        assertEquals("LANGGRAPH4J", graph.runtimeType());
        Map<String, Object> spec = graph.graphSpec();
        assertEquals("classify", spec.get("entry"));
        assertEquals(List.of("queryOrder"), spec.get("finish"));
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) spec.get("nodes");
        assertEquals(2, nodes.size());
        Map<String, Object> tool = nodes.get(1);
        assertEquals("TOOL", tool.get("type"));
        assertEquals("queryOrder", ((Map<String, Object>) tool.get("ref")).get("name"));
        Map<String, Object> config = (Map<String, Object>) tool.get("config");
        assertEquals("order", config.get("outputAlias"));
        assertEquals("$.message.orderNo", ((Map<String, String>) config.get("inputMapping")).get("orderNo"));
    }

    @Test
    void rejectsDuplicateNodes() {
        EafGraph.Builder builder = EafGraph.agent("bad")
                .llm("node")
                .tool("node");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("duplicate"));
    }

    @Test
    void rejectsMissingLlm() {
        EafGraph.Builder builder = EafGraph.agent("bad")
                .tool("queryOrder");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("LLM"));
    }

    @Test
    void rejectsInvalidEdgeEndpoint() {
        EafGraph.Builder builder = EafGraph.agent("bad")
                .llm("llm")
                .edge("llm", "missing").always();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesModelInstanceIdToLlmRegardlessOfBuilderOrder() {
        EafAgentGraph graph = EafGraph.agent("order_assistant")
                .llm("planner")
                .modelInstanceId("llm-1")
                .build();

        Map<String, Object> spec = graph.graphSpec();
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) spec.get("nodes");
        Map<String, Object> config = (Map<String, Object>) nodes.get(0).get("config");
        assertEquals("llm-1", config.get("modelInstanceId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void edgeBuilderCanBuildWithDefaultAlwaysCondition() {
        EafAgentGraph graph = EafGraph.agent("order_assistant")
                .llm("planner")
                .tool("queryOrder")
                .edge("planner", "queryOrder")
                .build();

        Map<String, Object> spec = graph.graphSpec();
        List<Map<String, Object>> edges = (List<Map<String, Object>>) spec.get("edges");
        assertTrue(edges.stream().anyMatch(edge ->
                "planner".equals(edge.get("from"))
                        && "queryOrder".equals(edge.get("to"))
                        && "always".equals(edge.get("condition"))));
    }
}
