package com.enterprise.ai.spring.registry;

public record RuntimeGovernanceState(
        boolean disabled,
        String status,
        String minSdkVersion,
        boolean sdkVersionSatisfied,
        boolean allowEmbeddedExecution,
        boolean allowHybridExecution,
        String message
) {

    public static RuntimeGovernanceState defaultState() {
        return new RuntimeGovernanceState(false, "UNKNOWN", null, true, true, true, "registry policy not loaded");
    }

    public boolean localExecutionAllowed() {
        return !disabled && sdkVersionSatisfied;
    }

    public boolean embeddedExecutionAllowed() {
        return localExecutionAllowed() && allowEmbeddedExecution;
    }

    public boolean hybridExecutionAllowed() {
        return localExecutionAllowed() && allowHybridExecution;
    }
}
