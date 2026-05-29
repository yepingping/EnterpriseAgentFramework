package com.enterprise.ai.spring.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eaf")
public class EafRegistryProperties {

    private final Registry registry = new Registry();

    private final Project project = new Project();

    private final Capability capability = new Capability();

    private final AgentGraph agentGraph = new AgentGraph();

    public Registry getRegistry() {
        return registry;
    }

    public Project getProject() {
        return project;
    }

    public Capability getCapability() {
        return capability;
    }

    public AgentGraph getAgentGraph() {
        return agentGraph;
    }

    public static class Registry {
        private boolean enabled = true;
        private String url;
        private String appKey;
        private String appSecret;
        private long heartbeatIntervalMs = 30_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public long getHeartbeatIntervalMs() {
            return heartbeatIntervalMs;
        }

        public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
        }
    }

    public static class Project {
        private String code;
        private String name;
        private String baseUrl;
        private String contextPath = "";
        private String environment = "default";
        private String owner;
        private String visibility = "PRIVATE";
        /**
         * 稳定实例 ID。配置后将完全覆盖 SDK 内部自动生成的实例 ID，
         * 用于保证业务系统重启、PID 变化时仍然复用同一行心跳记录。
         * <p>未配置时，SDK 会按 {@code host + ":" + appName + ":" + port} 的方式
         * 计算一个和进程生命周期无关的默认值。</p>
         */
        private String instanceId;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }
    }

    public static class Capability {
        private boolean scanController = true;
        private boolean exposeActuatorEndpoint = true;
        private boolean syncOnStartup = true;

        public boolean isScanController() {
            return scanController;
        }

        public void setScanController(boolean scanController) {
            this.scanController = scanController;
        }

        public boolean isExposeActuatorEndpoint() {
            return exposeActuatorEndpoint;
        }

        public void setExposeActuatorEndpoint(boolean exposeActuatorEndpoint) {
            this.exposeActuatorEndpoint = exposeActuatorEndpoint;
        }

        public boolean isSyncOnStartup() {
            return syncOnStartup;
        }

        public void setSyncOnStartup(boolean syncOnStartup) {
            this.syncOnStartup = syncOnStartup;
        }
    }

    public static class AgentGraph {
        private boolean syncOnStartup = true;

        public boolean isSyncOnStartup() {
            return syncOnStartup;
        }

        public void setSyncOnStartup(boolean syncOnStartup) {
            this.syncOnStartup = syncOnStartup;
        }
    }
}
