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

    private String embeddingModelInstanceId;
}
