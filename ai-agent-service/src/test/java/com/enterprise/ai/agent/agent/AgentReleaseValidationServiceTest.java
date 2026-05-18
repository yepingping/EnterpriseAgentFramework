package com.enterprise.ai.agent.agent;

import com.enterprise.ai.agent.graph.AgentGraphSpec;
import com.enterprise.ai.agent.runtime.AgentRuntimeSelector;
import com.enterprise.ai.agent.runtime.AgentRuntimeValidationResult;
import com.enterprise.ai.agent.tools.definition.ToolDefinitionEntity;
import com.enterprise.ai.agent.tools.definition.ToolDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentReleaseValidationServiceTest {

    private AgentRuntimeSelector runtimeSelector;
    private ToolDefinitionService toolDefinitionService;
    private AgentReleaseValidationService service;

    @BeforeEach
    void setUp() {
        runtimeSelector = mock(AgentRuntimeSelector.class);
        toolDefinitionService = mock(ToolDefinitionService.class);
        service = new AgentReleaseValidationService(runtimeSelector, toolDefinitionService);
        when(runtimeSelector.validate(any())).thenReturn(AgentRuntimeValidationResult.builder()
                .valid(true)
                .build());
    }

    @Test
    void validateAcceptsPublishableGraph() {
        ToolDefinitionEntity tool = new ToolDefinitionEntity();
        tool.setName("query_order");
        tool.setKind("TOOL");
        tool.setEnabled(true);
        tool.setAgentVisible(true);
        tool.setDraft(false);
        tool.setVisibility("PUBLIC");
        when(toolDefinitionService.findByName("query_order")).thenReturn(Optional.of(tool));

        AgentReleaseValidationResult result = service.validate(baseDefinition(AgentGraphSpec.builder()
                .entry("llm")
                .finishNode("tool")
                .node(AgentGraphSpec.Node.builder().id("llm").type("LLM").build())
                .node(AgentGraphSpec.Node.builder()
                        .id("tool")
                        .type("TOOL")
                        .ref(AgentGraphSpec.CapabilityRef.builder().name("query_order").build())
                        .config(Map.of("inputMapping", Map.of("orderId", "$input")))
                        .build())
                .edge(AgentGraphSpec.Edge.builder().from("llm").to("tool").condition("success").build())
                .build()));

        assertTrue(result.valid(), () -> result.errors().toString());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void validateAcceptsSdkGraphWithStartAndEndPseudoNodes() {
        AgentReleaseValidationResult result = service.validate(baseDefinition(AgentGraphSpec.builder()
                .entry("llm")
                .finishNode("llm")
                .node(AgentGraphSpec.Node.builder().id("llm").type("LLM").build())
                .edge(AgentGraphSpec.Edge.builder().from("START").to("llm").condition("always").build())
                .edge(AgentGraphSpec.Edge.builder().from("llm").to("END").condition("always").build())
                .build()));

        assertTrue(result.valid(), () -> result.errors().toString());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void validateRejectsInvalidPseudoNodeDirection() {
        AgentReleaseValidationResult result = service.validate(baseDefinition(AgentGraphSpec.builder()
                .entry("llm")
                .finishNode("llm")
                .node(AgentGraphSpec.Node.builder().id("llm").type("LLM").build())
                .edge(AgentGraphSpec.Edge.builder().from("END").to("llm").condition("always").build())
                .edge(AgentGraphSpec.Edge.builder().from("llm").to("START").condition("always").build())
                .build()));

        assertFalse(result.valid());
        List<String> codes = result.errors().stream().map(AgentReleaseValidationResult.Item::code).toList();
        assertTrue(codes.contains("GRAPH_EDGE_FROM_END"));
        assertTrue(codes.contains("GRAPH_EDGE_TO_START"));
    }

    @Test
    void validateReportsNullGraphItemsWithoutCrashing() {
        java.util.ArrayList<AgentGraphSpec.Node> nodes = new java.util.ArrayList<>();
        nodes.add(null);
        nodes.add(AgentGraphSpec.Node.builder().id("llm").type("LLM").build());
        java.util.ArrayList<AgentGraphSpec.Edge> edges = new java.util.ArrayList<>();
        edges.add(null);
        edges.add(AgentGraphSpec.Edge.builder().from("START").to("llm").condition("always").build());
        edges.add(AgentGraphSpec.Edge.builder().from("llm").to("END").condition("always").build());

        AgentReleaseValidationResult result = service.validate(baseDefinition(AgentGraphSpec.builder()
                .entry("llm")
                .finishNode("llm")
                .nodes(nodes)
                .edges(edges)
                .build()));

        assertFalse(result.valid());
        List<String> codes = result.errors().stream().map(AgentReleaseValidationResult.Item::code).toList();
        assertTrue(codes.contains("GRAPH_NODE_EMPTY_ITEM"));
        assertTrue(codes.contains("GRAPH_EDGE_EMPTY"));
    }

    @Test
    void validateRejectsUnreachableAndMissingTool() {
        when(toolDefinitionService.findByName("missing_tool")).thenReturn(Optional.empty());
        AgentReleaseValidationResult result = service.validate(baseDefinition(AgentGraphSpec.builder()
                .entry("llm")
                .finishNode("end")
                .node(AgentGraphSpec.Node.builder().id("llm").type("LLM").build())
                .node(AgentGraphSpec.Node.builder()
                        .id("tool")
                        .type("TOOL")
                        .ref(AgentGraphSpec.CapabilityRef.builder().name("missing_tool").build())
                        .build())
                .node(AgentGraphSpec.Node.builder().id("end").type("LLM").build())
                .edge(AgentGraphSpec.Edge.builder().from("llm").to("end").condition("sometimes").build())
                .build()));

        assertFalse(result.valid());
        List<String> codes = result.errors().stream().map(AgentReleaseValidationResult.Item::code).toList();
        assertTrue(codes.contains("GRAPH_REF_NOT_FOUND"));
        assertTrue(codes.contains("GRAPH_EDGE_CONDITION_UNSUPPORTED"));
        assertTrue(codes.contains("GRAPH_UNREACHABLE_NODE"));
    }

    private AgentDefinition baseDefinition(AgentGraphSpec graphSpec) {
        return AgentDefinition.builder()
                .id("agent-1")
                .keySlug("order-agent")
                .name("Order Agent")
                .enabled(true)
                .modelInstanceId("model-1")
                .runtimeType("LANGGRAPH4J")
                .runtimePlacement("CENTRAL")
                .graphSpec(graphSpec)
                .build();
    }
}
