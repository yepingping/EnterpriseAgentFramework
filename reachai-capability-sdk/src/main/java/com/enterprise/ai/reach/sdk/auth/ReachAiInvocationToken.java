package com.enterprise.ai.reach.sdk.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReachAiInvocationToken {

    public static final String HEADER_NAME = "X-ReachAI-Invocation-Token";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final String HMAC_SHA256 = "HmacSHA256";

    private ReachAiInvocationToken() {
    }

    public static String sign(String secret, ReachAiInvocationClaims claims, long issuedAtMillis, int ttlSeconds) {
        if (claims == null) {
            throw new IllegalArgumentException("claims is required");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
        long issuedAt = issuedAtMillis / 1000L;
        ReachAiInvocationClaims enriched = copyWithTimes(claims, issuedAt, issuedAt + ttlSeconds);
        String header = base64Url(json(header()));
        String payload = base64Url(json(enriched.toMap()));
        return header + "." + payload + "." + signature(secret, header + "." + payload);
    }

    public static ReachAiInvocationClaims verify(String secret,
                                                 String token,
                                                 String expectedProjectCode,
                                                 String expectedCapabilityName,
                                                 long nowMillis) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("ReachAI invocation token is required");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("ReachAI invocation token format is invalid");
        }
        String expected = signature(secret, parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("ReachAI invocation token signature is invalid");
        }
        ReachAiInvocationClaims claims = ReachAiInvocationClaims.fromMap(readMap(parts[1]));
        long now = nowMillis / 1000L;
        if (claims.getExpiresAtEpochSeconds() <= 0 || now > claims.getExpiresAtEpochSeconds()) {
            throw new IllegalArgumentException("ReachAI invocation token is expired");
        }
        if (!equalsText(expectedProjectCode, claims.getProjectCode())) {
            throw new IllegalArgumentException("ReachAI invocation token project does not match");
        }
        if (!equalsText(expectedCapabilityName, claims.getCapabilityName())) {
            throw new IllegalArgumentException("ReachAI invocation token capability does not match");
        }
        return claims;
    }

    private static ReachAiInvocationClaims copyWithTimes(ReachAiInvocationClaims claims, long issuedAt, long expiresAt) {
        return ReachAiInvocationClaims.builder()
                .projectCode(claims.getProjectCode())
                .appKey(claims.getAppKey())
                .capabilityName(claims.getCapabilityName())
                .tenantId(claims.getTenantId())
                .externalUserId(claims.getExternalUserId())
                .globalUserId(claims.getGlobalUserId())
                .userName(claims.getUserName())
                .deptId(claims.getDeptId())
                .deptName(claims.getDeptName())
                .roles(claims.getRoles())
                .agentId(claims.getAgentId())
                .sessionId(claims.getSessionId())
                .traceId(claims.getTraceId())
                .pageInstanceId(claims.getPageInstanceId())
                .origin(claims.getOrigin())
                .route(claims.getRoute())
                .jti(claims.getJti())
                .issuedAtEpochSeconds(issuedAt)
                .expiresAtEpochSeconds(expiresAt)
                .attributes(claims.getAttributes())
                .build();
    }

    private static Map<String, Object> header() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("alg", "HS256");
        out.put("typ", "JWT");
        return out;
    }

    private static String signature(String secret, String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(requireText(secret, "secret").getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("ReachAI invocation token signing failed", ex);
        }
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("ReachAI invocation token json failed", ex);
        }
    }

    private static Map<String, Object> readMap(String encoded) {
        try {
            return OBJECT_MAPPER.readValue(Base64.getUrlDecoder().decode(encoded), MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("ReachAI invocation token payload is invalid", ex);
        }
    }

    private static String base64Url(String value) {
        return base64Url(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static boolean equalsText(String expected, String actual) {
        return requireText(expected, "expected").equals(requireText(actual, "actual"));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
