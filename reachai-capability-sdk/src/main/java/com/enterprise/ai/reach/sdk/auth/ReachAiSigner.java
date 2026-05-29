package com.enterprise.ai.reach.sdk.auth;

import com.enterprise.ai.reach.sdk.client.ReachAiClientConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ReachAiSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private ReachAiSigner() {
    }

    public static ReachAiSignatureHeaders sign(ReachAiClientConfig config) {
        return sign(config, String.valueOf(System.currentTimeMillis()), UUID.randomUUID().toString());
    }

    public static ReachAiSignatureHeaders sign(ReachAiClientConfig config, String timestamp, String nonce) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        String signed = sign(config.getAppSecret(), config.getProjectCode() + "\n" + requireText(timestamp, "timestamp")
                + "\n" + requireText(nonce, "nonce"));
        return new ReachAiSignatureHeaders(config.getAppKey(), timestamp, nonce, signed);
    }

    public static String sign(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(requireText(secret, "secret").getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] bytes = mac.doFinal(requireText(message, "message").getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02x", b & 0xff));
            }
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("ReachAI request signing failed: " + e.getMessage(), e);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
