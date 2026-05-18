package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.client.ModelServiceClient;
import com.enterprise.ai.agent.graph.AgentGraphSpec;
import com.enterprise.ai.agent.tool.log.ToolCallLogService;
import com.enterprise.ai.agent.tools.definition.ToolDefinitionService;
import com.enterprise.ai.agent.trace.AgentTraceSpanService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LangGraph4jRuntimeAdapterTest {

    @Test
    void executesSingleAgentThroughLangGraph4j() {
        ToolCallLogService logService = mock(ToolCallLogService.class);
        LangGraph4jRuntimeAdapter adapter = adapter(logService);

        AgentRuntimeResult result = adapter.execute(AgentRuntimeRequest.builder()
                .traceId("trace-lg")
                .sessionId("session-lg")
                .userId("user-lg")
                .message("hello")
                .intentType("GENERAL_CHAT")
                .agentDefinition(AgentDefinition.builder()
                        .id("agent-1")
                        .name("LangGraph Agent")
                        .type("single")
                        .runtimeType(AgentRuntimeAdapter.LANGGRAPH4J_RUNTIME_TYPE)
                        .modelInstanceId("llm-1")
                        .systemPrompt("You are helpful.")
                        .graphSpec(singleLlmGraph("llm"))
                        .build())
                .build());

        assertTrue(result.isSuccess());
        assertEquals("langgraph answer", result.getAnswer());
        assertEquals(AgentRuntimeAdapter.LANGGRAPH4J_RUNTIME_TYPE, result.getRuntimeType());
        assertEquals("LangGraph Agent", result.getAgentName());
        assertEquals("qwen-plus", result.getMetadata().get("model"));
        assertEquals("tongyi", result.getMetadata().get("provider"));
        assertEquals(2, result.getSteps().size());
        assertEquals(7, result.getTokenUsage().get("totalTokens"));

        verify(logService).record(any(), eq("runtime.langgraph4j.node.llm"),
                any(), any(), eq(true), eq(null), any(Long.class), eq(7));
        verify(logService).record(any(), eq("runtime.agent.run"),
                any(), any(), eq(true), eq(null), any(Long.class), eq(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void executesConfiguredLlmNodeFromGraphSpec() {
        ToolCallLogService logService = mock(ToolCallLogService.class);
        LangGraph4jRuntimeAdapter adapter = adapter(logService);

        AgentRuntimeResult result = adapter.execute(AgentRuntimeRequest.builder()
                .traceId("trace-lg")
                .sessionId("session-lg")
                .message("hello")
                .agentDefinition(AgentDefinition.builder()
                        .id("agent-1")
                        .name("LangGraph Agent")
                        .type("single")
                        .runtimeType(AgentRuntimeAdapter.LANGGRAPH4J_RUNTIME_TYPE)
                        .modelInstanceId("llm-1")
                        .systemPrompt("You are helpful.")
                        .graphSpec(singleLlmGraph("planner"))
                        .build())
                .build());

        assertTrue(result.isSuccess());
        assertTrue(((List<String>) result.getMetadata().get("graphNodes")).contains("planner"));
        assertTrue(result.getSteps().contains("Graph node: planner (LLM)"));
        verify(logService).record(any(), eq("runtime.langgraph4j.node.planner"),
                any(), any(), eq(true), eq(null), any(Long.class), eq(7));
    }

    @Test
    void onlySupportsSingleAgentWithoutToolsForMinimumRuntime() {
        LangGraph4jRuntimeAdapter adapter = adapter(null);

        assertTrue(adapter.supports(request(AgentDefinition.builder()
                .type("single")
                .modelInstanceId("llm-1")
                .graphSpec(singleLlmGraph("llm"))
                .build())));
        assertFalse(adapter.supports(request(AgentDefinition.builder()
                .type("pipeline")
                .modelInstanceId("llm-1")
                .graphSpec(singleLlmGraph("llm"))
                .build())));
        assertTrue(adapter.supports(request(AgentDefinition.builder()
                .type("single")
                .modelInstanceId("llm-1")
                .tools(List.of("query_team"))
                .graphSpec(singleLlmGraph("llm"))
                .build())));
        assertFalse(adapter.supports(request(AgentDefinition.builder()
                .type("single")
                .modelInstanceId("llm-1")
                .graphSpec(AgentGraphSpec.builder()
                        .entry("not-llm")
                        .node(AgentGraphSpec.Node.builder().id("not-llm").type("TOOL").build())
                        .build())
                .build())));
    }

    private AgentRuntimeRequest request(AgentDefinition definition) {
        return AgentRuntimeRequest.builder()
                .traceId("trace")
                .message("hello")
                .agentDefinition(definition)
                .build();
    }

    private LangGraph4jRuntimeAdapter adapter(ToolCallLogService logService) {
        return new LangGraph4jRuntimeAdapter(
                successModelClient(),
                mock(ToolDefinitionService.class),
                logService,
                mock(AgentTraceSpanService.class));
    }

    private AgentGraphSpec singleLlmGraph(String nodeId) {
        return AgentGraphSpec.builder()
                .code("test_graph")
                .name("Test Graph")
                .runtimeHint(AgentRuntimeAdapter.LANGGRAPH4J_RUNTIME_TYPE)
                .entry(nodeId)
                .finishNode(nodeId)
                .node(AgentGraphSpec.Node.builder()
                        .id(nodeId)
                        .type("LLM")
                        .name(nodeId)
                        .config(Map.of("modelInstanceId", "llm-1"))
                        .build())
                .edge(AgentGraphSpec.Edge.builder().from("START").to(nodeId).condition("always").build())
                .edge(AgentGraphSpec.Edge.builder().from(nodeId).to("END").condition("always").build())
                .build();
    }

    private ModelServiceClient successModelClient() {
        return new ModelServiceClient() {
            @Override
            public ModelChatResult chat(ModelChatRequest request) {
                return new ModelChatResult(200, "ok", new ModelChatData(
                        "langgraph answer",
                        "qwen-plus",
                        "tongyi",
                        new ModelUsage(3, 4, 7),
                        null,
                        null,
                        "stop"));
            }

            @Override
            public ModelEmbeddingResult embed(ModelEmbeddingRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModelInstanceResult getModelInstance(String id) {
                return new ModelInstanceResult(200, "ok", new ModelInstanceData(
                        id,
                        "LLM",
                        "tongyi",
                        "LLM",
                        "qwen-plus",
                        null,
                        null,
                        Map.of(),
                        null,
                        "ACTIVE",
                        null));
            }
        };
    }
}
