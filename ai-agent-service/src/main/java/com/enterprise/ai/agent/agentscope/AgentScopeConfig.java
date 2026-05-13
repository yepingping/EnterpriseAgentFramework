package com.enterprise.ai.agent.agentscope;

import com.enterprise.ai.agent.config.ToolCallLogProperties;
import com.enterprise.ai.agent.tool.log.ToolCallLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.formatter.openai.OpenAIMultiAgentFormatter;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AgentScopeConfig {

    @Value("${services.model-service.url:http://localhost:18601}")
    private String modelServiceUrl;

    private final ToolCallLogService toolCallLogService;
    private final ToolCallLogProperties toolCallLogProperties;
    private final ObjectMapper objectMapper;

    public AgentScopeConfig(ToolCallLogService toolCallLogService,
                            ToolCallLogProperties toolCallLogProperties,
                            ObjectMapper objectMapper) {
        this.toolCallLogService = toolCallLogService;
        this.toolCallLogProperties = toolCallLogProperties;
        this.objectMapper = objectMapper;
    }

    public Model createChatModel(String modelInstanceId) {
        String required = requireModelInstanceId(modelInstanceId);
        String baseUrl = modelServiceUrl + "/model/openai-proxy/v1";
        log.debug("[AgentScope] create chat model: baseUrl={}, modelInstanceId={}", baseUrl, required);
        Model inner = OpenAIChatModel.builder()
                .apiKey("proxy-via-model-service")
                .baseUrl(baseUrl)
                .modelName(required)
                .build();
        return new TracingModel(inner, toolCallLogService, toolCallLogProperties, objectMapper);
    }

    public Model createMultiAgentModel(String modelInstanceId) {
        String required = requireModelInstanceId(modelInstanceId);
        String baseUrl = modelServiceUrl + "/model/openai-proxy/v1";
        log.debug("[AgentScope] create multi-agent model: baseUrl={}, modelInstanceId={}", baseUrl, required);
        Model inner = OpenAIChatModel.builder()
                .apiKey("proxy-via-model-service")
                .baseUrl(baseUrl)
                .modelName(required)
                .formatter(new OpenAIMultiAgentFormatter())
                .build();
        return new TracingModel(inner, toolCallLogService, toolCallLogProperties, objectMapper);
    }

    private String requireModelInstanceId(String modelInstanceId) {
        if (modelInstanceId == null || modelInstanceId.isBlank()) {
            throw new IllegalStateException("modelInstanceId is required for agent execution");
        }
        return modelInstanceId.trim();
    }
}
