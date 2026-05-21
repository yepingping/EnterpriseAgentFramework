package com.enterprise.ai.agent.studio;

import com.enterprise.ai.agent.graph.AgentGraphSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CursorWorkflowDraftGeneratorTest {

    private final CursorWorkflowDraftGenerator generator = new CursorWorkflowDraftGenerator();

    @Test
    void generateCreatesGraphSpecUsingAvailableCapability() {
        WorkflowDraftGenerationRequest request = WorkflowDraftGenerationRequest.builder()
                .agentId("agent-1")
                .agentName("订单助手")
                .requirement("查询订单状态并回复用户")
                .projectCode("order")
                .modelInstanceId("model-1")
                .tools(List.of(WorkflowDraftResource.builder()
                        .kind("TOOL")
                        .name("query_order")
                        .qualifiedName("order:query_order")
                        .projectCode("order")
                        .description("查询订单状态")
                        .build()))
                .build();

        WorkflowDraftGenerationResult result = generator.generate(request);

        assertEquals("CURSOR_SDK", result.provider());
        assertTrue(result.placeholderNodes().isEmpty());
        assertNotNull(result.graphSpec());
        assertEquals("user_input", result.graphSpec().getEntry());
        assertTrue(result.graphSpec().getNodes().stream().anyMatch(node ->
                "USER_INPUT".equals(node.getType()) && "user_input".equals(node.getId())));
        assertTrue(result.graphSpec().getNodes().stream().anyMatch(node ->
                "TOOL".equals(node.getType())
                        && node.getRef() != null
                        && "query_order".equals(node.getRef().getName())));
        assertTrue(result.graphSpec().getEdges().stream()
                .anyMatch(edge -> "START".equals(edge.getFrom()) && "user_input".equals(edge.getTo())));
        assertTrue(result.canvasSnapshot().containsKey("nodes"));
    }

    @Test
    void generateCreatesPlaceholderWhenNoCapabilityMatches() {
        WorkflowDraftGenerationResult result = generator.generate(WorkflowDraftGenerationRequest.builder()
                .agentId("agent-1")
                .agentName("订单助手")
                .requirement("查询订单状态并回复用户")
                .projectCode("order")
                .modelInstanceId("model-1")
                .tools(List.of())
                .build());

        assertFalse(result.placeholderNodes().isEmpty());
        WorkflowDraftPlaceholder placeholder = result.placeholderNodes().get(0);
        assertEquals("placeholder_tool", placeholder.nodeId());
        AgentGraphSpec.Node placeholderNode = result.graphSpec().getNodes().stream()
                .filter(node -> placeholder.nodeId().equals(node.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.TRUE, placeholderNode.getConfig().get("needsConfiguration"));
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void generateCreatesKnowledgeRetrievalNodeUsingKnowledgeBase() {
        WorkflowDraftGenerationResult result = generator.generate(WorkflowDraftGenerationRequest.builder()
                .agentId("agent-1")
                .agentName("制度助手")
                .requirement("查询差旅制度并回复用户")
                .projectCode("hr")
                .modelInstanceId("model-1")
                .knowledgeBases(List.of(WorkflowDraftResource.builder()
                        .kind("KNOWLEDGE")
                        .name("travel_policy")
                        .projectCode("hr")
                        .description("差旅制度知识库")
                        .build()))
                .build());

        assertTrue(result.placeholderNodes().isEmpty());
        AgentGraphSpec.Node knowledge = result.graphSpec().getNodes().stream()
                .filter(node -> "KNOWLEDGE_RETRIEVAL".equals(node.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("travel_policy"), knowledge.getConfig().get("knowledgeBaseCodes"));
        assertEquals("input", knowledge.getConfig().get("query"));
    }

    @Test
    void generateCreatesIntentClassifierBranchesWhenRequirementAsksIntentRouting() {
        WorkflowDraftGenerationResult result = generator.generate(WorkflowDraftGenerationRequest.builder()
                .agentId("agent-1")
                .agentName("metro_assistant")
                .requirement("先识别用户意图，如果是关于地铁类的问题就调用大模型回答，如果与地铁不相关就直接回复不知道")
                .projectCode("metro")
                .modelInstanceId("model-1")
                .build());

        AgentGraphSpec graphSpec = result.graphSpec();
        AgentGraphSpec.Node classifier = graphSpec.getNodes().stream()
                .filter(node -> "INTENT_CLASSIFIER".equals(node.getType()))
                .findFirst()
                .orElseThrow();

        assertEquals("user_input", graphSpec.getEntry());
        assertEquals(List.of("metro_answer", "unknown_reply"), graphSpec.getFinish());
        assertEquals("params.question", classifier.getConfig().get("inputExpression"));
        assertTrue(graphSpec.getNodes().stream().anyMatch(node ->
                "USER_INPUT".equals(node.getType()) && "user_input".equals(node.getId())));
        assertEquals("irrelevant", classifier.getConfig().get("defaultRoute"));
        assertTrue(graphSpec.getNodes().stream().anyMatch(node ->
                "LLM".equals(node.getType()) && "metro_answer".equals(node.getId())));
        assertTrue(graphSpec.getNodes().stream().anyMatch(node ->
                "ANSWER".equals(node.getType()) && "unknown_reply".equals(node.getId())));
        assertTrue(graphSpec.getEdges().stream().anyMatch(edge ->
                "intent_classifier".equals(edge.getFrom())
                        && "metro_answer".equals(edge.getTo())
                        && "route:metro_question".equals(edge.getCondition())));
        assertTrue(graphSpec.getEdges().stream().anyMatch(edge ->
                "intent_classifier".equals(edge.getFrom())
                        && "unknown_reply".equals(edge.getTo())
                        && "route:irrelevant".equals(edge.getCondition())));
    }

    @Test
    void generateCreatesIntentClassifierWhenRequirementExplicitlySaysIntentClassification() {
        WorkflowDraftGenerationResult result = generator.generate(WorkflowDraftGenerationRequest.builder()
                .agentId("agent-1")
                .agentName("metro_assistant")
                .requirement("帮我生成一个流程，用户输入问题，使用意图分类，走不同的分支，如果用户问的问题与地铁相关，就调用大模型回答，如果不相关，就回复说我不知道")
                .projectCode("metro")
                .modelInstanceId("model-1")
                .build());

        AgentGraphSpec graphSpec = result.graphSpec();

        assertTrue(graphSpec.getNodes().stream().anyMatch(node ->
                "INTENT_CLASSIFIER".equals(node.getType()) && "intent_classifier".equals(node.getId())));
        assertTrue(graphSpec.getEdges().stream().anyMatch(edge ->
                "intent_classifier".equals(edge.getFrom())
                        && "metro_answer".equals(edge.getTo())
                        && "route:metro_question".equals(edge.getCondition())));
        assertTrue(graphSpec.getEdges().stream().anyMatch(edge ->
                "intent_classifier".equals(edge.getFrom())
                        && "unknown_reply".equals(edge.getTo())
                        && "route:irrelevant".equals(edge.getCondition())));
    }
}
