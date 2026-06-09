package com.enterprise.ai.reach.sdk.auth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReachAiInvocationTokenTest {

    @Test
    void signsAndVerifiesDelegatedBusinessInvocation() {
        ReachAiInvocationClaims claims = ReachAiInvocationClaims.builder()
                .projectCode("bzsdk")
                .appKey("demo-key")
                .capabilityName("contract.query")
                .externalUserId("ADMIN001")
                .globalUserId("emp-0001")
                .roles(Arrays.asList("admin", "auditor"))
                .agentId("team-agent")
                .sessionId("session-1")
                .traceId("trace-1")
                .pageInstanceId("page-1")
                .build();

        String token = ReachAiInvocationToken.sign("secret", claims, 1_700_000_000_000L, 120);
        ReachAiInvocationClaims verified = ReachAiInvocationToken.verify(
                "secret", token, "bzsdk", "contract.query", 1_700_000_030_000L);

        assertEquals("bzsdk", verified.getProjectCode());
        assertEquals("demo-key", verified.getAppKey());
        assertEquals("contract.query", verified.getCapabilityName());
        assertEquals("ADMIN001", verified.getExternalUserId());
        assertEquals("emp-0001", verified.getGlobalUserId());
        assertEquals(Arrays.asList("admin", "auditor"), verified.getRoles());
        assertEquals("team-agent", verified.getAgentId());
        assertEquals("session-1", verified.getSessionId());
        assertEquals("trace-1", verified.getTraceId());
        assertEquals("page-1", verified.getPageInstanceId());
    }

    @Test
    void rejectsCapabilityNameMismatch() {
        ReachAiInvocationClaims claims = ReachAiInvocationClaims.builder()
                .projectCode("bzsdk")
                .appKey("demo-key")
                .capabilityName("contract.query")
                .externalUserId("ADMIN001")
                .roles(Collections.singletonList("admin"))
                .build();

        String token = ReachAiInvocationToken.sign("secret", claims, 1_700_000_000_000L, 120);

        assertThrows(IllegalArgumentException.class, () -> ReachAiInvocationToken.verify(
                "secret", token, "bzsdk", "contract.delete", 1_700_000_030_000L));
    }
}
