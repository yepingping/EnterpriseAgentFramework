package com.enterprise.ai.agent.controller;

import com.enterprise.ai.agent.registry.RegistryCredentialEntity;
import com.enterprise.ai.agent.registry.RegistrySecurityService;
import com.enterprise.ai.agent.scan.ScanProjectEntity;
import com.enterprise.ai.agent.scan.ScanProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScanProjectService scanProjectService = mock(ScanProjectService.class);
    private final RegistrySecurityService registrySecurityService = mock(RegistrySecurityService.class);
    private final AiAssistController controller = new AiAssistController(scanProjectService, registrySecurityService);

    @Test
    void onboardingManifestIncludesAppKeyButNeverAppSecret() {
        ScanProjectEntity project = project();
        RegistryCredentialEntity credential = new RegistryCredentialEntity();
        credential.setAppKey("demo-app-key");
        credential.setAppSecret("secret-must-not-leak");
        when(scanProjectService.getById(1L)).thenReturn(project);
        when(registrySecurityService.findPrimaryActiveCredential("demo-service"))
                .thenReturn(Optional.of(credential));

        ResponseEntity<?> response = controller.onboardingManifest(1L, null, request());
        Map<String, Object> body = objectMapper.convertValue(response.getBody(), new TypeReference<>() {
        });
        Map<String, Object> projectBody = objectMapper.convertValue(body.get("project"), new TypeReference<>() {
        });
        Map<String, Object> securityBody = objectMapper.convertValue(body.get("security"), new TypeReference<>() {
        });
        String serialized = objectMapper.convertValue(body, Map.class).toString();

        assertEquals("reachai.onboarding.v1", body.get("schema"));
        assertEquals("demo-app-key", projectBody.get("registryAppKey"));
        assertEquals(Boolean.TRUE, projectBody.get("registryCredentialConfigured"));
        assertEquals("REACHAI_REGISTRY_APP_SECRET", securityBody.get("appSecretEnv"));
        assertFalse(serialized.contains("secret-must-not-leak"));
    }

    @Test
    void onboardingManifestRejectsInvalidAiCodingAccessKey() {
        when(scanProjectService.matchesAiCodingAccessKey(1L, "wrong-key")).thenReturn(false);

        ResponseEntity<?> response = controller.onboardingManifest(1L, "wrong-key", request());

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void onboardingManifestAcceptsValidAiCodingAccessKey() {
        ScanProjectEntity project = project();
        project.setAiCodingAccessEnabled(true);
        project.setAiCodingAccessKey("rac_valid");
        when(scanProjectService.matchesAiCodingAccessKey(1L, "rac_valid")).thenReturn(true);
        when(scanProjectService.getById(1L)).thenReturn(project);
        when(registrySecurityService.findPrimaryActiveCredential("demo-service")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.onboardingManifest(1L, "rac_valid", request());

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateAiCodingAccessDelegatesAndReturnsSavedKey() {
        ScanProjectEntity updated = project();
        updated.setAiCodingAccessEnabled(true);
        updated.setAiCodingAccessKey("rac_saved");
        when(scanProjectService.updateAiCodingAccess(
                1L,
                new ScanProjectService.AiCodingAccessUpdate(true, "rac_saved")))
                .thenReturn(updated);

        ResponseEntity<?> response = controller.updateAiCodingAccess(
                1L,
                new AiAssistController.AiCodingAccessUpdateRequest(true, "rac_saved"));
        Map<String, Object> body = objectMapper.convertValue(response.getBody(), new TypeReference<>() {
        });

        assertEquals(Boolean.TRUE, body.get("enabled"));
        assertEquals("rac_saved", body.get("accessKey"));
        verify(scanProjectService).updateAiCodingAccess(
                1L,
                new ScanProjectService.AiCodingAccessUpdate(true, "rac_saved"));
    }

    @Test
    void latestSkillZipContainsSkillEntry() throws Exception {
        ResponseEntity<byte[]> response = controller.downloadLatestSkill();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        boolean hasSkill = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.getBody()))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if ("reachai-onboarding/SKILL.md".equals(entry.getName())) {
                    hasSkill = true;
                    break;
                }
            }
        }
        assertTrue(hasSkill);
    }

    @Test
    void latestSkillMetadataPointsToDownloadUrl() {
        ResponseEntity<AiAssistController.SkillPackageResponse> response = controller.latestSkill(request());

        assertEquals("reachai-onboarding", response.getBody().name());
        assertTrue(response.getBody().downloadUrl().endsWith("/api/ai-assist/skills/reachai-onboarding/latest.zip"));
        assertTrue(response.getBody().files().stream().anyMatch(file -> "SKILL.md".equals(file.path())));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(18603);
        return request;
    }

    private ScanProjectEntity project() {
        ScanProjectEntity entity = new ScanProjectEntity();
        entity.setId(1L);
        entity.setName("Demo Service");
        entity.setProjectCode("demo-service");
        entity.setProjectKind("REGISTERED");
        entity.setEnvironment("dev");
        entity.setBaseUrl("http://localhost:8080");
        entity.setContextPath("");
        entity.setScanPath("");
        entity.setScanType("controller");
        entity.setToolCount(0);
        entity.setStatus("created");
        entity.setAiCodingAccessEnabled(true);
        entity.setAiCodingAccessKey("rac_demo");
        return entity;
    }
}
