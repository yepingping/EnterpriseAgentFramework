package com.enterprise.ai.agent.controller;

import com.enterprise.ai.agent.registry.RegistryCredentialEntity;
import com.enterprise.ai.agent.registry.RegistrySecurityService;
import com.enterprise.ai.agent.scan.ScanProjectEntity;
import com.enterprise.ai.agent.scan.ScanProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/ai-assist")
@RequiredArgsConstructor
public class AiAssistController {

    private static final String SKILL_NAME = "reachai-onboarding";
    private static final String SKILL_VERSION = "0.1.0";
    private static final String SKILL_ROOT = "ai-assist/skills/" + SKILL_NAME + "/";
    private static final String SECRET_ENV_NAME = "REACHAI_REGISTRY_APP_SECRET";

    private static final List<String> SKILL_FILES = List.of(
            "SKILL.md",
            "agents/openai.yaml",
            "references/java-sdk-access.md",
            "references/platform-apis.md",
            "references/security.md",
            "templates/application-reachai.yml",
            "templates/pom-dependencies.xml",
            "templates/reach-capability-example.java",
            "scripts/verify-reachai-access.py"
    );

    private final ScanProjectService scanProjectService;
    private final RegistrySecurityService registrySecurityService;

    @GetMapping("/skills/reachai-onboarding/latest")
    public ResponseEntity<SkillPackageResponse> latestSkill(HttpServletRequest request) {
        String baseUrl = requestBaseUrl(request);
        String downloadUrl = baseUrl + "/api/ai-assist/skills/" + SKILL_NAME + "/latest.zip";
        List<SkillFileResponse> files = SKILL_FILES.stream()
                .map(path -> new SkillFileResponse(path))
                .toList();
        return ResponseEntity.ok(new SkillPackageResponse(
                SKILL_NAME,
                SKILL_VERSION,
                "ReachAI SDK onboarding skill for AI coding tools.",
                downloadUrl,
                files
        ));
    }

    @GetMapping(value = "/skills/reachai-onboarding/latest.zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadLatestSkill() throws IOException {
        byte[] body = zipSkillFiles();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(SKILL_NAME + "-" + SKILL_VERSION + ".zip", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(body.length)
                .body(body);
    }

    @GetMapping("/projects/{projectId}/onboarding-manifest")
    public ResponseEntity<?> onboardingManifest(@PathVariable Long projectId,
                                                @RequestParam(value = "aiCodingKey", required = false) String aiCodingKey,
                                                HttpServletRequest request) {
        try {
            if (StringUtils.hasText(aiCodingKey)
                    && !scanProjectService.matchesAiCodingAccessKey(projectId, aiCodingKey)) {
                return ResponseEntity.status(403).body(new ApiErrorResponse("invalid AI Coding access key"));
            }
            ScanProjectEntity project = scanProjectService.getById(projectId);
            String baseUrl = requestBaseUrl(request);
            Optional<RegistryCredentialEntity> credential = StringUtils.hasText(project.getProjectCode())
                    ? registrySecurityService.findPrimaryActiveCredential(project.getProjectCode())
                    : Optional.empty();
            String appKey = credential.map(RegistryCredentialEntity::getAppKey).orElse(null);

            return ResponseEntity.ok(new OnboardingManifestResponse(
                    "reachai.onboarding.v1",
                    new ProjectManifest(
                            project.getId(),
                            project.getName(),
                            project.getProjectCode(),
                            project.getProjectKind(),
                            project.getEnvironment(),
                            project.getBaseUrl(),
                            emptyToNull(project.getContextPath()),
                            appKey,
                            credential.isPresent()
                    ),
                    new AiCodingAccessManifest(
                            Boolean.TRUE.equals(project.getAiCodingAccessEnabled()),
                            project.getAiCodingAccessKey()
                    ),
                    new SdkManifest(
                            "1.0.0-SNAPSHOT",
                            List.of(
                                    new MavenDependency("com.enterprise.ai", "reachai-capability-sdk", "1.0.0-SNAPSHOT"),
                                    new MavenDependency("com.enterprise.ai", "reachai-spring-boot2-starter", "1.0.0-SNAPSHOT")
                            ),
                            new ReachAiConfigManifest(
                                    baseUrl,
                                    appKey,
                                    SECRET_ENV_NAME,
                                    emptyToNull(project.getProjectCode()),
                                    project.getName(),
                                    project.getBaseUrl(),
                                    emptyToNull(project.getContextPath()),
                                    emptyToNull(project.getEnvironment())
                            )
                    ),
                    new PlatformEndpoints(
                            baseUrl + "/api/ai-assist/skills/" + SKILL_NAME + "/latest.zip",
                            baseUrl + "/api/ai-assist/projects/" + projectId + "/onboarding-manifest",
                            baseUrl + "/api/scan-projects/" + projectId + "/sdk-access-check",
                            baseUrl + "/api/scan-projects/" + projectId + "/tools/reconcile"
                    ),
                    new SecurityGuidance(
                            SECRET_ENV_NAME,
                            "Do not paste or write the registry app secret into AI chat context. Store it in a local environment variable or secret manager."
                    )
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/projects/{projectId}/ai-coding-access")
    public ResponseEntity<?> updateAiCodingAccess(@PathVariable Long projectId,
                                                  @RequestBody(required = false) AiCodingAccessUpdateRequest request) {
        try {
            ScanProjectEntity project = scanProjectService.updateAiCodingAccess(projectId,
                    new ScanProjectService.AiCodingAccessUpdate(
                            request == null ? Boolean.FALSE : request.enabled(),
                            request == null ? null : request.accessKey()));
            return ResponseEntity.ok(new AiCodingAccessManifest(
                    Boolean.TRUE.equals(project.getAiCodingAccessEnabled()),
                    project.getAiCodingAccessKey()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    private byte[] zipSkillFiles() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (String path : SKILL_FILES) {
                ClassPathResource resource = new ClassPathResource(SKILL_ROOT + path);
                if (!resource.exists()) {
                    throw new IOException("Missing ReachAI skill resource: " + path);
                }
                ZipEntry entry = new ZipEntry(SKILL_NAME + "/" + path);
                zip.putNextEntry(entry);
                try (var input = resource.getInputStream()) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static String requestBaseUrl(HttpServletRequest request) {
        String scheme = headerOrDefault(request, "X-Forwarded-Proto", request.getScheme());
        String host = headerOrDefault(request, "X-Forwarded-Host", request.getServerName());
        String port = request.getServerPort() <= 0 ? "" : ":" + request.getServerPort();
        if (host.contains(":") || ("http".equalsIgnoreCase(scheme) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(scheme) && request.getServerPort() == 443)) {
            port = "";
        }
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return scheme + "://" + host + port + contextPath;
    }

    private static String headerOrDefault(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return StringUtils.hasText(value) ? value.split(",")[0].trim() : fallback;
    }

    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    record SkillPackageResponse(
            String name,
            String version,
            String description,
            String downloadUrl,
            List<SkillFileResponse> files
    ) {
    }

    record SkillFileResponse(String path) {
    }

    record OnboardingManifestResponse(
            String schema,
            ProjectManifest project,
            AiCodingAccessManifest aiCodingAccess,
            SdkManifest sdk,
            PlatformEndpoints endpoints,
            SecurityGuidance security
    ) {
    }

    record ProjectManifest(
            Long id,
            String name,
            String projectCode,
            String projectKind,
            String environment,
            String baseUrl,
            String contextPath,
            String registryAppKey,
            boolean registryCredentialConfigured
    ) {
    }

    record AiCodingAccessManifest(
            boolean enabled,
            String accessKey
    ) {
    }

    record AiCodingAccessUpdateRequest(
            Boolean enabled,
            String accessKey
    ) {
    }

    record SdkManifest(
            String version,
            List<MavenDependency> dependencies,
            ReachAiConfigManifest config
    ) {
    }

    record MavenDependency(String groupId, String artifactId, String version) {
    }

    record ReachAiConfigManifest(
            String registryUrl,
            String appKey,
            String appSecretEnv,
            String projectCode,
            String projectName,
            String projectBaseUrl,
            String projectContextPath,
            String environment
    ) {
    }

    record PlatformEndpoints(
            String skillPackageUrl,
            String manifestUrl,
            String sdkAccessCheckUrl,
            String reconcileToolsUrl
    ) {
    }

    record SecurityGuidance(String appSecretEnv, String message) {
    }

    record ApiErrorResponse(String message) {
    }
}
