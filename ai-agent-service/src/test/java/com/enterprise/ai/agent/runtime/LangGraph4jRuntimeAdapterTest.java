package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.client.ModelServiceClient;
import com.enterprise.ai.agent.tool.log.ToolCallLogService;
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
        LangGraph4jRuntimeAdapter adapter = new LangGraph4jRuntimeAdapter(successModelClient(), logService);

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
                        .build())
                .build());

        assertTrue(result.isSuccess());
        assertEquals("langgraph answer", result.getAnswer());
        assertEquals(AgentRuntimeAdapter.LANGGRAPH4J_RUNTIME_TYPE, result.getRuntimeType());
        assertEquals("LangGraph Agent", result.getAgentName());
        assertEquals("qwen-plus", result.getMetadata().get("model"));
        assertEquals("tongyi", result.getMetadata().get("provider"));
        assertEquals(3, result.getSteps().size());
        assertEquals(7, result.getTokenUsage().get("totalTokens"));

        verify(logService).record(any(), eq("runtime.langgraph4j.node.llm"),
                any(), any(), eq(true), eq(null), any(Long.class), eq(7));
        verify(logService).record(any(), eq("runtime.agent.run"),
                any(), any(), eq(true), eq(null), any(Long.class), eq(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void executesConfiguredLlmNodeFromRuntimeConfig() {
        ToolCallLogService logService = mock(ToolCallLogService.class);
        LangGraph4jRuntimeAdapter adapter = new LangGraph4jRuntimeAdapter(successModelClient(), logService);

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
                        .runtimeConfig(Map.of("langGraph4j", Map.of(
                                "graphMode", "single-llm",
                                "nodes", List.of(Map.of("id", "planner", "type", "llm", "label", "Planner")),
                                "edges", List.of(
                                        Map.of("from", "START", "to", "planner"),
                                        Map.of("from", "planner", "to", "END")))))
                        .build())
                .build());

        assertTrue(result.isSuccess());
        assertTrue(((List<String>) result.getMetadata().get("graphNodes")).contains("planner"));
        assertTrue(result.getSteps().contains("LangGraph4j 节点: planner"));
        verify(logService).record(any(), eq("runtime.langgraph4j.node.planner"),
                any(), any(), eq(true), eq(null), any(Long.class), eq(7));
    }

    @Test
    void onlySupportsSingleAgentWithoutToolsForMinimumRuntime() {
        LangGraph4jRuntimeAdapter adapter = new LangGraph4jRuntimeAdapter(successModelClient(), null);

        assertTrue(adapter.supports(request(AgentDefinition.builder()
                .type("single")
                .modelInstanceId("llm-1")
                .build())));
        assertFalse(adapter.supports(request(AgentDefinition.builder()
                .type("pipeline")
                .modelInstanceId("llm-1")
                .build())));
        assertFalse(adapter.supports(request(AgentDefinition.builder()
                .type("single")
                .modelInstanceId("llm-1")
                .tools(List.of("query_team"))
                .build())));
        assertFalse(adapter.supports(request(AgentDefinition.builder()
                .type("single")
                .modelInstanceId("llm-1")
                .runtimeConfig(Map.of("langGraph4j", Map.of(
                        "nodes", List.of(Map.of("id", "not-llm", "type", "tool")))))
                .build())));
    }

    private AgentRuntimeRequest request(AgentDefinition definition) {
        return AgentRuntimeRequest.builder()
                .traceId("trace")
                .message("hello")
                .agentDefinition(definition)
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
