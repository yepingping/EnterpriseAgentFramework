package com.enterprise.ai.agent.runtime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RuntimeRegistryEntry(
        String id,
        String source,
        String runtimeType,
        String displayName,
        String description,
        String runtimePlacement,
        String status,
        boolean available,
        String unavailableReason,
        boolean supportsGraph,
        boolean supportsTools,
        boolean supportsAutonomous,
        boolean supportsWorkflow,
        boolean supportsEmbeddedExecution,
        boolean supportsHybridExecution,
        String projectCode,
        String instanceId,
        String baseUrl,
        String host,
        Integer port,
        String appVersion,
        String sdkVersion,
        LocalDateTime lastHeartbeatAt,
        boolean policyDisabled,
        String minSdkVersion,
        Boolean allowEmbeddedExecution,
        Boolean allowHybridExecution,
        String policyMessage,
        List<String> runtimeTypes,
        Map<String, Object> metadata
) {
}
