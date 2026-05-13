package com.enterprise.ai.agent.llm;

import com.enterprise.ai.agent.client.ModelServiceClient;
import com.enterprise.ai.agent.client.ModelServiceClient.ModelChatRequest;
import com.enterprise.ai.agent.client.ModelServiceClient.ModelChatResult;
import com.enterprise.ai.agent.client.ModelStreamClient;
import com.enterprise.ai.agent.memory.MemoryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ModelServiceClient modelServiceClient;
    private final ModelStreamClient modelStreamClient;

    public static final String DEFAULT_SYSTEM_PROMPT =
            "你是企业级智能助手。请用专业且友好的语气回答用户问题。";

    public String chat(String userMessage) {
        throw explicitModelRequired();
    }

    public String chat(String systemPrompt, String userMessage) {
        throw explicitModelRequired();
    }

    public String chat(String systemPrompt, String userMessage, String modelInstanceId) {
        return callModelService(systemPrompt, userMessage, null, modelInstanceId);
    }

    public String chatWithHistory(String systemPrompt, List<MemoryMessage> history, String userMessage) {
        throw explicitModelRequired();
    }

    public String chatWithHistory(String systemPrompt, List<MemoryMessage> history,
                                  String userMessage, String modelInstanceId) {
        return callModelService(systemPrompt, userMessage, history, modelInstanceId);
    }

    public String chatWithContext(String systemPrompt, String context, String userMessage) {
        throw explicitModelRequired();
    }

    public String chatWithContext(String systemPrompt, String context, String userMessage, String modelInstanceId) {
        String combined = "Background:\n" + context + "\n\nUser question:\n" + userMessage;
        return callModelService(systemPrompt, combined, null, modelInstanceId);
    }

    public Flux<String> chatStreamWithHistory(String systemPrompt, List<MemoryMessage> history, String userMessage) {
        return Flux.error(explicitModelRequired());
    }

    public Flux<String> chatStreamWithHistory(String systemPrompt, List<MemoryMessage> history,
                                              String userMessage, String modelInstanceId) {
        return modelStreamClient.chatStream(buildRequest(systemPrompt, history, userMessage, modelInstanceId));
    }

    public Flux<String> chatStream(String userMessage) {
        return Flux.error(explicitModelRequired());
    }

    public Flux<String> chatStream(String userMessage, String modelInstanceId) {
        return modelStreamClient.chatStream(buildRequest(DEFAULT_SYSTEM_PROMPT, null, userMessage, modelInstanceId));
    }

    private String callModelService(String systemPrompt, String userMessage,
                                    List<MemoryMessage> history, String modelInstanceId) {
        ModelChatRequest request = buildRequest(systemPrompt, history, userMessage, modelInstanceId);
        try {
            ModelChatResult result = modelServiceClient.chat(request);
            if (result.getData() != null && result.getData().getContent() != null) {
                return result.getData().getContent();
            }
            log.warn("[LlmService] model-service returned empty content: code={}, msg={}",
                    result.getCode(), result.getMessage());
            return "模型服务返回为空";
        } catch (Exception e) {
            log.error("[LlmService] model-service call failed", e);
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    private ModelChatRequest buildRequest(String systemPrompt, List<MemoryMessage> history,
                                          String userMessage, String modelInstanceId) {
        List<ModelChatRequest.ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ModelChatRequest.ChatMessage.builder().role("system").content(systemPrompt).build());
        }
        if (history != null) {
            for (MemoryMessage msg : history) {
                messages.add(ModelChatRequest.ChatMessage.builder()
                        .role(msg.getRole()).content(msg.getContent()).build());
            }
        }
        messages.add(ModelChatRequest.ChatMessage.builder().role("user").content(userMessage).build());

        return ModelChatRequest.builder()
                .modelInstanceId(requireModelInstanceId(modelInstanceId))
                .messages(messages)
                .build();
    }

    private String requireModelInstanceId(String modelInstanceId) {
        if (modelInstanceId == null || modelInstanceId.isBlank()) {
            throw explicitModelRequired();
        }
        return modelInstanceId.trim();
    }

    private IllegalStateException explicitModelRequired() {
        return new IllegalStateException("modelInstanceId is required");
    }
}
