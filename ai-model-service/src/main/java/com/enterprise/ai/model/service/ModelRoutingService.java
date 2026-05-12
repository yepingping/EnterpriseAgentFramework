package com.enterprise.ai.model.service;

import com.enterprise.ai.common.exception.BizException;
import com.enterprise.ai.model.instance.EndpointType;
import com.enterprise.ai.model.instance.ModelInstanceEntity;
import com.enterprise.ai.model.instance.ModelInstanceRuntime;
import com.enterprise.ai.model.instance.ModelInstanceService;
import com.enterprise.ai.model.provider.ModelProvider;
import com.enterprise.ai.model.provider.tongyi.TongyiEmbeddingProvider;
import com.enterprise.ai.model.runtime.OpenAiCompatibleRuntimeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ModelRoutingService {

    private final Map<String, ModelProvider> providerMap = new HashMap<>();
    private final TongyiEmbeddingProvider tongyiEmbeddingProvider;
    private final ModelInstanceService modelInstanceService;
    private final OpenAiCompatibleRuntimeClient openAiCompatibleRuntimeClient;

    public ModelRoutingService(List<ModelProvider> providers,
                               TongyiEmbeddingProvider tongyiEmbeddingProvider,
                               ModelInstanceService modelInstanceService,
                               OpenAiCompatibleRuntimeClient openAiCompatibleRuntimeClient) {
        this.tongyiEmbeddingProvider = tongyiEmbeddingProvider;
        this.modelInstanceService = modelInstanceService;
        this.openAiCompatibleRuntimeClient = openAiCompatibleRuntimeClient;
        for (ModelProvider p : providers) {
            providerMap.put(p.getName(), p);
            log.info("Registered model provider: {}", p.getName());
        }
    }

    public ChatResponse chat(ChatRequest request) {
        ModelInstanceRuntime runtime = resolveRuntime(request.getModelInstanceId());
        if (EndpointType.OPENAI_COMPATIBLE.name().equals(runtime.getEndpointType())) {
            return openAiCompatibleRuntimeClient.chat(runtime, request);
        }
        applyRuntime(request, runtime);
        ModelProvider provider = resolveProvider(request.getProvider());
        log.debug("Chat routed to provider: {}", provider.getName());
        return provider.chat(request);
    }

    public Flux<String> chatStream(ChatRequest request) {
        ModelInstanceRuntime runtime = resolveRuntime(request.getModelInstanceId());
        if (EndpointType.OPENAI_COMPATIBLE.name().equals(runtime.getEndpointType())) {
            return openAiCompatibleRuntimeClient.chatStream(runtime, request);
        }
        applyRuntime(request, runtime);
        ModelProvider provider = resolveProvider(request.getProvider());
        log.debug("Chat stream routed to provider: {}", provider.getName());
        return provider.chatStream(request);
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        ModelInstanceRuntime runtime = resolveRuntime(request.getModelInstanceId());
        if (EndpointType.OPENAI_COMPATIBLE.name().equals(runtime.getEndpointType())) {
            return openAiCompatibleRuntimeClient.embed(runtime, request.getTexts());
        }
        request.setProvider(runtime.getProvider());
        request.setModel(runtime.getModelName());
        log.debug("Embedding routed to provider: {}", runtime.getProvider());
        if ("tongyi".equals(runtime.getProvider())) {
            return tongyiEmbeddingProvider.embed(request.getTexts(), request.getModel());
        }
        ModelProvider provider = resolveProvider(runtime.getProvider());
        return provider.embed(request.getTexts(), request.getModel());
    }

    public RerankResponse rerank(RerankRequest request) {
        ModelInstanceRuntime runtime = resolveRuntime(request.getModelInstanceId());
        if (!EndpointType.OPENAI_COMPATIBLE.name().equals(runtime.getEndpointType())) {
            throw new BizException(400, "Reranker currently requires OPENAI_COMPATIBLE model instance");
        }
        return openAiCompatibleRuntimeClient.rerank(runtime, request);
    }

    public List<ProviderInfo> listProviders() {
        return providerMap.values().stream()
                .map(p -> new ProviderInfo(p.getName(), p.listModels()))
                .toList();
    }

    public boolean testProvider(String providerName) {
        ModelProvider provider = resolveProvider(providerName);
        return provider.test();
    }

    private ModelProvider resolveProvider(String name) {
        String key = name != null ? name.trim() : "";
        if (key.isBlank()) {
            throw new BizException(400, "provider is required after resolving modelInstanceId");
        }
        ModelProvider provider = providerMap.get(key);
        if (provider == null) {
            throw new BizException(400, "Unknown model provider: " + key + ", available: " + providerMap.keySet());
        }
        return provider;
    }

    private ModelInstanceRuntime resolveRuntime(String modelInstanceId) {
        if (modelInstanceId == null || modelInstanceId.isBlank()) {
            throw new BizException(400, "modelInstanceId is required");
        }
        ModelInstanceEntity entity = modelInstanceService.getActiveEntity(modelInstanceId);
        return modelInstanceService.toRuntime(entity);
    }

    private void applyRuntime(ChatRequest request, ModelInstanceRuntime runtime) {
        request.setProvider(runtime.getProvider());
        request.setModel(runtime.getModelName());
        request.setOptions(mergeOptions(runtime.getDefaultOptions(), request.getOptions()));
    }

    private Map<String, Object> mergeOptions(Map<String, Object> defaults, Map<String, Object> overrides) {
        Map<String, Object> merged = new HashMap<>();
        if (defaults != null) {
            merged.putAll(defaults);
        }
        if (overrides != null) {
            merged.putAll(overrides);
        }
        return merged;
    }

    public record ProviderInfo(String name, List<String> models) {}
}
