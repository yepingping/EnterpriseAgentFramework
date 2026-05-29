package com.enterprise.ai.reach.sdk.auth;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReachAiSignatureHeaders {

    public static final String HEADER_APP_KEY = "X-ReachAI-App-Key";
    public static final String HEADER_TIMESTAMP = "X-ReachAI-Timestamp";
    public static final String HEADER_NONCE = "X-ReachAI-Nonce";
    public static final String HEADER_SIGNATURE = "X-ReachAI-Signature";

    private final String appKey;
    private final String timestamp;
    private final String nonce;
    private final String signature;

    public ReachAiSignatureHeaders(String appKey, String timestamp, String nonce, String signature) {
        this.appKey = appKey;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public String getSignature() {
        return signature;
    }

    public Map<String, String> toHttpHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HEADER_APP_KEY, appKey);
        headers.put(HEADER_TIMESTAMP, timestamp);
        headers.put(HEADER_NONCE, nonce);
        headers.put(HEADER_SIGNATURE, signature);
        return headers;
    }
}
