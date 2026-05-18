package com.enterprise.ai.spring.registry;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeGovernanceGuardTest {

    @Test
    void allowsAllModesWhenStateAllowsExecution() {
        RuntimeGovernanceGuard guard = new RuntimeGovernanceGuard(() ->
                new RuntimeGovernanceState(false, "ONLINE", null, true, true, true, "ok"));

        guard.assertLocalExecutionAllowed();
        guard.assertEmbeddedExecutionAllowed();
        guard.assertHybridExecutionAllowed();
    }

    @Test
    void blocksLocalExecutionWhenRuntimeIsDisabled() {
        RuntimeGovernanceGuard guard = new RuntimeGovernanceGuard(() ->
                new RuntimeGovernanceState(true, "DISABLED", null, true, true, true, "disabled"));

        RuntimeGovernanceException ex = assertThrows(
                RuntimeGovernanceException.class,
                guard::assertLocalExecutionAllowed);

        assertEquals("local", ex.getExecutionMode());
        assertEquals("disabled", ex.getMessage());
    }

    @Test
    void blocksEmbeddedAndHybridIndependently() {
        AtomicReference<RuntimeGovernanceState> state = new AtomicReference<>(
                new RuntimeGovernanceState(false, "ONLINE", null, true, false, true, "embedded blocked"));
        RuntimeGovernanceGuard guard = new RuntimeGovernanceGuard(state::get);

        RuntimeGovernanceException embedded = assertThrows(
                RuntimeGovernanceException.class,
                guard::assertEmbeddedExecutionAllowed);
        assertEquals("embedded", embedded.getExecutionMode());

        state.set(new RuntimeGovernanceState(false, "ONLINE", null, true, true, false, "hybrid blocked"));
        RuntimeGovernanceException hybrid = assertThrows(
                RuntimeGovernanceException.class,
                guard::assertHybridExecutionAllowed);
        assertEquals("hybrid", hybrid.getExecutionMode());
    }
}
