package com.enterprise.ai.rag.impl;

import com.enterprise.ai.client.ModelServiceClient;
import com.enterprise.ai.common.dto.ApiResult;
import com.enterprise.ai.rag.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class ModelServiceLlmService implements LlmService {

    private final ModelServiceClient modelServiceClient;

    @Value("${rag.model-instance-id:}")
    private String modelInstanceId;

    @Override
    public String chat(String prompt) {
        if (modelInstanceId == null || modelInstanceId.isBlank()) {
            throw new IllegalStateException("rag.model-instance-id is required");
        }
        Map<String, Object> request = Map.of(
                "modelInstanceId", modelInstanceId.trim(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        ApiResult<Map<String, Object>> result = modelServiceClient.chat(request);
        if (result.getCode() != 200 || result.getData() == null) {
            throw new RuntimeException("ai-model-service chat failed: " + result.getMessage());
        }
        return (String) result.getData().get("content");
    }

    @Override
    public String getModelName() {
        return modelInstanceId;
    }
}
