package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.client.ModelServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AgentRuntimeModelValidator {

    private static final String ACTIVE = "ACTIVE";
    private static final int SUCCESS_CODE = 200;

    private final ModelServiceClient modelServiceClient;

    public ModelServiceClient.ModelInstanceData validate(AgentRuntimeRequest request,
                                                         AgentRuntimeCapability capability) {
        AgentDefinition definition = request == null ? null : request.getAgentDefinition();
        String modelInstanceId = definition == null ? null : definition.getModelInstanceId();
        if (modelInstanceId == null || modelInstanceId.isBlank()) {
            throw new IllegalStateException("modelInstanceId is required for runtime selection");
        }

        ModelServiceClient.ModelInstanceResult result = modelServiceClient.getModelInstance(modelInstanceId.trim());
        if (result == null || result.getCode() != SUCCESS_CODE || result.getData() == null) {
            String message = result == null ? "empty response" : result.getMessage();
            throw new IllegalStateException("模型实例查询失败: " + message);
        }
        ModelServiceClient.ModelInstanceData instance = result.getData();
        if (!ACTIVE.equalsIgnoreCase(nullToEmpty(instance.getStatus()))) {
            throw new IllegalStateException("模型实例不可用: " + modelInstanceId + "，status=" + instance.getStatus());
        }
        List<String> supported = capability == null ? null : capability.getSupportedModelTypes();
        if (supported != null && !supported.isEmpty()) {
            String modelType = normalize(instance.getModelType());
            boolean matched = supported.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(AgentRuntimeModelValidator::normalize)
                    .anyMatch(modelType::equals);
            if (!matched) {
                throw new IllegalStateException("模型实例类型不兼容: runtime="
                        + capability.getRuntimeType() + ", modelType=" + instance.getModelType());
            }
        }
        return instance;
    }

    private static String normalize(String value) {
        return nullToEmpty(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
