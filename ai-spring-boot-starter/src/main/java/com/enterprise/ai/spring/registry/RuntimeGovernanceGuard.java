package com.enterprise.ai.spring.registry;

import java.util.Objects;
import java.util.function.Supplier;

public class RuntimeGovernanceGuard {

    private final Supplier<RuntimeGovernanceState> stateSupplier;

    public RuntimeGovernanceGuard(Supplier<RuntimeGovernanceState> stateSupplier) {
        this.stateSupplier = Objects.requireNonNull(stateSupplier, "stateSupplier must not be null");
    }

    public RuntimeGovernanceState currentState() {
        RuntimeGovernanceState state = stateSupplier.get();
        return state == null ? RuntimeGovernanceState.defaultState() : state;
    }

    public boolean localExecutionAllowed() {
        return currentState().localExecutionAllowed();
    }

    public boolean embeddedExecutionAllowed() {
        return currentState().embeddedExecutionAllowed();
    }

    public boolean hybridExecutionAllowed() {
        return currentState().hybridExecutionAllowed();
    }

    public void assertLocalExecutionAllowed() {
        RuntimeGovernanceState state = currentState();
        if (!state.localExecutionAllowed()) {
            throw blocked("local", state);
        }
    }

    public void assertEmbeddedExecutionAllowed() {
        RuntimeGovernanceState state = currentState();
        if (!state.embeddedExecutionAllowed()) {
            throw blocked("embedded", state);
        }
    }

    public void assertHybridExecutionAllowed() {
        RuntimeGovernanceState state = currentState();
        if (!state.hybridExecutionAllowed()) {
            throw blocked("hybrid", state);
        }
    }

    private RuntimeGovernanceException blocked(String mode, RuntimeGovernanceState state) {
        String reason = state.message() == null || state.message().isBlank()
                ? "runtime execution blocked by control plane"
                : state.message();
        return new RuntimeGovernanceException(mode, state, reason);
    }
}
