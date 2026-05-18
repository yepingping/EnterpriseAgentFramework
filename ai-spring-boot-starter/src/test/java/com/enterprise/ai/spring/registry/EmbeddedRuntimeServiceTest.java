package com.enterprise.ai.spring.registry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddedRuntimeServiceTest {

    @Test
    void executesWithFirstSupportedExecutorWhenGovernanceAllows() {
        RuntimeGovernanceGuard guard = new RuntimeGovernanceGuard(() ->
                new RuntimeGovernanceState(false, "ONLINE", null, true, true, true, "ok"));
        EmbeddedRuntimeExecutor unsupported = new TestExecutor("other", "no");
        EmbeddedRuntimeExecutor supported = new TestExecutor("agent-a", "done");
        EmbeddedRuntimeService service = new EmbeddedRuntimeService(guard, List.of(unsupported, supported));

        EmbeddedRuntimeResult result = service.execute(request("agent-a"));

        assertEquals(true, result.success());
        assertEquals("done", result.answer());
    }

    @Test
    void blocksBeforeExecutorSelectionWhenGovernanceDisallowsEmbeddedRuntime() {
        RuntimeGovernanceGuard guard = new RuntimeGovernanceGuard(() ->
                new RuntimeGovernanceState(false, "ONLINE", null, true, false, true, "embedded disabled"));
        EmbeddedRuntimeService service = new EmbeddedRuntimeService(guard, List.of(new TestExecutor("agent-a", "done")));

        RuntimeGovernanceException ex = assertThrows(
                RuntimeGovernanceException.class,
                () -> service.execute(request("agent-a")));

        assertEquals("embedded", ex.getExecutionMode());
        assertEquals("embedded disabled", ex.getMessage());
    }

    @Test
    void failsClearlyWhenNoExecutorSupportsRequest() {
        RuntimeGovernanceGuard guard = new RuntimeGovernanceGuard(() ->
                new RuntimeGovernanceState(false, "ONLINE", null, true, true, true, "ok"));
        EmbeddedRuntimeService service = new EmbeddedRuntimeService(guard, List.of(new TestExecutor("agent-b", "done")));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.execute(request("agent-a")));

        assertEquals("没有可用的 EmbeddedRuntimeExecutor: agent-a", ex.getMessage());
    }

    private EmbeddedRuntimeRequest request(String agentKey) {
        return new EmbeddedRuntimeRequest(agentKey, "hello", "s1", "u1", Map.of(), Map.of());
    }

    private record TestExecutor(String agentKey, String answer) implements EmbeddedRuntimeExecutor {
        @Override
        public boolean supports(EmbeddedRuntimeRequest request) {
            return request != null && agentKey.equals(request.agentKey());
        }

        @Override
        public EmbeddedRuntimeResult execute(EmbeddedRuntimeRequest request) {
            return EmbeddedRuntimeResult.success(answer);
        }
    }
}
