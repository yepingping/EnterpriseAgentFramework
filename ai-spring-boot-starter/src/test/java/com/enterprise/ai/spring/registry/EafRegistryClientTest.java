package com.enterprise.ai.spring.registry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(body.get("instanceId").toString().contains("-"));
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
