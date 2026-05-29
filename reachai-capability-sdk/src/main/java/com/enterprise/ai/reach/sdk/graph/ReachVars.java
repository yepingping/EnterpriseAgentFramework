package com.enterprise.ai.reach.sdk.graph;

public final class ReachVars {

    private ReachVars() {
    }

    public static String input(String name) {
        return "${input." + requireText(name, "input name") + "}";
    }

    public static String var(String name) {
        return "${var." + requireText(name, "variable name") + "}";
    }

    public static String nodeOutput(String nodeId) {
        return "${nodeOutput." + requireText(nodeId, "node id") + "}";
    }

    public static String constValue(String value) {
        return "const:" + (value == null ? "" : value);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
