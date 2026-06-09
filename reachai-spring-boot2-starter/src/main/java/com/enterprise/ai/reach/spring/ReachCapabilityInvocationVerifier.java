package com.enterprise.ai.reach.spring;

import com.enterprise.ai.reach.sdk.auth.ReachAiInvocationClaims;
import com.enterprise.ai.reach.sdk.auth.ReachAiInvocationToken;
import org.springframework.util.StringUtils;

public class ReachCapabilityInvocationVerifier {

    private final ReachAiRegistryProperties properties;

    public ReachCapabilityInvocationVerifier(ReachAiRegistryProperties properties) {
        this.properties = properties;
    }

    public ReachAiInvocationContext verify(String capabilityName, String token) {
        if (!requireInvocationToken()) {
            return null;
        }
        String projectCode = required(properties.getProject().getCode(), "reachai.project.code");
        String appSecret = required(properties.getRegistry().getAppSecret(), "reachai.registry.appSecret");
        ReachAiInvocationClaims claims = ReachAiInvocationToken.verify(
                appSecret, token, projectCode, capabilityName, System.currentTimeMillis());
        String configuredAppKey = properties.getRegistry().getAppKey();
        if (StringUtils.hasText(configuredAppKey) && !configuredAppKey.trim().equals(claims.getAppKey())) {
            throw new IllegalArgumentException("ReachAI invocation token appKey does not match");
        }
        return new ReachAiInvocationContext(claims);
    }

    private boolean requireInvocationToken() {
        return properties != null && properties.getCapability().isRequireInvocationToken();
    }

    private String required(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
