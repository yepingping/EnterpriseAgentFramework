package com.enterprise.ai.client;

import com.enterprise.ai.common.dto.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * ai-model-service Feign Client。
 * Nacos 启用时按服务名路由，否则使用 url 属性直连。
 */
@FeignClient(
        name = "ai-model-service",
        url = "${embedding.model-service.url:}",
        path = "/model"
)
public interface ModelServiceClient {

    @PostMapping("/chat")
    ApiResult<Map<String, Object>> chat(@RequestBody Map<String, Object> request);

    @PostMapping("/embedding")
    ApiResult<EmbeddingResult> embed(@RequestBody EmbeddingParam request);

    @PostMapping("/rerank")
    ApiResult<RerankResult> rerank(@RequestBody RerankParam request);

    record EmbeddingParam(String modelInstanceId, List<String> texts) {}

    record EmbeddingResult(String model, String provider, int dimension, List<List<Float>> embeddings) {}

    record RerankParam(String modelInstanceId, String query, List<String> documents, Integer topN, Map<String, Object> options) {}

    record RerankResult(String model, String provider, List<RerankItem> results) {}

    record RerankItem(int index, float score, String document) {}
}
