package com.enterprise.ai.spring.registry;

public class RuntimeGovernanceException extends RuntimeException {

    private final String executionMode;

    private final RuntimeGovernanceState state;

    public RuntimeGovernanceException(String executionMode, RuntimeGovernanceState state, String message) {
        super(message);
        this.executionMode = executionMode;
        this.state = state;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public RuntimeGovernanceState getState() {
        return state;
    }
}
