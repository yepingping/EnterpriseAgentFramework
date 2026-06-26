package com.enterprise.ai.agent.context.memory;

import java.util.List;

public interface RuntimeMemoryExtractor {

    List<RuntimeMemoryExtraction> extract(RuntimeMemoryExtractionRequest request);
}
