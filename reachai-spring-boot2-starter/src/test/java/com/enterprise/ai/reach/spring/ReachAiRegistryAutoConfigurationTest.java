package com.enterprise.ai.reach.spring;

import com.enterprise.ai.reach.sdk.annotation.ReachCapability;
import com.enterprise.ai.reach.sdk.annotation.ReachParam;
import com.enterprise.ai.reach.sdk.capability.ReachCapabilityDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReachAiRegistryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReachAiRegistryAutoConfiguration.class))
            .withBean(ContractCapability.class)
            .withPropertyValues(
                    "reachai.registry.url=https://reachai.example.com",
                    "reachai.registry.app-key=demo-key",
                    "reachai.registry.app-secret=demo-secret",
                    "reachai.project.code=demo",
                    "reachai.project.name=Demo Project",
                    "reachai.embed.allowed-origins[0]=http://localhost:9200",
                    "reachai.embed.allowed-agent-ids[0]=team-archive-assistant",
                    "reachai.embed.token-ttl-seconds=1800");

    @Test
    void autoConfiguresReachAiPropertiesAndCapabilityScanner() {
        contextRunner.run(context -> {
            ReachAiRegistryProperties properties = context.getBean(ReachAiRegistryProperties.class);
            assertEquals("https://reachai.example.com", properties.getRegistry().getUrl());
            assertEquals("demo", properties.getProject().getCode());
            assertEquals("http://localhost:9200", properties.getEmbed().getAllowedOrigins().get(0));
            assertEquals("team-archive-assistant", properties.getEmbed().getAllowedAgentIds().get(0));
            assertEquals(1800, properties.getEmbed().getTokenTtlSeconds());

            ReachCapabilityBeanScanner scanner = context.getBean(ReachCapabilityBeanScanner.class);
            List<ReachCapabilityDescriptor> descriptors = scanner.scan();
            assertEquals(1, descriptors.size());
            assertEquals("contract.query", descriptors.get(0).getName());
            assertEquals("query", descriptors.get(0).getMethodName());
            assertNotNull(context.getBean(ReachAiRegistryClient.class));
        });
    }

    static class ContractCapability {
        @ReachCapability(name = "contract.query", title = "查询合同")
        public String query(@ReachParam(description = "合同编号", required = true) String contractNo) {
            return contractNo;
        }
    }
}
