package com.enterprise.ai.agent.runtime;

/**
 * Platform-level Agent runtime SPI.
 * <p>
 * Implementations hide concrete framework details such as AgentScope ReActAgent,
 * LangGraph graph state, or OpenAI Agents SDK handoffs from the platform core.
 */
public interface AgentRuntimeAdapter {

    String DEFAULT_RUNTIME_TYPE = "AGENTSCOPE";
    String LANGGRAPH4J_RUNTIME_TYPE = "LANGGRAPH4J";
    String OPENAI_AGENTS_RUNTIME_TYPE = "OPENAI_AGENTS";
    String CURSOR_CODE_AGENT_RUNTIME_TYPE = "CURSOR_CODE_AGENT";

    String runtimeType();

    AgentRuntimeCapability capability();

    boolean supports(AgentRuntimeRequest request);

    default String unsupportedReason(AgentRuntimeRequest request) {
        return "Agent Runtime Adapter 不支持当前请求: " + runtimeType();
    }

    AgentRuntimeResult execute(AgentRuntimeRequest request);
}
