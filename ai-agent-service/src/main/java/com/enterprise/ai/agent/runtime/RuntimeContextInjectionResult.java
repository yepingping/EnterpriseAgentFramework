package com.enterprise.ai.agent.runtime;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RuntimeContextInjectionResult {

    private boolean enabled;
    private String skippedReason;
    private RuntimeContextIdentity identity;
    private String promptSection;
    private int itemCount;
    private int truncatedCount;

    @Singular("hit")
    private List<Map<String, Object>> hitSummaries;
}
