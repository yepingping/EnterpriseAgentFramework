package com.enterprise.ai.spring.registry;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EafRegistryClientTest {

    @Test
    void heartbeatBodyReportsEmbeddedRuntimeMetadata() {
        EafRegistryProperties properties = new EafRegistryProperties();
        properties.getRegistry().setUrl("http://localhost:18603");
        properties.getProject().setCode("order-service");
        properties.getProject().setBaseUrl("http://localhost:8080");

        EafCapabilityScanner scanner = mock(EafCapabilityScanner.class);
        when(scanner.scan()).thenReturn(List.of());

        EafRegistryClient client = new EafRegistryClient(
                properties,
                scanner,
                new SdkDescriptionSourceSettingsHolder());

        Map<String, Object> body = client.buildHeartbeatBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");

        assertEquals("EMBEDDED", metadata.get("runtimePlacement"));
        assertEquals(List.of("SPRING_BOOT_EMBEDDED"), metadata.get("runtimeTypes"));
        assertEquals(true, metadata.get("supportsEmbeddedExecution"));
        assertEquals(false, metadata.get("supportsGraph"));
        assertTrue(body.get("instanceId").toString().length() > 0);
    }

    @Test
    void instanceIdStaysStableAcrossRestartsWhenAppNameAndPortMatch() {
        EafRegistryProperties properties = new EafRegistryProperties();
        properties.getRegistry().setUrl("http://localhost:18603");
        properties.getProject().setCode("order-service");
        properties.getProject().setBaseUrl("http://localhost:8080");

        EafCapabilityScanner scanner = mock(EafCapabilityScanner.class);
        when(scanner.scan()).thenReturn(List.of());

        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "order-service")
                .withProperty("server.port", "8611");

        EafRegistryClient first = new EafRegistryClient(properties, scanner,
                new EafAgentGraphScanner(List.of()),
                new SdkDescriptionSourceSettingsHolder(), env);
        EafRegistryClient second = new EafRegistryClient(properties, scanner,
                new EafAgentGraphScanner(List.of()),
                new SdkDescriptionSourceSettingsHolder(), env);

        assertEquals(first.getInstanceId(), second.getInstanceId(),
                "同一台机器、同一应用、同一端口重启后 instanceId 必须保持稳定");
        assertTrue(first.getInstanceId().contains("order-service"));
        assertTrue(first.getInstanceId().contains("8611"));
    }

    @Test
    void explicitInstanceIdOverridesAutoGeneration() {
        EafRegistryProperties properties = new EafRegistryProperties();
        properties.getRegistry().setUrl("http://localhost:18603");
        properties.getProject().setCode("order-service");
        properties.getProject().setBaseUrl("http://localhost:8080");
        properties.getProject().setInstanceId("order-service-node-a");

        EafCapabilityScanner scanner = mock(EafCapabilityScanner.class);
        when(scanner.scan()).thenReturn(List.of());

        EafRegistryClient client = new EafRegistryClient(properties, scanner,
                new EafAgentGraphScanner(List.of()),
                new SdkDescriptionSourceSettingsHolder(),
                new MockEnvironment().withProperty("server.port", "9999"));

        assertEquals("order-service-node-a", client.getInstanceId());
        assertEquals("order-service-node-a", client.buildHeartbeatBody().get("instanceId"));
    }

    @Test
    void heartbeatBodyIncludesPortAndAppVersionWhenAvailable() {
        EafRegistryProperties properties = new EafRegistryProperties();
        properties.getRegistry().setUrl("http://localhost:18603");
        properties.getProject().setCode("order-service");
        properties.getProject().setBaseUrl("http://localhost:8080");

        EafCapabilityScanner scanner = mock(EafCapabilityScanner.class);
        when(scanner.scan()).thenReturn(List.of());

        MockEnvironment env = new MockEnvironment()
                .withProperty("server.port", "8611")
                .withProperty("eaf.project.app-version", "1.4.2");

        EafRegistryClient client = new EafRegistryClient(properties, scanner,
                new EafAgentGraphScanner(List.of()),
                new SdkDescriptionSourceSettingsHolder(), env);

        Map<String, Object> body = client.buildHeartbeatBody();
        assertEquals(8611, body.get("port"));
        assertEquals("1.4.2", body.get("appVersion"));
    }

    @Test
    void instanceIdDiffersWhenPortDiffers() {
        EafRegistryProperties properties = new EafRegistryProperties();
        properties.getRegistry().setUrl("http://localhost:18603");
        properties.getProject().setCode("order-service");
        properties.getProject().setBaseUrl("http://localhost:8080");

        EafCapabilityScanner scanner = mock(EafCapabilityScanner.class);
        when(scanner.scan()).thenReturn(List.of());

        EafRegistryClient a = new EafRegistryClient(properties, scanner,
                new EafAgentGraphScanner(List.of()),
                new SdkDescriptionSourceSettingsHolder(),
                new MockEnvironment().withProperty("server.port", "8611"));
        EafRegistryClient b = new EafRegistryClient(properties, scanner,
                new EafAgentGraphScanner(List.of()),
                new SdkDescriptionSourceSettingsHolder(),
                new MockEnvironment().withProperty("server.port", "8612"));

        assertNotEquals(a.getInstanceId(), b.getInstanceId(),
                "同机多实例靠端口区分时 instanceId 必须不同");
    }

    @Test
    void governancePolicyUpdatesLocalRuntimeState() {
        EafRegistryClient client = newClient();

        RuntimeGovernanceState state = client.updateGovernanceState(new EafRegistryClient.RuntimeGovernancePolicy(
                true,
                "DISABLED",
                "1.2.0",
                false,
                true,
                "disabled by control plane"
        ));

        assertEquals(true, state.disabled());
        assertEquals(false, state.localExecutionAllowed());
        assertEquals(false, state.embeddedExecutionAllowed());
        assertEquals(false, state.allowEmbeddedExecution());
        assertEquals(true, state.allowHybridExecution());
        assertEquals("disabled by control plane", client.governanceState().message());
    }

    @Test
    void governancePolicyDefaultsRuntimeAdmissionToAllowed() {
        EafRegistryClient client = newClient();

        RuntimeGovernanceState state = client.updateGovernanceState(new EafRegistryClient.RuntimeGovernancePolicy(
                false,
                "ONLINE",
                null,
                null,
                null,
                "ok"
        ));

        assertEquals(true, state.localExecutionAllowed());
        assertEquals(true, state.embeddedExecutionAllowed());
        assertEquals(true, state.hybridExecutionAllowed());
    }

    @Test
    void heartbeatPolicyParserIgnoresInstanceDateFields() {
        EafRegistryClient client = newClient();

        EafRegistryClient.RuntimeGovernancePolicy policy = client.parseHeartbeatPolicy("""
                {
                  "instance": {
                    "instanceId": "order-service-node-a",
                    "lastHeartbeatAt": "2026-05-28T20:18:57"
                  },
                  "policy": {
                    "disabled": false,
                    "status": "ONLINE",
                    "minSdkVersion": "1.0.0",
                    "allowEmbeddedExecution": true,
                    "allowHybridExecution": false,
                    "message": "ok"
                  }
                }
                """);

        assertEquals("ONLINE", policy.status());
        assertEquals("1.0.0", policy.minSdkVersion());
        assertEquals(true, policy.allowEmbeddedExecution());
        assertEquals(false, policy.allowHybridExecution());
    }

    private EafRegistryClient newClient() {
        EafRegistryProperties properties = new EafRegistryProperties();
        properties.getRegistry().setUrl("http://localhost:18603");
        properties.getProject().setCode("order-service");
        properties.getProject().setBaseUrl("http://localhost:8080");

        EafCapabilityScanner scanner = mock(EafCapabilityScanner.class);
        when(scanner.scan()).thenReturn(List.of());

        return new EafRegistryClient(
                properties,
                scanner,
                new SdkDescriptionSourceSettingsHolder());
    }
}
