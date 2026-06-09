package com.enterprise.ai.reach.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReachAiPrincipal {

    private String tenantId;
    private String appId;
    private String externalUserId;
    private String globalUserId;
    private String userName;
    private String deptId;
    private String deptName;
    private List<String> roles = Collections.emptyList();
    private Map<String, Object> attributes = Collections.emptyMap();

    public static Builder builder() {
        return new Builder();
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAppId() {
        return appId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getGlobalUserId() {
        return globalUserId;
    }

    public String getUserName() {
        return userName;
    }

    public String getDeptId() {
        return deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class Builder {
        private final ReachAiPrincipal principal = new ReachAiPrincipal();

        public Builder tenantId(String value) {
            principal.tenantId = trimToNull(value);
            return this;
        }

        public Builder appId(String value) {
            principal.appId = trimToNull(value);
            return this;
        }

        public Builder externalUserId(String value) {
            principal.externalUserId = trimToNull(value);
            return this;
        }

        public Builder globalUserId(String value) {
            principal.globalUserId = trimToNull(value);
            return this;
        }

        public Builder userName(String value) {
            principal.userName = trimToNull(value);
            return this;
        }

        public Builder deptId(String value) {
            principal.deptId = trimToNull(value);
            return this;
        }

        public Builder deptName(String value) {
            principal.deptName = trimToNull(value);
            return this;
        }

        public Builder roles(List<String> value) {
            principal.roles = value == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(value));
            return this;
        }

        public Builder attributes(Map<String, Object> value) {
            principal.attributes = value == null
                    ? Collections.<String, Object>emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
            return this;
        }

        public ReachAiPrincipal build() {
            return principal;
        }

        private static String trimToNull(String value) {
            return value == null || value.trim().isEmpty() ? null : value.trim();
        }
    }
}
