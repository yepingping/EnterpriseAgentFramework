package com.enterprise.ai.reach.sdk.auth;

import com.enterprise.ai.reach.sdk.client.ReachAiClientConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReachAiSignerTest {

    @Test
    void signsRegistryRequestsWithReachAiHeaders() {
        ReachAiClientConfig config = ReachAiClientConfig.builder()
                .endpoint("https://reachai.example.com/")
                .projectCode("demo-project")
                .projectName("Demo Project")
                .appKey("demo-key")
                .appSecret("secret")
                .build();

        ReachAiSignatureHeaders headers = ReachAiSigner.sign(config, "1700000000000", "nonce-1");
        Map<String, String> httpHeaders = headers.toHttpHeaders();

        assertEquals("demo-key", headers.getAppKey());
        assertEquals("1700000000000", headers.getTimestamp());
        assertEquals("nonce-1", headers.getNonce());
        assertEquals("2f2c051571df34fa32c04e07c7299d938bdae3fc9a10ae961c87d5b1d3b5043c", headers.getSignature());
        assertEquals("demo-key", httpHeaders.get("X-ReachAI-App-Key"));
        assertEquals("2f2c051571df34fa32c04e07c7299d938bdae3fc9a10ae961c87d5b1d3b5043c",
                httpHeaders.get("X-ReachAI-Signature"));
    }

    @Test
    void normalizesEndpointAndRequiresProjectIdentity() {
        ReachAiClientConfig config = ReachAiClientConfig.builder()
                .endpoint("https://reachai.example.com/")
                .projectCode("demo-project")
                .appKey("demo-key")
                .appSecret("secret")
                .build();

        assertEquals("https://reachai.example.com", config.getEndpoint());
        assertEquals("demo-project", config.getProjectCode());

        assertThrows(IllegalArgumentException.class, () -> ReachAiClientConfig.builder()
                .endpoint("https://reachai.example.com")
                .appKey("demo-key")
                .appSecret("secret")
                .build());
    }
}
