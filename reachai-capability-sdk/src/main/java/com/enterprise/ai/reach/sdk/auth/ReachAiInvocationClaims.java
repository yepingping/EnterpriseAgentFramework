package com.enterprise.ai.reach.sdk.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReachAiInvocationClaims {

    private String projectCode;
    private String appKey;
    private String capabilityName;
    private String tenantId;
    private String externalUserId;
    private String globalUserId;
    private String userName;
    private String deptId;
    private String deptName;
    private List<String> roles = Collections.emptyList();
    private String agentId;
    private String sessionId;
    private String traceId;
    private String pageInstanceId;
    private String origin;
    private String route;
    private String jti;
    private long issuedAtEpochSeconds;
    private long expiresAtEpochSeconds;
    private Map<String, Object> attributes = Collections.emptyMap();

    public static Builder builder() {
        return new Builder();
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public String getTenantId() {
        return tenantId;
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

    public String getAgentId() {
        return agentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPageInstanceId() {
        return pageInstanceId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getRoute() {
        return route;
    }

    public String getJti() {
        return jti;
    }

    public long getIssuedAtEpochSeconds() {
        return issuedAtEpochSeconds;
    }

    public long getExpiresAtEpochSeconds() {
        return expiresAtEpochSeconds;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        put(out, "projectCode", projectCode);
        put(out, "appKey", appKey);
        put(out, "capabilityName", capabilityName);
        put(out, "tenantId", tenantId);
        put(out, "externalUserId", externalUserId);
        put(out, "globalUserId", globalUserId);
        put(out, "userName", userName);
        put(out, "deptId", deptId);
        put(out, "deptName", deptName);
        out.put("roles", roles == null ? Collections.emptyList() : roles);
        put(out, "agentId", agentId);
        put(out, "sessionId", sessionId);
        put(out, "traceId", traceId);
        put(out, "pageInstanceId", pageInstanceId);
        put(out, "origin", origin);
        put(out, "route", route);
        put(out, "jti", jti);
        out.put("iat", issuedAtEpochSeconds);
        out.put("exp", expiresAtEpochSeconds);
        out.put("attributes", attributes == null ? Collections.emptyMap() : attributes);
        return out;
    }

    @SuppressWarnings("unchecked")
    static ReachAiInvocationClaims fromMap(Map<String, Object> map) {
        Builder builder = builder()
                .projectCode(text(map.get("projectCode")))
                .appKey(text(map.get("appKey")))
                .capabilityName(text(map.get("capabilityName")))
                .tenantId(text(map.get("tenantId")))
                .externalUserId(text(map.get("externalUserId")))
                .globalUserId(text(map.get("globalUserId")))
                .userName(text(map.get("userName")))
                .deptId(text(map.get("deptId")))
                .deptName(text(map.get("deptName")))
                .agentId(text(map.get("agentId")))
                .sessionId(text(map.get("sessionId")))
                .traceId(text(map.get("traceId")))
                .pageInstanceId(text(map.get("pageInstanceId")))
                .origin(text(map.get("origin")))
                .route(text(map.get("route")))
                .jti(text(map.get("jti")))
                .issuedAtEpochSeconds(longValue(map.get("iat")))
                .expiresAtEpochSeconds(longValue(map.get("exp")));
        Object roles = map.get("roles");
        if (roles instanceof List<?>) {
            List<String> normalized = new ArrayList<String>();
            for (Object role : (List<?>) roles) {
                String text = text(role);
                if (text != null) {
                    normalized.add(text);
                }
            }
            builder.roles(normalized);
        }
        Object attributes = map.get("attributes");
        if (attributes instanceof Map<?, ?>) {
            builder.attributes((Map<String, Object>) attributes);
        }
        return builder.build();
    }

    private static void put(Map<String, Object> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = text(value);
        return text == null ? 0L : Long.parseLong(text);
    }

    public static class Builder {
        private final ReachAiInvocationClaims claims = new ReachAiInvocationClaims();

        public Builder projectCode(String value) {
            claims.projectCode = trimToNull(value);
            return this;
        }

        public Builder appKey(String value) {
            claims.appKey = trimToNull(value);
            return this;
        }

        public Builder capabilityName(String value) {
            claims.capabilityName = trimToNull(value);
            return this;
        }

        public Builder tenantId(String value) {
            claims.tenantId = trimToNull(value);
            return this;
        }

        public Builder externalUserId(String value) {
            claims.externalUserId = trimToNull(value);
            return this;
        }

        public Builder globalUserId(String value) {
            claims.globalUserId = trimToNull(value);
            return this;
        }

        public Builder userName(String value) {
            claims.userName = trimToNull(value);
            return this;
        }

        public Builder deptId(String value) {
            claims.deptId = trimToNull(value);
            return this;
        }

        public Builder deptName(String value) {
            claims.deptName = trimToNull(value);
            return this;
        }

        public Builder roles(List<String> value) {
            claims.roles = value == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<String>(value));
            return this;
        }

        public Builder agentId(String value) {
            claims.agentId = trimToNull(value);
            return this;
        }

        public Builder sessionId(String value) {
            claims.sessionId = trimToNull(value);
            return this;
        }

        public Builder traceId(String value) {
            claims.traceId = trimToNull(value);
            return this;
        }

        public Builder pageInstanceId(String value) {
            claims.pageInstanceId = trimToNull(value);
            return this;
        }

        public Builder origin(String value) {
            claims.origin = trimToNull(value);
            return this;
        }

        public Builder route(String value) {
            claims.route = trimToNull(value);
            return this;
        }

        public Builder jti(String value) {
            claims.jti = trimToNull(value);
            return this;
        }

        public Builder issuedAtEpochSeconds(long value) {
            claims.issuedAtEpochSeconds = value;
            return this;
        }

        public Builder expiresAtEpochSeconds(long value) {
            claims.expiresAtEpochSeconds = value;
            return this;
        }

        public Builder attributes(Map<String, Object> value) {
            claims.attributes = value == null ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
            return this;
        }

        public ReachAiInvocationClaims build() {
            if (claims.jti == null) {
                claims.jti = UUID.randomUUID().toString();
            }
            return claims;
        }

        private static String trimToNull(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return value.trim();
        }
    }
}
