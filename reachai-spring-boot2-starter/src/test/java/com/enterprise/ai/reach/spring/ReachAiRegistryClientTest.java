package com.enterprise.ai.reach.spring;

import com.enterprise.ai.reach.sdk.annotation.ReachCapability;
import com.enterprise.ai.reach.sdk.annotation.ReachParam;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReachAiRegistryClientTest {

    @Test
    void registerAndSyncPostsProjectThenCapabilitiesWithReachAiHeaders() {
        ReachAiRegistryProperties properties = new ReachAiRegistryProperties();
        properties.getRegistry().setUrl("https://reachai.example.com/");
        properties.getRegistry().setAppKey("demo-key");
        properties.getRegistry().setAppSecret("demo-secret");
        properties.getProject().setCode("demo");
        properties.getProject().setName("Demo Project");
        properties.getProject().setBaseUrl("https://biz.example.com");
        properties.getEmbed().setAllowedOrigins(Arrays.asList("http://localhost:9200"));
        properties.getEmbed().setAllowedAgentIds(Arrays.asList("team-archive-assistant"));
        properties.getEmbed().setTokenTtlSeconds(1800);

        RecordingTransport transport = new RecordingTransport();
        ReachAiRegistryClient client = new ReachAiRegistryClient(
                properties,
                new ReachCapabilityBeanScanner(new Object[]{new ContractCapability()}),
                transport);

        client.registerAndSync();

        assertEquals(3, transport.requests.size());
        RecordingTransport.Request register = transport.requests.get(0);
        assertEquals("POST", register.method);
        assertEquals("https://reachai.example.com/api/registry/projects/register", register.url);
        assertEquals("demo-key", register.headers.get("X-ReachAI-App-Key"));
        assertNotNull(register.headers.get("X-ReachAI-Timestamp"));
        assertNotNull(register.headers.get("X-ReachAI-Nonce"));
        assertNotNull(register.headers.get("X-ReachAI-Signature"));
        assertEquals(4, register.headers.size());
        assertEquals("demo", register.body.get("projectCode"));
        assertEquals("Demo Project", register.body.get("name"));
        assertEquals(Arrays.asList("http://localhost:9200"), register.body.get("allowedOrigins"));
        assertEquals(Arrays.asList("team-archive-assistant"), register.body.get("allowedAgentIds"));
        assertEquals(1800, register.body.get("tokenTtlSeconds"));

        RecordingTransport.Request heartbeat = transport.requests.get(1);
        assertEquals("POST", heartbeat.method);
        assertEquals("https://reachai.example.com/api/registry/projects/demo/instances/heartbeat", heartbeat.url);
        assertNotNull(heartbeat.body.get("instanceId"));
        Map<?, ?> metadata = (Map<?, ?>) heartbeat.body.get("metadata");
        assertEquals("CAPABILITY_HOST", metadata.get("runtimePlacement"));
        assertEquals(Boolean.TRUE, metadata.get("supportsTools"));
        assertEquals(1, metadata.get("capabilityCount"));

        RecordingTransport.Request sync = transport.requests.get(2);
        assertEquals("POST", sync.method);
        assertEquals("https://reachai.example.com/api/registry/projects/demo/capabilities/sync", sync.url);
        assertEquals("SDK", sync.body.get("source"));
        assertEquals(Boolean.TRUE, sync.body.get("apply"));
        List<?> capabilities = (List<?>) sync.body.get("capabilities");
        assertEquals(1, capabilities.size());
        Map<?, ?> capability = (Map<?, ?>) capabilities.get(0);
        assertEquals("contract.query", capability.get("name"));
        assertEquals("POST", capability.get("httpMethod"));
        assertEquals("https://biz.example.com", capability.get("baseUrl"));
        assertEquals("", capability.get("contextPath"));
        assertEquals("/reachai/capabilities/contract.query/invoke", capability.get("endpointPath"));
        assertEquals("java.util.Map", capability.get("requestBodyType"));
        assertEquals("java.lang.String", capability.get("responseType"));
        assertEquals("WRITE", capability.get("sideEffect"));
        assertEquals(Boolean.TRUE, capability.get("enabled"));
        assertEquals(Boolean.TRUE, capability.get("agentVisible"));
    }

    @Test
    void registerAndSyncDoesNothingWhenRegistrySecretIsMissing() {
        ReachAiRegistryProperties properties = new ReachAiRegistryProperties();
        properties.getRegistry().setUrl("https://reachai.example.com");
        properties.getRegistry().setAppKey("demo-key");
        properties.getProject().setCode("demo");

        RecordingTransport transport = new RecordingTransport();
        ReachAiRegistryClient client = new ReachAiRegistryClient(
                properties,
                new ReachCapabilityBeanScanner(new Object[]{new ContractCapability()}),
                transport);

        client.registerAndSync();

        assertEquals(0, transport.requests.size());
    }

    @Test
    void registerAndSyncDoesNotBreakApplicationStartupWhenRegistryIsUnavailable() {
        ReachAiRegistryProperties properties = new ReachAiRegistryProperties();
        properties.getRegistry().setUrl("https://reachai.example.com");
        properties.getRegistry().setAppKey("demo-key");
        properties.getRegistry().setAppSecret("demo-secret");
        properties.getProject().setCode("demo");

        ReachAiRegistryClient client = new ReachAiRegistryClient(
                properties,
                new ReachCapabilityBeanScanner(new Object[]{new ContractCapability()}),
                new FailingTransport());

        assertDoesNotThrow(client::registerAndSync);
    }

    @Test
    void registerAndSyncIncludesPlainSpringMvcControllerEndpoints() {
        ReachAiRegistryProperties properties = new ReachAiRegistryProperties();
        properties.getRegistry().setUrl("https://reachai.example.com");
        properties.getRegistry().setAppKey("demo-key");
        properties.getRegistry().setAppSecret("demo-secret");
        properties.getProject().setCode("demo");
        properties.getProject().setBaseUrl("https://biz.example.com");

        RecordingTransport transport = new RecordingTransport();
        ReachAiRegistryClient client = new ReachAiRegistryClient(
                properties,
                new ReachCapabilityBeanScanner(new Object[]{new PlainController()}),
                transport);

        client.registerAndSync();

        RecordingTransport.Request sync = transport.requests.get(2);
        List<?> capabilities = (List<?>) sync.body.get("capabilities");
        assertEquals(2, capabilities.size());

        Map<?, ?> getItem = findCapability(capabilities, "plain_getStatus");
        assertEquals("plain_getStatus", getItem.get("name"));
        assertEquals("GET", getItem.get("httpMethod"));
        assertEquals("/plain/getStatus", getItem.get("endpointPath"));
        assertEquals(Boolean.FALSE, getItem.get("agentVisible"));
        Map<?, ?> getMetadata = (Map<?, ?>) getItem.get("metadata");
        assertEquals(Boolean.FALSE, getMetadata.get("declared"));
        assertEquals("SpringMvcController", getMetadata.get("source"));
        assertEquals("PlainController", getMetadata.get("module"));

        Map<?, ?> postItem = findCapability(capabilities, "plain_create");
        assertEquals("plain_create", postItem.get("name"));
        assertEquals("POST", postItem.get("httpMethod"));
        assertEquals("/plain/create", postItem.get("endpointPath"));
        assertEquals("java.lang.String", postItem.get("requestBodyType"));
    }

    static class ContractCapability {
        @ReachCapability(name = "contract.query", title = "Query contract")
        public String query(@ReachParam(description = "Contract number", required = true) String contractNo) {
            return contractNo;
        }
    }

    private Map<?, ?> findCapability(List<?> capabilities, String name) {
        for (Object item : capabilities) {
            Map<?, ?> map = (Map<?, ?>) item;
            if (name.equals(map.get("name"))) {
                return map;
            }
        }
        throw new AssertionError("capability not found: " + name);
    }

    @RestController
    @RequestMapping("/plain")
    static class PlainController {
        @GetMapping("/getStatus")
        public String getStatus(String keyword) {
            return "ok";
        }

        @PostMapping("/create")
        public String create(@RequestBody String payload) {
            return payload;
        }
    }

    static class RecordingTransport implements ReachAiRegistryTransport {
        private final List<Request> requests = new ArrayList<Request>();

        @Override
        public String exchange(String method, String url, Map<String, String> headers, Object body) {
            requests.add(new Request(method, url, headers, body));
            return "{}";
        }

        static class Request {
            private final String method;
            private final String url;
            private final Map<String, String> headers;
            private final Map<?, ?> body;

            Request(String method, String url, Map<String, String> headers, Object body) {
                this.method = method;
                this.url = url;
                this.headers = headers;
                this.body = (Map<?, ?>) body;
            }
        }
    }

    static class FailingTransport implements ReachAiRegistryTransport {
        @Override
        public String exchange(String method, String url, Map<String, String> headers, Object body) {
            throw new IllegalStateException("registry unavailable");
        }
    }
}
