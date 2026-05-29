package com.enterprise.ai.spring.registry;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EafAutoConfigurationMetadataTest {

    @Test
    void exposesBoot2SpringFactoriesAutoConfigurationMetadata() throws IOException {
        ClassPathResource resource = new ClassPathResource("META-INF/spring.factories");

        assertTrue(resource.exists(), "Spring Boot 2.x needs META-INF/spring.factories");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(content.contains("org.springframework.boot.autoconfigure.EnableAutoConfiguration"));
        assertTrue(content.contains(EafRegistryAutoConfiguration.class.getName()));
    }
}
