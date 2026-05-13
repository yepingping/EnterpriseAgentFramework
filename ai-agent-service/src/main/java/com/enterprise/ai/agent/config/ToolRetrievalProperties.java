package com.enterprise.ai.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.tool-retrieval")
public class ToolRetrievalProperties {

    private boolean enabled = true;

    private int topK = 15;

    private double minScore = 0.22;

    private boolean fallbackOnError = true;

    private String collectionName = "tool_embeddings";

    private int embeddingDim = 1536;

    /**
     * 可选：运维覆盖用。默认运行时以库表 {@code tool_retrieval_setting}（「重建向量索引」所选）为准，
     * 此处仅在该表无记录或需临时切模型时使用。
     */
    private String embeddingModelInstanceId;
}
