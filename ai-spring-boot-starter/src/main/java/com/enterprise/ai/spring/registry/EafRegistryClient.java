package com.enterprise.ai.spring.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class EafRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(EafRegistryClient.class);

    private final EafRegistryProperties properties;

    private final EafCapabilityScanner scanner;

    private final EafAgentGraphScanner graphScanner;

    private final SdkDescriptionSourceSettingsHolder descriptionSettingsHolder;

    private final RestTemplate restTemplate;

    private final String registryBaseUrl;

    private final Environment environment;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String instanceId;

    private final AtomicReference<RuntimeGovernanceState> governanceState =
            new AtomicReference<>(RuntimeGovernanceState.defaultState());

    public EafRegistryClient(EafRegistryProperties properties,
                             EafCapabilityScanner scanner,
                             SdkDescriptionSourceSettingsHolder descriptionSettingsHolder) {
        this(properties, scanner, new EafAgentGraphScanner(List.of()), descriptionSettingsHolder, null);
    }

    public EafRegistryClient(EafRegistryProperties properties,
                             EafCapabilityScanner scanner,
                             EafAgentGraphScanner graphScanner,
                             SdkDescriptionSourceSettingsHolder descriptionSettingsHolder) {
        this(properties, scanner, graphScanner, descriptionSettingsHolder, null);
    }

    public EafRegistryClient(EafRegistryProperties properties,
                             EafCapabilityScanner scanner,
                             EafAgentGraphScanner graphScanner,
                             SdkDescriptionSourceSettingsHolder descriptionSettingsHolder,
                             Environment environment) {
        this.properties = properties;
        this.scanner = scanner;
        this.graphScanner = graphScanner;
        this.descriptionSettingsHolder = descriptionSettingsHolder;
        this.environment = environment;
        this.restTemplate = createRestTemplate();
        this.registryBaseUrl = trimTrailingSlash(properties.getRegistry().getUrl());
        this.instanceId = resolveInstanceId();
    }

    /**
     * 从注册中心拉取「接口/参数说明来源」顺序（与 scan_settings 一致，服务端已过滤 Javadoc）。
     * 失败时使用内置默认，不抛异常。
     */
    public void refreshDescriptionSettings() {
        if (!isConfigured()) {
            return;
        }
        try {
            SdkCapabilityDescriptionSettings s = signedGet(
                    "/api/registry/projects/{projectCode}/capability-description-settings",
                    SdkCapabilityDescriptionSettings.class);
            if (s != null) {
                descriptionSettingsHolder.update(s);
            }
        } catch (Exception ex) {
            log.warn("[ReachAI Registry] capability-description-settings fetch failed, using built-in defaults: {}",
                    ex.toString());
            descriptionSettingsHolder.resetToBuiltInDefaults();
        }
    }

    public void registerAndSync() {
        if (!isConfigured()) {
            return;
        }
        try {
            refreshDescriptionSettings();
            registerProject();
            heartbeat();
            if (properties.getCapability().isSyncOnStartup()) {
                syncCapabilities(scanner.scan());
            }
            if (properties.getAgentGraph().isSyncOnStartup()) {
                syncAgentGraphs(graphScanner.scan());
            }
        } catch (Exception ex) {
            // Starter 不能因为注册中心临时不可用拖垮业务系统启动；后续心跳会继续重试。
            logFailure("registerAndSync", ex);
        }
    }

    public void heartbeat() {
        if (!isConfigured()) {
            return;
        }
        refreshDescriptionSettings();
        try {
            String response = signedPost(
                    "/api/registry/projects/{projectCode}/instances/heartbeat",
                    buildHeartbeatBody(),
                    String.class);
            RuntimeGovernancePolicy policy = parseHeartbeatPolicy(response);
            RuntimeGovernanceState state = updateGovernanceState(policy);
            if (!state.localExecutionAllowed()) {
                log.warn("[ReachAI Registry] runtime local execution restricted by control plane: status={} minSdkVersion={} sdkVersionSatisfied={} message={}",
                        state.status(), state.minSdkVersion(), state.sdkVersionSatisfied(), state.message());
            }
        } catch (Exception ex) {
            // 定时任务上抛异常会刷屏；这里打日志即可，下一轮继续重试。
            logFailure("heartbeat", ex);
        }
    }

    RuntimeGovernancePolicy parseHeartbeatPolicy(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode policy = objectMapper.readTree(responseBody).path("policy");
            if (policy.isMissingNode() || policy.isNull()) {
                return null;
            }
            return new RuntimeGovernancePolicy(
                    policy.path("disabled").asBoolean(false),
                    textOrNull(policy, "status"),
                    textOrNull(policy, "minSdkVersion"),
                    booleanOrNull(policy, "allowEmbeddedExecution"),
                    booleanOrNull(policy, "allowHybridExecution"),
                    textOrNull(policy, "message")
            );
        } catch (Exception ex) {
            log.warn("[ReachAI Registry] heartbeat policy parse failed: {}", ex.toString());
            return null;
        }
    }

    public void offline() {
        if (!isConfigured()) {
            return;
        }
        try {
            signedPost("/api/registry/projects/{projectCode}/instances/offline", Map.of("instanceId", instanceId));
        } catch (Exception ignored) {
            // shutdown hook 中失败不影响业务进程退出
        }
    }

    public List<EafCapabilityDescriptor> capabilities() {
        return scanner.scan();
    }

    public List<EafAgentGraph> agentGraphs() {
        return graphScanner.scan();
    }

    public RuntimeGovernanceState governanceState() {
        return governanceState.get();
    }

    public EafEmbedTokenResponse exchangeEmbedToken(Map<String, Object> body) {
        return signedPost("/api/embed/token/exchange", body, EafEmbedTokenResponse.class);
    }

    public EafUserSyncResult upsertExternalUser(EafExternalUser user) {
        return signedPost("/api/embed/users/{projectCode}", EafIdentityClient.externalUserBody(user), EafUserSyncResult.class);
    }

    public EafUserSyncResult disableExternalUser(String externalUserId) {
        return signedPost(
                "/api/embed/users/{projectCode}/{externalUserId}/disable",
                Map.of(),
                EafUserSyncResult.class,
                properties.getProject().getCode(),
                externalUserId);
    }

    public EafUserSyncResult deleteExternalUser(String externalUserId) {
        return signedPost(
                "/api/embed/users/{projectCode}/{externalUserId}/delete",
                Map.of(),
                EafUserSyncResult.class,
                properties.getProject().getCode(),
                externalUserId);
    }

    public EafBatchUserSyncResult syncExternalUsers(List<EafExternalUser> users) {
        List<Map<String, Object>> body = users.stream()
                .map(EafIdentityClient::externalUserBody)
                .toList();
        return signedPost("/api/embed/users/{projectCode}/sync", Map.of("users", body), EafBatchUserSyncResult.class);
    }

    private void registerProject() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectCode", properties.getProject().getCode());
        body.put("name", defaultString(properties.getProject().getName(), properties.getProject().getCode()));
        body.put("environment", defaultString(properties.getProject().getEnvironment(), "default"));
        body.put("owner", defaultString(properties.getProject().getOwner(), ""));
        body.put("visibility", defaultString(properties.getProject().getVisibility(), "PRIVATE"));
        body.put("baseUrl", defaultString(properties.getProject().getBaseUrl(), ""));
        body.put("contextPath", defaultString(properties.getProject().getContextPath(), ""));
        body.put("appKey", defaultString(properties.getRegistry().getAppKey(), ""));
        if (StringUtils.hasText(properties.getRegistry().getAppSecret())) {
            body.put("appSecret", properties.getRegistry().getAppSecret());
        }
        signedPost("/api/registry/projects/register", body);
    }

    private void syncCapabilities(List<EafCapabilityDescriptor> capabilities) {
        Map<String, Object> body = Map.of(
                "syncId", UUID.randomUUID().toString(),
                "source", "SDK",
                "apply", true,
                "capabilities", capabilities
        );
        signedPost("/api/registry/projects/{projectCode}/capabilities/sync", body);
    }

    private void syncAgentGraphs(List<EafAgentGraph> graphs) {
        if (graphs == null || graphs.isEmpty()) {
            return;
        }
        Map<String, Object> body = Map.of(
                "syncId", UUID.randomUUID().toString(),
                "source", "SDK",
                "apply", true,
                "graphs", graphs
        );
        signedPost("/api/registry/projects/{projectCode}/agent-graphs/sync", body);
    }

    Map<String, Object> buildHeartbeatBody() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("runtimePlacement", "EMBEDDED");
        metadata.put("runtimeTypes", List.of("SPRING_BOOT_EMBEDDED"));
        int graphCount = graphScanner.scan().size();
        metadata.put("supportsGraph", graphCount > 0);
        metadata.put("supportsTools", true);
        metadata.put("supportsWorkflow", graphCount > 0);
        metadata.put("supportsAutonomous", false);
        metadata.put("supportsEmbeddedExecution", true);
        metadata.put("supportsHybridExecution", false);
        metadata.put("capabilityCount", scanner.scan().size());
        metadata.put("agentGraphCount", graphCount);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instanceId", instanceId);
        body.put("baseUrl", defaultString(properties.getProject().getBaseUrl(), ""));
        body.put("host", hostName());
        Integer port = serverPort();
        if (port != null) {
            body.put("port", port);
        }
        String appVersion = applicationVersion();
        if (StringUtils.hasText(appVersion)) {
            body.put("appVersion", appVersion);
        }
        body.put("sdkVersion", sdkVersion());
        body.put("metadata", metadata);
        return body;
    }

    private boolean isConfigured() {
        return properties.getRegistry().isEnabled()
                && StringUtils.hasText(properties.getRegistry().getUrl())
                && StringUtils.hasText(properties.getProject().getCode());
    }

    private String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void signedPost(String uri, Object body) {
        signedPost(uri, body, Void.class);
    }

    private <T> T signedPost(String uri, Object body, Class<T> bodyType) {
        return signedPost(uri, body, bodyType, properties.getProject().getCode());
    }

    private <T> T signedPost(String uri, Object body, Class<T> bodyType, Object... uriVariables) {
        SignatureHeaders headers = signatureHeaders();
        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders(headers));
        return restTemplate.postForObject(registryBaseUrl + uri, entity, bodyType, uriVariables);
    }

    private <T> T signedGet(String uriTemplate, Class<T> bodyType) {
        SignatureHeaders headers = signatureHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders(headers));
        return restTemplate.exchange(
                registryBaseUrl + uriTemplate,
                HttpMethod.GET,
                entity,
                bodyType,
                properties.getProject().getCode()
        ).getBody();
    }

    private HttpHeaders httpHeaders(SignatureHeaders headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("X-EAF-App-Key", headers.appKey());
        httpHeaders.add("X-EAF-Timestamp", headers.timestamp());
        httpHeaders.add("X-EAF-Nonce", headers.nonce());
        httpHeaders.add("X-EAF-Signature", headers.signature());
        return httpHeaders;
    }

    private RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();
        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter(objectMapper);
        template.getMessageConverters().removeIf(converter ->
                converter.getClass().getName().contains("GsonHttpMessageConverter"));
        template.getMessageConverters().add(0, jackson);
        template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return template;
    }

    private void logFailure(String phase, Exception ex) {
        String base = properties.getRegistry().getUrl();
        String code = properties.getProject().getCode();
        if (ex instanceof RestClientResponseException rre) {
            String body = rre.getResponseBodyAsString();
            if (body != null && body.length() > 800) {
                body = body.substring(0, 800) + "...";
            }
            log.warn("[ReachAI Registry] {} failed project={} registryUrl={} httpStatus={} responseBody={}",
                    phase, code, base, rre.getStatusCode().value(), body);
            return;
        }
        log.warn("[ReachAI Registry] {} failed project={} registryUrl={}",
                phase, code, base, ex);
    }

    private SignatureHeaders signatureHeaders() {
        String appKey = defaultString(properties.getRegistry().getAppKey(), properties.getProject().getCode());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String message = properties.getProject().getCode() + "\n" + timestamp + "\n" + nonce;
        String signature = StringUtils.hasText(properties.getRegistry().getAppSecret())
                ? hmacSha256Hex(properties.getRegistry().getAppSecret(), message)
                : "";
        return new SignatureHeaders(appKey, timestamp, nonce, signature);
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 计算稳定的实例 ID。优先级：
     * <ol>
     *     <li>{@code eaf.project.instance-id} 显式配置。</li>
     *     <li>{@code host + ":" + appName + ":" + port}（appName 取 {@code spring.application.name}
     *     或项目 code，port 取 {@code server.port}）。</li>
     *     <li>退化方案：{@code host + "-" + projectCode}。</li>
     * </ol>
     * 全部基于配置或宿主机信息，和 JVM 进程 PID 解耦，业务系统重启不会再生成新实例记录。
     */
    String resolveInstanceId() {
        String configured = properties.getProject().getInstanceId();
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        String host = hostName();
        String appName = applicationName();
        Integer port = serverPort();
        StringBuilder sb = new StringBuilder(host);
        if (StringUtils.hasText(appName)) {
            sb.append(":").append(appName);
        }
        if (port != null) {
            sb.append(":").append(port);
        }
        if (sb.length() == host.length()) {
            sb.append("-").append(defaultString(properties.getProject().getCode(), "default"));
        }
        return sb.toString();
    }

    String getInstanceId() {
        return instanceId;
    }

    private String applicationName() {
        String fromEnv = environment == null ? null : environment.getProperty("spring.application.name");
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.trim();
        }
        String code = properties.getProject().getCode();
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private String applicationVersion() {
        if (environment == null) {
            return null;
        }
        String version = environment.getProperty("eaf.project.app-version");
        if (StringUtils.hasText(version)) {
            return version.trim();
        }
        return environment.getProperty("info.app.version");
    }

    private Integer serverPort() {
        if (environment == null) {
            return null;
        }
        // 优先用实际监听端口（Tomcat/Jetty/Netty 启动后 local.server.port 会暴露真实值）
        String raw = environment.getProperty("local.server.port");
        if (!StringUtils.hasText(raw)) {
            raw = environment.getProperty("server.port");
        }
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            int port = Integer.parseInt(raw.trim());
            return port > 0 ? port : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String sdkVersion() {
        String version = EafRegistryClient.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    RuntimeGovernanceState updateGovernanceState(RuntimeGovernancePolicy policy) {
        if (policy == null) {
            return governanceState.get();
        }
        String currentSdkVersion = sdkVersion();
        boolean sdkVersionSatisfied = isSdkVersionSatisfied(currentSdkVersion, policy.minSdkVersion());
        RuntimeGovernanceState state = new RuntimeGovernanceState(
                policy.disabled(),
                defaultString(policy.status(), "UNKNOWN"),
                blankToNull(policy.minSdkVersion()),
                sdkVersionSatisfied,
                policy.allowEmbeddedExecution() == null || policy.allowEmbeddedExecution(),
                policy.allowHybridExecution() == null || policy.allowHybridExecution(),
                defaultString(policy.message(), policy.disabled() ? "runtime disabled by control plane" : "ok")
        );
        governanceState.set(state);
        return state;
    }

    private boolean isSdkVersionSatisfied(String current, String minimum) {
        if (!StringUtils.hasText(minimum) || !StringUtils.hasText(current) || "dev".equalsIgnoreCase(current)) {
            return true;
        }
        return compareVersions(current, minimum) >= 0;
    }

    private int compareVersions(String left, String right) {
        List<Integer> l = versionSegments(left);
        List<Integer> r = versionSegments(right);
        int size = Math.max(l.size(), r.size());
        for (int i = 0; i < size; i++) {
            int lv = i < l.size() ? l.get(i) : 0;
            int rv = i < r.size() ? r.get(i) : 0;
            if (lv != rv) {
                return Integer.compare(lv, rv);
            }
        }
        return 0;
    }

    private List<Integer> versionSegments(String version) {
        String normalized = version == null ? "" : version.trim().replaceFirst("^[vV]", "");
        String[] parts = normalized.split("[^0-9]+");
        return java.util.Arrays.stream(parts)
                .filter(StringUtils::hasText)
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .toList();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Boolean booleanOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asBoolean();
    }

    private record SignatureHeaders(String appKey, String timestamp, String nonce, String signature) {
    }

    record RuntimeGovernancePolicy(boolean disabled,
                                           String status,
                                           String minSdkVersion,
                                           Boolean allowEmbeddedExecution,
                                           Boolean allowHybridExecution,
                                           String message) {
    }
}
