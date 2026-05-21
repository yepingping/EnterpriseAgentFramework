package com.enterprise.ai.spring.registry;

import org.springframework.util.StringUtils;

/**
 * Canonical variable reference helpers for SDK-declared Agent graphs.
 */
public final class EafVars {

    private EafVars() {
    }

    public static String input(String path) {
        return path("input", path);
    }

    public static String params(String path) {
        return path("params", path);
    }

    public static String node(String nodeId, String path) {
        return path("node." + clean(nodeId), path);
    }

    public static String alias(String alias, String path) {
        return path(clean(alias), path);
    }

    public static String sys(String path) {
        return path("sys", path);
    }

    private static String path(String root, String path) {
        String cleanRoot = clean(root);
        String cleanPath = clean(path);
        return StringUtils.hasText(cleanPath) ? cleanRoot + "." + cleanPath : cleanRoot;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
