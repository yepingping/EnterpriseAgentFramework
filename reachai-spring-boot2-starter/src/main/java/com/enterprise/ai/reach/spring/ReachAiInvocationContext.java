package com.enterprise.ai.reach.spring;

import com.enterprise.ai.reach.sdk.auth.ReachAiInvocationClaims;

import java.util.List;
import java.util.Map;

public class ReachAiInvocationContext {

    private final ReachAiInvocationClaims claims;

    public ReachAiInvocationContext(ReachAiInvocationClaims claims) {
        if (claims == null) {
            throw new IllegalArgumentException("claims is required");
        }
        this.claims = claims;
    }

    public ReachAiInvocationClaims getClaims() {
        return claims;
    }

    public String getProjectCode() {
        return claims.getProjectCode();
    }

    public String getAppKey() {
        return claims.getAppKey();
    }

    public String getCapabilityName() {
        return claims.getCapabilityName();
    }

    public String getTenantId() {
        return claims.getTenantId();
    }

    public String getExternalUserId() {
        return claims.getExternalUserId();
    }

    public String getGlobalUserId() {
        return claims.getGlobalUserId();
    }

    public String getUserName() {
        return claims.getUserName();
    }

    public String getDeptId() {
        return claims.getDeptId();
    }

    public String getDeptName() {
        return claims.getDeptName();
    }

    public List<String> getRoles() {
        return claims.getRoles();
    }

    public String getAgentId() {
        return claims.getAgentId();
    }

    public String getSessionId() {
        return claims.getSessionId();
    }

    public String getTraceId() {
        return claims.getTraceId();
    }

    public String getPageInstanceId() {
        return claims.getPageInstanceId();
    }

    public String getOrigin() {
        return claims.getOrigin();
    }

    public String getRoute() {
        return claims.getRoute();
    }

    public Map<String, Object> getAttributes() {
        return claims.getAttributes();
    }
}
